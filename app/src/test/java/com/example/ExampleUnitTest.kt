package com.example

import com.example.data.backup.CloudBackupManager
import com.example.data.backup.SonoraCloudBackup
import com.example.data.lyrics.LrcParser
import com.example.data.model.Playlist
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.Track
import com.example.data.model.TrackBookmark
import com.example.data.prefs.AccentTheme
import com.example.data.prefs.AppFontChoice
import com.example.data.prefs.ArtworkShapeStyle
import com.example.data.prefs.ThemeMode
import com.example.data.prefs.UserCustomization
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun parseLrcAndFindActiveLine() {
        val raw = """
            [ti:Test Song]
            [00:01.00]Primera línea
            [00:05.50]Segunda línea
            [00:10.00]Tercera línea
        """.trimIndent()

        val lines = LrcParser.parse(raw)
        assertEquals(3, lines.size)
        assertEquals(1000L, lines[0].timeMs)
        assertEquals("Segunda línea", lines[1].text)
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 2500L))
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 5500L))
        assertEquals(2, LrcParser.findActiveLineIndex(lines, 12000L))
    }

    @Test
    fun parsePlainTextDistributesTimestamps() {
        val raw = "Verso uno\nVerso dos\nVerso tres"
        val lines = LrcParser.parse(raw, totalDurationMs = 9000L)
        assertEquals(3, lines.size)
        assertFalse(lines[0].isTimed)
        assertEquals(0L, lines[0].timeMs)
        assertEquals(3000L, lines[1].timeMs)
        assertEquals(6000L, lines[2].timeMs)
    }

    @Test
    fun formatTimestampFormatsMinutesSecondsCentis() {
        assertEquals("[01:05.25]", LrcParser.formatTimestamp(65_250L))
    }

    @Test
    fun shiftTimestampsAdjustsPositiveAndClampsNegativeToZero() {
        val raw = "[00:00.40]Inicio\n[00:02.00]Estribillo"
        val shiftedForward = LrcParser.shiftTimestamps(raw, deltaMs = 500L)
        val parsedForward = LrcParser.parse(shiftedForward)
        assertEquals(900L, parsedForward[0].timeMs)
        assertEquals(2500L, parsedForward[1].timeMs)

        val shiftedBack = LrcParser.shiftTimestamps(raw, deltaMs = -1000L)
        val parsedBack = LrcParser.parse(shiftedBack)
        assertEquals(0L, parsedBack[0].timeMs)
        assertEquals(1000L, parsedBack[1].timeMs)
    }

    @Test
    fun buildM3uContentFormatsValidPlaylist() {
        val playlistWithTracks = PlaylistWithTracks(
            playlist = Playlist(id = 1L, name = "Sesión Nocturna"),
            tracks = listOf(
                Track(
                    id = "t1",
                    title = "Bruma Tonal",
                    artist = "Sonora",
                    album = "Sesiones",
                    uri = "content://media/audio/1",
                    durationMs = 125_000L
                )
            )
        )
        val m3u = CloudBackupManager.buildM3uContent(playlistWithTracks)
        assertTrue(m3u.contains("#EXTM3U"))
        assertTrue(m3u.contains("#PLAYLIST:Sesión Nocturna"))
        assertTrue(m3u.contains("#EXTINF:125,Sonora - Bruma Tonal"))
        assertTrue(m3u.contains("content://media/audio/1"))
    }

    @Test
    fun cloudBackupRoundTripPreservesTracksBookmarksAndCustomization() {
        val backup = SonoraCloudBackup(
            tracks = listOf(
                Track(
                    id = "t1",
                    title = "Nocturno en Estudio",
                    artist = "Sonora",
                    album = "Sesiones",
                    uri = "file:///tmp/test.flac",
                    formatBadge = "FLAC",
                    durationMs = 180_000L
                )
            ),
            bookmarks = listOf(
                TrackBookmark(
                    id = 1L,
                    trackId = "t1",
                    timeMs = 45_000L,
                    label = "Solo de guitarra"
                )
            ),
            customization = UserCustomization(
                themeMode = ThemeMode.DARK_M3,
                accentTheme = AccentTheme.SAGE_TONAL,
                fontChoice = AppFontChoice.EDITORIAL_SERIF,
                artworkShape = ArtworkShapeStyle.CIRCLE,
                skipSilence = true,
                monoAudio = true,
                useTonalContainerBackdrop = true
            )
        )
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val encoded = json.encodeToString(backup)
        val decoded = json.decodeFromString<SonoraCloudBackup>(encoded)
        assertEquals(1, decoded.tracks.size)
        assertEquals("FLAC", decoded.tracks.first().formatBadge)
        assertEquals(1, decoded.bookmarks.size)
        assertEquals("Solo de guitarra", decoded.bookmarks.first().label)
        assertEquals(AccentTheme.SAGE_TONAL, decoded.customization.accentTheme)
        assertEquals(AppFontChoice.EDITORIAL_SERIF, decoded.customization.fontChoice)
        assertEquals(ArtworkShapeStyle.CIRCLE, decoded.customization.artworkShape)
        assertTrue(decoded.customization.skipSilence)
        assertTrue(decoded.customization.monoAudio)
        assertTrue(decoded.customization.useTonalContainerBackdrop)
    }
}
