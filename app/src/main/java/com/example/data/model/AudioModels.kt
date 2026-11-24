package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String? = null,
    val formatBadge: String = "AUDIO",
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 44100,
    val folderName: String = "Biblioteca",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val lyricsLrc: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false
)

@Serializable
data class TrackBookmark(
    val id: Long = 0L,
    val trackId: String,
    val timeMs: Long,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Playlist(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val colorHex: Long = 0xFF6750A4,
    val artworkUri: String? = null,
    val iconKey: String = "music_note",
    val createdAt: Long = System.currentTimeMillis()
)

data class PlaylistWithTracks(
    val playlist: Playlist,
    val tracks: List<Track>
)

@Serializable
data class EqPreset(
    val name: String,
    val bandLevelsMb: List<Int>,
    val bassStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val preampDb: Float = 0f,
    val stereoBalance: Float = 0f,
    val isCustom: Boolean = false
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val isTimed: Boolean = true
)

@Serializable
data class SyncLogEntry(
    val timestampMs: Long,
    val action: String,
    val detail: String
)

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class LibraryFilter(val label: String) {
    ALL("Todas"),
    FAVORITES("Favoritas"),
    LOSSLESS("Lossless / Hi-Res"),
    STREAMS("Streams"),
    RECENT("Recientes")
}

enum class LibrarySortOrder(val label: String) {
    RECENT_ADDED("Más recientes"),
    TITLE_ASC("Título A–Z"),
    ARTIST_ASC("Artista A–Z"),
    DURATION_DESC("Mayor duración"),
    MOST_PLAYED("Más reproducidas")
}

enum class LibraryBrowseTab(val label: String) {
    TRACKS("Pistas"),
    ALBUMS("Álbumes"),
    ARTISTS("Artistas"),
    FOLDERS("Carpetas")
}

enum class SmartPlaylistType(val title: String, val subtitle: String) {
    MOST_PLAYED("Más escuchadas", "Tus canciones con más reproducciones"),
    RECENTLY_ADDED("Recién añadidas", "Últimas incorporaciones a tu biblioteca"),
    FAVORITES("Favoritas", "Todas las pistas marcadas con corazón"),
    WITH_LYRICS("Con letra en vivo", "Canciones listas para karaoke y LRC")
}
