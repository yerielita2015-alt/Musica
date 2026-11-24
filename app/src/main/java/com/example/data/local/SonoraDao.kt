package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SonoraDao {

    @Query("SELECT * FROM tracks ORDER BY addedAt DESC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracksOnce(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun getTrackById(trackId: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isFavorite = :favorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: String, favorite: Boolean)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE id = :trackId")
    suspend fun recordPlay(trackId: String, playedAt: Long)

    @Query("UPDATE tracks SET lyricsLrc = :lyricsLrc WHERE id = :trackId")
    suspend fun updateLyrics(trackId: String, lyricsLrc: String)

    @Query("UPDATE tracks SET title = :title, artist = :artist, album = :album, folderName = :folderName WHERE id = :trackId")
    suspend fun updateTrackMetadata(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        folderName: String
    )

    @Query("UPDATE tracks SET durationMs = :durationMs WHERE id = :trackId AND durationMs <= 0")
    suspend fun updateDurationIfMissing(trackId: String, durationMs: Long)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: String)

    // Track Bookmarks
    @Query("SELECT * FROM track_bookmarks WHERE trackId = :trackId ORDER BY timeMs ASC")
    fun observeBookmarksForTrack(trackId: String): Flow<List<TrackBookmarkEntity>>

    @Query("SELECT * FROM track_bookmarks ORDER BY timeMs ASC")
    suspend fun getAllBookmarksOnce(): List<TrackBookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: TrackBookmarkEntity): Long

    @Query("DELETE FROM track_bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: Long)

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylistsWithTracks(): Flow<List<PlaylistWithTrackRefs>>

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylistsOnce(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_track_cross_ref WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getCrossRefsForPlaylist(playlistId: Long): List<PlaylistTrackCrossRef>

    @Query("SELECT * FROM playlist_track_cross_ref ORDER BY position ASC")
    fun observeAllCrossRefs(): Flow<List<PlaylistTrackCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, description = :description, colorHex = :colorHex, artworkUri = :artworkUri, iconKey = :iconKey WHERE id = :playlistId")
    suspend fun updatePlaylist(
        playlistId: Long,
        name: String,
        description: String,
        colorHex: Long,
        artworkUri: String?,
        iconKey: String
    )

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistTrackRef(ref: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun nextPlaylistPosition(playlistId: Long): Int

    @Query("SELECT * FROM eq_presets ORDER BY name ASC")
    fun observeCustomEqPresets(): Flow<List<EqPresetEntity>>

    @Query("SELECT * FROM eq_presets ORDER BY name ASC")
    suspend fun getCustomEqPresetsOnce(): List<EqPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEqPreset(preset: EqPresetEntity)

    @Query("DELETE FROM eq_presets WHERE name = :name")
    suspend fun deleteEqPreset(name: String)

    @Query("SELECT * FROM sync_history ORDER BY timestampMs DESC LIMIT 20")
    fun observeSyncHistory(): Flow<List<SyncHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(entry: SyncHistoryEntity)
}
