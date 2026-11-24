package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.model.EqPreset
import com.example.data.model.Playlist
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.Track
import com.example.data.model.TrackBookmark
import com.example.data.prefs.UserCustomization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SonoraCloudBackup(
    val schemaVersion: Int = 2,
    val exportedAtMs: Long = System.currentTimeMillis(),
    val tracks: List<Track> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList(),
    val eqPresets: List<EqPreset> = emptyList(),
    val bookmarks: List<TrackBookmark> = emptyList(),
    val customization: UserCustomization = UserCustomization()
)

@Serializable
data class BackupPlaylist(
    val playlist: Playlist,
    val trackIds: List<String>
)

object CloudBackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        tracks: List<Track>,
        playlists: List<PlaylistWithTracks>,
        eqPresets: List<EqPreset>,
        bookmarks: List<TrackBookmark>,
        customization: UserCustomization
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val payload = SonoraCloudBackup(
                exportedAtMs = System.currentTimeMillis(),
                tracks = tracks,
                playlists = playlists.map { p ->
                    BackupPlaylist(
                        playlist = p.playlist,
                        trackIds = p.tracks.map { it.id }
                    )
                },
                eqPresets = eqPresets,
                bookmarks = bookmarks,
                customization = customization
            )
            val encoded = json.encodeToString(payload)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(encoded.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("No se pudo abrir el destino para guardar el respaldo.")
            Result.success(playlists.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromUri(context: Context, uri: Uri): Result<SonoraCloudBackup> = withContext(Dispatchers.IO) {
        try {
            val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("No se pudo leer el archivo de respaldo.")
            val decoded = json.decodeFromString<SonoraCloudBackup>(raw)
            Result.success(decoded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildM3uContent(playlistWithTracks: PlaylistWithTracks): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine("#PLAYLIST:${playlistWithTracks.playlist.name}")
        playlistWithTracks.tracks.forEach { track ->
            val sec = (track.durationMs / 1000L).coerceAtLeast(0L)
            sb.appendLine("#EXTINF:$sec,${track.artist} - ${track.title}")
            sb.appendLine(track.uri)
        }
        return sb.toString()
    }

    suspend fun exportM3uPlaylist(
        context: Context,
        uri: Uri,
        playlistWithTracks: PlaylistWithTracks
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val content = buildM3uContent(playlistWithTracks)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("No se pudo guardar el archivo .M3U8.")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportLrcText(
        context: Context,
        uri: Uri,
        lrcText: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(lrcText.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("No se pudo guardar el archivo .LRC.")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
