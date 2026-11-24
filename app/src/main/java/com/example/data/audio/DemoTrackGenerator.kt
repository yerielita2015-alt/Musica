package com.example.data.audio

import android.content.Context
import android.net.Uri
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object DemoTrackGenerator {

    private data class StudioSpec(
        val id: String,
        val fileName: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationSec: Int,
        val baseFreqs: DoubleArray,
        val tempoBpm: Double,
        val lyricsLrc: String
    )

    private val specs = listOf(
        StudioSpec(
            id = "studio_nocturne_hi_res",
            fileName = "nocturno_en_estudio.wav",
            title = "Nocturno en Estudio",
            artist = "Sonora Studio Ensemble",
            album = "Sesiones de Estudio (Hi-Res)",
            durationSec = 28,
            baseFreqs = doubleArrayOf(220.0, 261.63, 329.63, 392.0), // Am7
            tempoBpm = 92.0,
            lyricsLrc = """
                [ti:Nocturno en Estudio]
                [ar:Sonora Studio Ensemble]
                [00:00.20]Luces suaves caen sobre el estudio oscuro
                [00:04.00]Cada frecuencia respira en silencio puro
                [00:08.00]Suben los graves, vibra la habitación
                [00:12.00]El espectro dibuja nuestra propia canción
                [00:16.20]Gira el disco con calma en órbita digital
                [00:20.40]Sonido limpio sin pérdida en cada señal
                [00:24.20]Quédate aquí hasta que amanezca el compás
            """.trimIndent()
        ),
        StudioSpec(
            id = "studio_aurora_synth",
            fileName = "aurora_acustica.wav",
            title = "Aurora Acústica",
            artist = "Vega & Ensamble",
            album = "Frecuencias de Medianoche",
            durationSec = 26,
            baseFreqs = doubleArrayOf(174.61, 220.0, 261.63, 349.23), // Fmaj7
            tempoBpm = 108.0,
            lyricsLrc = """
                [ti:Aurora Acústica]
                [ar:Vega & Ensamble]
                [00:00.20]Pulso constante cruzando el horizonte azul
                [00:04.10]Cálidos armónicos despiertan en el sur
                [00:08.20]Ajusta el ecualizador a tu propio latir
                [00:12.40]Deja que el aire espacial empiece a fluir
                [00:16.50]Ondas serenas en estéreo alrededor
                [00:20.60]Brilla la calma en cada acorde interior
            """.trimIndent()
        ),
        StudioSpec(
            id = "studio_brisa_acustica",
            fileName = "brisa_de_terciopelo.wav",
            title = "Brisa de Terciopelo",
            artist = "Clara Solís",
            album = "Acústico 24-Bit",
            durationSec = 24,
            baseFreqs = doubleArrayOf(196.0, 246.94, 293.66, 369.99), // Gmaj7
            tempoBpm = 80.0,
            lyricsLrc = """
                [ti:Brisa de Terciopelo]
                [ar:Clara Solís]
                [00:00.20]Notas suaves flotan sin prisa al caer
                [00:04.20]Un eco cálido vuelve a renacer
                [00:08.40]Cierra los ojos, escucha la claridad
                [00:12.60]Cada detalle vibra con libertad
                [00:16.80]Terciopelo y calma en tiempo real
                [00:20.50]Un refugio íntimo y musical
            """.trimIndent()
        )
    )

    suspend fun generateStudioTracksIfNeeded(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "sonora_studio").apply { mkdirs() }
        specs.mapIndexed { index, spec ->
            val file = File(dir, spec.fileName)
            if (!file.exists() || file.length() < 1024L) {
                writeWarmMusicalWav(file, spec.durationSec, spec.baseFreqs, spec.tempoBpm)
            }
            val durationMs = spec.durationSec * 1000L
            Track(
                id = spec.id,
                title = spec.title,
                artist = spec.artist,
                album = spec.album,
                durationMs = durationMs,
                uri = Uri.fromFile(file).toString(),
                artworkUri = null,
                formatBadge = "WAV",
                bitrateKbps = 1411,
                sampleRateHz = 44100,
                folderName = "Sonora Estudio",
                isFavorite = index == 0,
                playCount = 0,
                lyricsLrc = spec.lyricsLrc,
                addedAt = System.currentTimeMillis() - (index * 1000L),
                isDemo = true
            )
        }
    }

    private fun writeWarmMusicalWav(
        file: File,
        durationSec: Int,
        chordFreqs: DoubleArray,
        tempoBpm: Double
    ) {
        val sampleRate = 44100
        val channels = 2
        val bitsPerSample = 16
        val totalFrames = sampleRate * durationSec
        val dataSize = totalFrames * channels * (bitsPerSample / 8)
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val beatSec = 60.0 / tempoBpm

        BufferedOutputStream(FileOutputStream(file), 32768).use { out ->
            out.write("RIFF".toByteArray(Charsets.US_ASCII))
            writeIntLE(out, 36 + dataSize)
            out.write("WAVE".toByteArray(Charsets.US_ASCII))
            out.write("fmt ".toByteArray(Charsets.US_ASCII))
            writeIntLE(out, 16)
            writeShortLE(out, 1)
            writeShortLE(out, channels)
            writeIntLE(out, sampleRate)
            writeIntLE(out, byteRate)
            writeShortLE(out, channels * (bitsPerSample / 8))
            writeShortLE(out, bitsPerSample)
            out.write("data".toByteArray(Charsets.US_ASCII))
            writeIntLE(out, dataSize)

            val buffer = ByteArray(4096)
            var bufIdx = 0

            val progressions = arrayOf(1.0, 1.12246, 0.8909, 1.2599)
            for (frame in 0 until totalFrames) {
                val t = frame.toDouble() / sampleRate
                val barIndex = ((t / (beatSec * 4.0)).toInt()) % progressions.size
                val rootScale = progressions[barIndex]

                val masterEnv = when {
                    t < 0.8 -> t / 0.8
                    t > durationSec - 1.2 -> ((durationSec - t) / 1.2).coerceAtLeast(0.0)
                    else -> 1.0
                }

                var padL = 0.0
                var padR = 0.0
                for (i in chordFreqs.indices) {
                    val f = chordFreqs[i] * rootScale
                    val detune = if (i % 2 == 0) 1.002 else 0.998
                    padL += sin(2.0 * PI * f * t) * 0.16 + sin(2.0 * PI * f * 2.0 * t) * 0.04
                    padR += sin(2.0 * PI * f * detune * t) * 0.16 + sin(2.0 * PI * f * 2.0 * t) * 0.04
                }

                val beatPos = (t % beatSec) / beatSec
                val bassEnv = exp(-beatPos * 4.2)
                val bassFreq = (chordFreqs[0] * 0.5) * rootScale
                val bass = sin(2.0 * PI * bassFreq * t) * 0.34 * bassEnv

                val eighthPos = (t % (beatSec * 0.5)) / (beatSec * 0.5)
                val arpIndex = ((t / (beatSec * 0.5)).toInt()) % chordFreqs.size
                val arpEnv = exp(-eighthPos * 6.5)
                val arpFreq = chordFreqs[arpIndex] * 2.0 * rootScale
                val arp = sin(2.0 * PI * arpFreq * t) * 0.16 * arpEnv

                val left = ((padL + bass + arp * 0.75) * masterEnv).coerceIn(-0.92, 0.92)
                val right = ((padR + bass + arp * 1.15) * masterEnv).coerceIn(-0.92, 0.92)

                val sL = (left * 32767.0).toInt().coerceIn(-32768, 32767)
                val sR = (right * 32767.0).toInt().coerceIn(-32768, 32767)

                buffer[bufIdx++] = (sL and 0xFF).toByte()
                buffer[bufIdx++] = ((sL shr 8) and 0xFF).toByte()
                buffer[bufIdx++] = (sR and 0xFF).toByte()
                buffer[bufIdx++] = ((sR shr 8) and 0xFF).toByte()

                if (bufIdx >= buffer.size) {
                    out.write(buffer, 0, bufIdx)
                    bufIdx = 0
                }
            }
            if (bufIdx > 0) {
                out.write(buffer, 0, bufIdx)
            }
        }
    }

    private fun writeIntLE(out: BufferedOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
        out.write((value shr 16) and 0xFF)
        out.write((value shr 24) and 0xFF)
    }

    private fun writeShortLE(out: BufferedOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }
}
