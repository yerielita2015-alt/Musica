package com.example.data.local

import com.example.data.model.EqPreset
import com.example.data.model.Playlist
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SyncLogEntry
import com.example.data.model.Track
import com.example.data.model.TrackBookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SonoraRepository(private val dao: SonoraDao) {

    val tracksFlow: Flow<List<Track>> = dao.observeAllTracks().map { list ->
        list.map { it.toDomain() }
    }

    val playlistsFlow: Flow<List<PlaylistWithTracks>> = combine(
        dao.observePlaylistsWithTracks(),
        dao.observeAllCrossRefs()
    ) { playlists, refs ->
        val refOrderByPlaylist = refs.groupBy { it.playlistId }
        playlists.map { pwr ->
            val trackMap = pwr.tracks.associateBy { it.id }
            val orderedRefs = refOrderByPlaylist[pwr.playlist.id].orEmpty().sortedBy { it.position }
            val orderedTracks = orderedRefs.mapNotNull { trackMap[it.trackId]?.toDomain() }
            PlaylistWithTracks(
                playlist = pwr.playlist.toDomain(),
                tracks = orderedTracks
            )
        }
    }

    val customEqPresetsFlow: Flow<List<EqPreset>> = dao.observeCustomEqPresets().map { list ->
        list.map { it.toDomain() }
    }

    val syncHistoryFlow: Flow<List<SyncLogEntry>> = dao.observeSyncHistory().map { list ->
        list.map { it.toDomain() }
    }

    fun observeBookmarksForTrack(trackId: String): Flow<List<TrackBookmark>> =
        dao.observeBookmarksForTrack(trackId).map { list -> list.map { it.toDomain() } }

    suspend fun getAllBookmarksOnce(): List<TrackBookmark> =
        dao.getAllBookmarksOnce().map { it.toDomain() }

    suspend fun addBookmark(trackId: String, timeMs: Long, label: String) {
        dao.insertBookmark(
            TrackBookmarkEntity(
                trackId = trackId,
                timeMs = timeMs.coerceAtLeast(0L),
                label = label.trim().ifEmpty { "Marcador" }
            )
        )
    }

    suspend fun deleteBookmark(bookmarkId: Long) {
        dao.deleteBookmark(bookmarkId)
    }

    suspend fun getAllTracksOnce(): List<Track> = dao.getAllTracksOnce().map { it.toDomain() }

    suspend fun getAllPlaylistsOnce(): List<PlaylistWithTracks> {
        val tracksById = dao.getAllTracksOnce().associateBy { it.id }
        return dao.getAllPlaylistsOnce().map { playlistEntity ->
            val refs = dao.getCrossRefsForPlaylist(playlistEntity.id).sortedBy { it.position }
            PlaylistWithTracks(
                playlist = playlistEntity.toDomain(),
                tracks = refs.mapNotNull { tracksById[it.trackId]?.toDomain() }
            )
        }
    }

    suspend fun getCustomEqPresetsOnce(): List<EqPreset> =
        dao.getCustomEqPresetsOnce().map { it.toDomain() }

    suspend fun upsertTracks(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            dao.upsertTracks(tracks.map { it.toEntity() })
        }
    }

    suspend fun upsertTrack(track: Track) {
        dao.upsertTrack(track.toEntity())
    }

    suspend fun toggleFavorite(trackId: String, favorite: Boolean) {
        dao.setFavorite(trackId, favorite)
    }

    suspend fun recordPlay(trackId: String) {
        dao.recordPlay(trackId, System.currentTimeMillis())
    }

    suspend fun updateLyrics(trackId: String, lyricsLrc: String) {
        dao.updateLyrics(trackId, lyricsLrc)
    }

    suspend fun updateTrackMetadata(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        folderName: String
    ) {
        dao.updateTrackMetadata(
            trackId = trackId,
            title = title.trim(),
            artist = artist.trim().ifEmpty { "Artista desconocido" },
            album = album.trim().ifEmpty { "Álbum desconocido" },
            folderName = folderName.trim().ifEmpty { "Biblioteca" }
        )
    }

    suspend fun updateDurationIfMissing(trackId: String, durationMs: Long) {
        if (durationMs > 0L) {
            dao.updateDurationIfMissing(trackId, durationMs)
        }
    }

    suspend fun deleteTrack(trackId: String) {
        dao.deleteTrack(trackId)
    }

    suspend fun createPlaylist(
        name: String,
        description: String,
        colorHex: Long,
        artworkUri: String? = null,
        iconKey: String = "music_note"
    ): Long {
        return dao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                description = description.trim(),
                colorHex = colorHex,
                artworkUri = artworkUri,
                iconKey = iconKey
            )
        )
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        dao.updatePlaylist(
            playlistId = playlist.id,
            name = playlist.name.trim(),
            description = playlist.description.trim(),
            colorHex = playlist.colorHex,
            artworkUri = playlist.artworkUri,
            iconKey = playlist.iconKey
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        val nextPos = dao.nextPlaylistPosition(playlistId)
        dao.upsertPlaylistTrackRef(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = nextPos
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        dao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun saveCustomEqPreset(preset: EqPreset) {
        dao.upsertEqPreset(
            EqPresetEntity(
                name = preset.name.trim(),
                bandLevelsCsv = preset.bandLevelsMb.joinToString(","),
                bassStrength = preset.bassStrength,
                virtualizerStrength = preset.virtualizerStrength,
                loudnessGainMb = preset.loudnessGainMb,
                preampDb = preset.preampDb,
                stereoBalance = preset.stereoBalance,
                isCustom = true
            )
        )
    }

    suspend fun deleteCustomEqPreset(name: String) {
        dao.deleteEqPreset(name)
    }

    suspend fun addSyncLog(action: String, detail: String) {
        dao.insertSyncLog(
            SyncHistoryEntity(
                timestampMs = System.currentTimeMillis(),
                action = action,
                detail = detail
            )
        )
    }
}
