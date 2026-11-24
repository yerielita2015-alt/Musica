package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.data.model.EqPreset
import com.example.data.model.Playlist
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SyncLogEntry
import com.example.data.model.Track
import com.example.data.model.TrackBookmark

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
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

@Entity(
    tableName = "track_bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId")]
)
data class TrackBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackId: String,
    val timeMs: Long,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val colorHex: Long = 0xFF6750A4,
    val artworkUri: String? = null,
    val iconKey: String = "music_note",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_track_cross_ref",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId"), Index("playlistId")]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey val name: String,
    val bandLevelsCsv: String,
    val bassStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val preampDb: Float = 0f,
    val stereoBalance: Float = 0f,
    val isCustom: Boolean = true
)

@Entity(tableName = "sync_history")
data class SyncHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMs: Long,
    val action: String,
    val detail: String
)

data class PlaylistWithTrackRefs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistTrackCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "trackId"
        )
    )
    val tracks: List<TrackEntity>
)

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    uri = uri,
    artworkUri = artworkUri,
    formatBadge = formatBadge,
    bitrateKbps = bitrateKbps,
    sampleRateHz = sampleRateHz,
    folderName = folderName,
    isFavorite = isFavorite,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt,
    lyricsLrc = lyricsLrc,
    addedAt = addedAt,
    isDemo = isDemo
)

fun Track.toEntity(): TrackEntity = TrackEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    uri = uri,
    artworkUri = artworkUri,
    formatBadge = formatBadge,
    bitrateKbps = bitrateKbps,
    sampleRateHz = sampleRateHz,
    folderName = folderName,
    isFavorite = isFavorite,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt,
    lyricsLrc = lyricsLrc,
    addedAt = addedAt,
    isDemo = isDemo
)

fun TrackBookmarkEntity.toDomain(): TrackBookmark = TrackBookmark(
    id = id,
    trackId = trackId,
    timeMs = timeMs,
    label = label,
    createdAt = createdAt
)

fun PlaylistEntity.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    colorHex = colorHex,
    artworkUri = artworkUri,
    iconKey = iconKey,
    createdAt = createdAt
)

fun EqPresetEntity.toDomain(): EqPreset = EqPreset(
    name = name,
    bandLevelsMb = bandLevelsCsv.split(",").mapNotNull { it.trim().toIntOrNull() },
    bassStrength = bassStrength,
    virtualizerStrength = virtualizerStrength,
    loudnessGainMb = loudnessGainMb,
    preampDb = preampDb,
    stereoBalance = stereoBalance,
    isCustom = isCustom
)

fun SyncHistoryEntity.toDomain(): SyncLogEntry = SyncLogEntry(
    timestampMs = timestampMs,
    action = action,
    detail = detail
)

fun PlaylistWithTrackRefs.toDomain(): PlaylistWithTracks = PlaylistWithTracks(
    playlist = playlist.toDomain(),
    tracks = tracks.map { it.toDomain() }
)
