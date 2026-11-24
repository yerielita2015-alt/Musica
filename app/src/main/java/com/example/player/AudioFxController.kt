package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.example.data.model.EqPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqUiState(
    val enabled: Boolean = true,
    val bandCount: Int = 5,
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500,
    val centerFreqsHz: List<Int> = listOf(60, 230, 910, 3600, 14000),
    val bandLevelsMb: List<Int> = listOf(200, 100, 0, 150, 250),
    val bassStrength: Int = 280,
    val virtualizerStrength: Int = 220,
    val loudnessGainMb: Int = 0,
    val preampDb: Float = 0f,
    val stereoBalance: Float = 0f,
    val activePresetName: String = "Estudio Sonora",
    val hardwareAttached: Boolean = false
)

object AudioFxController {

    val builtInPresets: List<EqPreset> = listOf(
        EqPreset("Plano", listOf(0, 0, 0, 0, 0), bassStrength = 0, virtualizerStrength = 0, loudnessGainMb = 0),
        EqPreset("Estudio Sonora", listOf(220, 110, 0, 160, 260), bassStrength = 280, virtualizerStrength = 220, loudnessGainMb = 150),
        EqPreset("Graves Profundos", listOf(650, 420, 80, -60, 120), bassStrength = 750, virtualizerStrength = 180, loudnessGainMb = 250),
        EqPreset("Rock", listOf(450, 200, -120, 280, 420), bassStrength = 400, virtualizerStrength = 250, loudnessGainMb = 100),
        EqPreset("Electrónica", listOf(550, 300, -80, 320, 520), bassStrength = 620, virtualizerStrength = 480, loudnessGainMb = 200),
        EqPreset("Pop", listOf(-100, 240, 420, 220, -80), bassStrength = 200, virtualizerStrength = 200, loudnessGainMb = 100),
        EqPreset("Jazz", listOf(280, 140, -140, 220, 340), bassStrength = 180, virtualizerStrength = 350, loudnessGainMb = 0),
        EqPreset("Clásica", listOf(320, 150, -50, 220, 380), bassStrength = 100, virtualizerStrength = 420, loudnessGainMb = 0),
        EqPreset("Vocal", listOf(-280, -80, 480, 360, 120), bassStrength = 0, virtualizerStrength = 150, loudnessGainMb = 200),
        EqPreset("Acústico", listOf(300, 180, 120, 280, 240), bassStrength = 140, virtualizerStrength = 300, loudnessGainMb = 0)
    )

    private val _state = MutableStateFlow(EqUiState())
    val state: StateFlow<EqUiState> = _state.asStateFlow()

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var boundSessionId: Int = 0

    @Synchronized
    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == boundSessionId) return
        releaseEffects()
        boundSessionId = audioSessionId

        var attached = false
        val current = _state.value
        var bandCount = current.bandCount
        var minMb = current.minLevelMb
        var maxMb = current.maxLevelMb
        var freqs = current.centerFreqsHz

        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            attached = true
            bandCount = eq.numberOfBands.toInt().coerceAtLeast(1)
            val range = eq.bandLevelRange
            minMb = range[0].toInt()
            maxMb = range[1].toInt()
            freqs = (0 until bandCount).map { band ->
                (eq.getCenterFreq(band.toShort()) / 1000).coerceAtLeast(20)
            }
        } catch (_: Throwable) {
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, audioSessionId)
        } catch (_: Throwable) {
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(0, audioSessionId)
        } catch (_: Throwable) {
            virtualizer = null
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
        } catch (_: Throwable) {
            loudnessEnhancer = null
        }

        val normalizedLevels = resampleBands(current.bandLevelsMb, bandCount, minMb, maxMb)
        _state.value = current.copy(
            bandCount = bandCount,
            minLevelMb = minMb,
            maxLevelMb = maxMb,
            centerFreqsHz = freqs,
            bandLevelsMb = normalizedLevels,
            hardwareAttached = attached
        )
        applyCurrentParameters()
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(enabled = enabled)
        applyCurrentParameters()
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val current = _state.value
        if (bandIndex !in current.bandLevelsMb.indices) return
        val clamped = levelMb.coerceIn(current.minLevelMb, current.maxLevelMb)
        val updated = current.bandLevelsMb.toMutableList().apply { this[bandIndex] = clamped }
        _state.value = current.copy(
            bandLevelsMb = updated,
            activePresetName = "Personalizado"
        )
        applyCurrentParameters()
    }

    fun setBassStrength(strength: Int) {
        _state.value = _state.value.copy(
            bassStrength = strength.coerceIn(0, 1000),
            activePresetName = "Personalizado"
        )
        applyCurrentParameters()
    }

    fun setVirtualizerStrength(strength: Int) {
        _state.value = _state.value.copy(
            virtualizerStrength = strength.coerceIn(0, 1000),
            activePresetName = "Personalizado"
        )
        applyCurrentParameters()
    }

    fun setLoudnessGain(gainMb: Int) {
        _state.value = _state.value.copy(
            loudnessGainMb = gainMb.coerceIn(0, 1500),
            activePresetName = "Personalizado"
        )
        applyCurrentParameters()
    }

    fun setPreampDb(preampDb: Float) {
        val clamped = preampDb.coerceIn(-12f, 12f)
        _state.value = _state.value.copy(preampDb = clamped)
        applyCurrentParameters()
    }

    fun setStereoBalance(balance: Float) {
        val clamped = balance.coerceIn(-1f, 1f)
        _state.value = _state.value.copy(stereoBalance = clamped)
        applyCurrentParameters()
    }

    fun applyPreset(preset: EqPreset) {
        val current = _state.value
        val levels = resampleBands(preset.bandLevelsMb, current.bandCount, current.minLevelMb, current.maxLevelMb)
        _state.value = current.copy(
            bandLevelsMb = levels,
            bassStrength = preset.bassStrength.coerceIn(0, 1000),
            virtualizerStrength = preset.virtualizerStrength.coerceIn(0, 1000),
            loudnessGainMb = preset.loudnessGainMb.coerceIn(0, 1500),
            preampDb = preset.preampDb.coerceIn(-12f, 12f),
            stereoBalance = preset.stereoBalance.coerceIn(-1f, 1f),
            activePresetName = preset.name
        )
        applyCurrentParameters()
    }

    fun restoreFromPreferences(
        enabled: Boolean,
        presetName: String,
        bandLevelsCsv: String,
        bassStrength: Int,
        virtualizerStrength: Int,
        loudnessGainMb: Int,
        preampDb: Float,
        stereoBalance: Float
    ) {
        val current = _state.value
        val parsed = bandLevelsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        val levels = resampleBands(
            if (parsed.isNotEmpty()) parsed else current.bandLevelsMb,
            current.bandCount,
            current.minLevelMb,
            current.maxLevelMb
        )
        _state.value = current.copy(
            enabled = enabled,
            activePresetName = presetName,
            bandLevelsMb = levels,
            bassStrength = bassStrength.coerceIn(0, 1000),
            virtualizerStrength = virtualizerStrength.coerceIn(0, 1000),
            loudnessGainMb = loudnessGainMb.coerceIn(0, 1500),
            preampDb = preampDb.coerceIn(-12f, 12f),
            stereoBalance = stereoBalance.coerceIn(-1f, 1f)
        )
        applyCurrentParameters()
    }

    private fun applyCurrentParameters() {
        val s = _state.value
        AudioSpectrumBus.preampDb = if (s.enabled) s.preampDb else 0f
        AudioSpectrumBus.stereoBalance = s.stereoBalance

        // Software 5-band biquad EQ acts as seamless fallback when hardware Equalizer is unavailable
        val swGains = FloatArray(5) { i ->
            val mb = s.bandLevelsMb.getOrElse(i) { 0 }
            val bassBonus = if (i == 0 && bassBoost == null) (s.bassStrength / 1000f) * 6f else 0f
            (mb / 100f) + bassBonus
        }
        AudioSpectrumBus.softwareEqEnabled = s.enabled && !s.hardwareAttached
        AudioSpectrumBus.softwareEqGainsDb = swGains
        AudioSpectrumBus.softwareEqVersion++

        try {
            equalizer?.let { eq ->
                eq.enabled = s.enabled
                if (s.enabled) {
                    val count = eq.numberOfBands.toInt()
                    for (i in 0 until count) {
                        val level = s.bandLevelsMb.getOrElse(i) { 0 }
                            .coerceIn(s.minLevelMb, s.maxLevelMb)
                            .toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
            }
        } catch (_: Throwable) {
        }

        try {
            bassBoost?.let { bb ->
                bb.enabled = s.enabled && s.bassStrength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(s.bassStrength.coerceIn(0, 1000).toShort())
                }
            }
        } catch (_: Throwable) {
        }

        try {
            virtualizer?.let { vz ->
                vz.enabled = s.enabled && s.virtualizerStrength > 0
                if (vz.strengthSupported) {
                    vz.setStrength(s.virtualizerStrength.coerceIn(0, 1000).toShort())
                }
            }
        } catch (_: Throwable) {
        }

        try {
            loudnessEnhancer?.let { le ->
                le.enabled = s.enabled && s.loudnessGainMb > 0
                le.setTargetGain(s.loudnessGainMb.coerceIn(0, 1500))
            }
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    fun releaseEffects() {
        try { equalizer?.release() } catch (_: Throwable) {}
        try { bassBoost?.release() } catch (_: Throwable) {}
        try { virtualizer?.release() } catch (_: Throwable) {}
        try { loudnessEnhancer?.release() } catch (_: Throwable) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        boundSessionId = 0
    }

    private fun resampleBands(source: List<Int>, targetCount: Int, minMb: Int, maxMb: Int): List<Int> {
        if (targetCount <= 0) return emptyList()
        if (source.isEmpty()) return List(targetCount) { 0 }
        if (source.size == targetCount) return source.map { it.coerceIn(minMb, maxMb) }
        return List(targetCount) { i ->
            val mappedIdx = ((i.toFloat() / (targetCount - 1).coerceAtLeast(1)) * (source.size - 1))
                .toInt()
                .coerceIn(0, source.lastIndex)
            source[mappedIdx].coerceIn(minMb, maxMb)
        }
    }
}
