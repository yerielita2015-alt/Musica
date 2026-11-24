package com.example.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

object AudioSpectrumBus {
    private const val BAND_COUNT = 18
    private val _bands = MutableStateFlow(List(BAND_COUNT) { 0.06f })
    val bands: StateFlow<List<Float>> = _bands.asStateFlow()

    @Volatile
    var preampDb: Float = 0f

    @Volatile
    var stereoBalance: Float = 0f

    @Volatile
    var crossfadeGain: Float = 1f

    @Volatile
    var sleepFadeGain: Float = 1f

    @Volatile
    var monoAudio: Boolean = false

    @Volatile
    var softLimiter: Boolean = true

    @Volatile
    var skipSilenceEnabled: Boolean = false

    @Volatile
    var onSkipSilenceChanged: ((Boolean) -> Unit)? = null

    @Volatile
    var softwareEqEnabled: Boolean = false

    @Volatile
    var softwareEqGainsDb: FloatArray = floatArrayOf(0f, 0f, 0f, 0f, 0f)

    @Volatile
    var softwareEqVersion: Int = 0

    fun setSkipSilence(enabled: Boolean) {
        skipSilenceEnabled = enabled
        onSkipSilenceChanged?.invoke(enabled)
    }

    internal fun publish(newBands: List<Float>) {
        _bands.value = newBands
    }

    internal fun reset() {
        _bands.value = List(BAND_COUNT) { 0.06f }
    }
}

@UnstableApi
class SonoraAudioProcessor : BaseAudioProcessor() {

    private class BiquadBand {
        var b0 = 1f
        var b1 = 0f
        var b2 = 0f
        var a1 = 0f
        var a2 = 0f

        var z1L = 0f
        var z2L = 0f
        var z1R = 0f
        var z2R = 0f

        fun resetState() {
            z1L = 0f
            z2L = 0f
            z1R = 0f
            z2R = 0f
        }

        fun configurePeaking(freqHz: Float, sampleRate: Int, gainDb: Float, q: Float = 1.25f) {
            if (abs(gainDb) < 0.05f || sampleRate <= 0) {
                b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f
                return
            }
            val safeFreq = freqHz.coerceIn(20f, sampleRate * 0.44f)
            val a = 10.0.pow(gainDb / 40.0)
            val omega = 2.0 * PI * safeFreq / sampleRate
            val sinW = sin(omega)
            val cosW = cos(omega)
            val alpha = sinW / (2.0 * q)

            val rawB0 = 1.0 + alpha * a
            val rawB1 = -2.0 * cosW
            val rawB2 = 1.0 - alpha * a
            val rawA0 = 1.0 + alpha / a
            val rawA1 = -2.0 * cosW
            val rawA2 = 1.0 - alpha / a

            b0 = (rawB0 / rawA0).toFloat()
            b1 = (rawB1 / rawA0).toFloat()
            b2 = (rawB2 / rawA0).toFloat()
            a1 = (rawA1 / rawA0).toFloat()
            a2 = (rawA2 / rawA0).toFloat()
        }

        fun processLeft(x: Float): Float {
            val y = b0 * x + z1L
            z1L = b1 * x - a1 * y + z2L
            z2L = b2 * x - a2 * y
            return y
        }

        fun processRight(x: Float): Float {
            val y = b0 * x + z1R
            z1R = b1 * x - a1 * y + z2R
            z2R = b2 * x - a2 * y
            return y
        }
    }

    private val biquads = Array(5) { BiquadBand() }
    private val bandFreqs = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
    private var appliedEqVersion = -1
    private var configuredSampleRate = 44100

    private var lastPublishNs = 0L
    private val smoothed = FloatArray(18) { 0.06f }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            configuredSampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8000)
            appliedEqVersion = -1
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    private fun syncBiquadsIfNeeded() {
        val v = AudioSpectrumBus.softwareEqVersion
        if (v == appliedEqVersion) return
        appliedEqVersion = v
        val gains = AudioSpectrumBus.softwareEqGainsDb
        val enabled = AudioSpectrumBus.softwareEqEnabled
        for (i in biquads.indices) {
            val db = if (enabled && i < gains.size) gains[i] else 0f
            biquads[i].configurePeaking(bandFreqs[i], configuredSampleRate, db)
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        syncBiquadsIfNeeded()

        val out = replaceOutputBuffer(remaining)
        out.order(ByteOrder.LITTLE_ENDIAN)
        val inRead = inputBuffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)

        val preampFactor = 10.0.pow((AudioSpectrumBus.preampDb / 20.0)).toFloat()
        val masterGain = preampFactor * AudioSpectrumBus.crossfadeGain * AudioSpectrumBus.sleepFadeGain
        val bal = AudioSpectrumBus.stereoBalance.coerceIn(-1f, 1f)
        val leftGain = (if (bal > 0f) 1f - bal else 1f) * masterGain
        val rightGain = (if (bal < 0f) 1f + bal else 1f) * masterGain
        val useSwEq = AudioSpectrumBus.softwareEqEnabled
        val useMono = AudioSpectrumBus.monoAudio
        val useLimiter = AudioSpectrumBus.softLimiter

        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val sampleCount = remaining / 2
        val step = (sampleCount / 64).coerceAtLeast(1)
        val bins = FloatArray(18)
        var binSampleCount = 0

        if (useMono && channels == 2) {
            var frameIdx = 0
            while (inRead.remaining() >= 4) {
                val l = inRead.short.toFloat()
                val r = inRead.short.toFloat()
                var mono = (l + r) * 0.5f
                if (useSwEq) {
                    for (b in biquads.indices) mono = biquads[b].processLeft(mono)
                }
                val outL = applyPostGainAndLimit(mono * leftGain, useLimiter)
                val outR = applyPostGainAndLimit(mono * rightGain, useLimiter)
                out.putShort(outL)
                out.putShort(outR)

                if (frameIdx % step == 0 && binSampleCount < 64) {
                    val norm = abs(outL.toInt()) / 32768f
                    val bandIdx = (binSampleCount * 18) / 64
                    val waveWeight = 0.55f + 0.45f * abs(sin(binSampleCount * 0.42f + norm * 3.1f))
                    bins[bandIdx] = (bins[bandIdx] + norm * waveWeight).coerceAtMost(1f)
                    binSampleCount++
                }
                frameIdx += 2
            }
        } else {
            var idx = 0
            while (inRead.remaining() >= 2) {
                var sample = inRead.short.toFloat()
                val channelIndex = idx % channels

                if (useSwEq) {
                    if (channels == 1 || channelIndex == 0) {
                        for (b in biquads.indices) sample = biquads[b].processLeft(sample)
                    } else {
                        for (b in biquads.indices) sample = biquads[b].processRight(sample)
                    }
                }

                val gain = when {
                    channels == 1 -> (leftGain + rightGain) * 0.5f
                    channelIndex == 0 -> leftGain
                    else -> rightGain
                }
                val scaled = applyPostGainAndLimit(sample * gain, useLimiter)
                out.putShort(scaled)

                if (idx % step == 0 && binSampleCount < 64) {
                    val norm = abs(scaled.toInt()) / 32768f
                    val bandIdx = (binSampleCount * 18) / 64
                    val waveWeight = 0.55f + 0.45f * abs(sin(binSampleCount * 0.42f + norm * 3.1f))
                    bins[bandIdx] = (bins[bandIdx] + norm * waveWeight).coerceAtMost(1f)
                    binSampleCount++
                }
                idx++
            }
        }
        while (inRead.hasRemaining()) {
            out.put(inRead.get())
        }

        inputBuffer.position(inputBuffer.limit())
        out.flip()

        val now = System.nanoTime()
        if (now - lastPublishNs >= 40_000_000L) {
            lastPublishNs = now
            val next = List(18) { i ->
                val target = (bins[i] * 1.45f).coerceIn(0.06f, 1.0f)
                smoothed[i] = smoothed[i] * 0.52f + target * 0.48f
                smoothed[i]
            }
            AudioSpectrumBus.publish(next)
        }
    }

    private fun applyPostGainAndLimit(value: Float, useLimiter: Boolean): Short {
        if (!useLimiter) {
            return value.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        val norm = value / 32768f
        val limited = if (abs(norm) > 0.85f) {
            val sign = if (norm >= 0f) 1f else -1f
            sign * (0.85f + 0.15f * tanh(((abs(norm) - 0.85f) / 0.15f).toDouble()).toFloat())
        } else {
            norm
        }
        return (limited * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    override fun onFlush() {
        super.onFlush()
        biquads.forEach { it.resetState() }
        AudioSpectrumBus.reset()
    }

    override fun onReset() {
        super.onReset()
        biquads.forEach { it.resetState() }
        AudioSpectrumBus.reset()
    }
}
