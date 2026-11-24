package com.example.data.lyrics

import com.example.data.model.LyricLine
import java.util.Locale

object LrcParser {

    private val TIME_TAG_REGEX = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val META_TAG_REGEX = Regex("^\\[(ti|ar|al|au|by|offset|length|re|ve):.*]$", RegexOption.IGNORE_CASE)
    private val OFFSET_REGEX = Regex("^\\[offset:([+-]?\\d+)]$", RegexOption.IGNORE_CASE)

    fun parse(rawText: String, totalDurationMs: Long = 0L): List<LyricLine> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()

        var offsetMs = 0L
        val timedLines = mutableListOf<LyricLine>()
        val plainLines = mutableListOf<String>()

        for (rawLine in trimmed.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val offsetMatch = OFFSET_REGEX.find(line)
            if (offsetMatch != null) {
                offsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
                continue
            }
            if (META_TAG_REGEX.matches(line)) continue

            val matches = TIME_TAG_REGEX.findAll(line).toList()
            if (matches.isNotEmpty()) {
                val lyricText = TIME_TAG_REGEX.replace(line, "").trim()
                if (lyricText.isNotEmpty()) {
                    for (m in matches) {
                        val min = m.groupValues[1].toLongOrNull() ?: 0L
                        val sec = m.groupValues[2].toLongOrNull() ?: 0L
                        val fracStr = m.groupValues[3]
                        val millis = when (fracStr.length) {
                            0 -> 0L
                            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                            else -> (fracStr.take(3).toLongOrNull() ?: 0L)
                        }
                        val timeMs = ((min * 60_000L) + (sec * 1000L) + millis - offsetMs).coerceAtLeast(0L)
                        timedLines.add(LyricLine(timeMs = timeMs, text = lyricText, isTimed = true))
                    }
                }
            } else {
                plainLines.add(line)
            }
        }

        if (timedLines.isNotEmpty()) {
            return timedLines.sortedBy { it.timeMs }
        }

        if (plainLines.isEmpty()) return emptyList()
        val safeDuration = totalDurationMs.coerceAtLeast(plainLines.size * 3000L)
        val step = safeDuration / plainLines.size.coerceAtLeast(1)
        return plainLines.mapIndexed { index, text ->
            LyricLine(timeMs = index * step, text = text, isTimed = false)
        }
    }

    fun shiftTimestamps(rawText: String, deltaMs: Long, totalDurationMs: Long = 0L): String {
        val parsed = parse(rawText, totalDurationMs)
        if (parsed.isEmpty()) return rawText
        return parsed.joinToString("\n") { line ->
            "${formatTimestamp((line.timeMs + deltaMs).coerceAtLeast(0L))} ${line.text}"
        }
    }

    fun findActiveLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var active = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs + 120L) {
                active = i
            } else {
                break
            }
        }
        return active
    }

    fun formatTimestamp(timeMs: Long): String {
        val safe = timeMs.coerceAtLeast(0L)
        val minutes = safe / 60_000L
        val seconds = (safe % 60_000L) / 1000L
        val centis = (safe % 1000L) / 10L
        return String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, centis)
    }

    fun serialize(lines: List<LyricLine>): String {
        return lines.joinToString("\n") { line ->
            "${formatTimestamp(line.timeMs)} ${line.text}"
        }
    }
}
