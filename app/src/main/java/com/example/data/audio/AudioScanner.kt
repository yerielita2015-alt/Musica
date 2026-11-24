package com.example.data.audio

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object AudioScanner {

    private val knownExtensions = setOf(
        "MP3", "FLAC", "WAV", "OPUS", "OGG", "OGA", "AAC", "M4A", "ALAC",
        "AIFF", "AIF", "WMA", "APE", "WV", "DSF", "DFF", "AMR", "MID",
        "MIDI", "MKA", "WEBM", "MP2", "AC3", "EAC3", "DTS"
    )

    suspend fun importDocumentUris(context: Context, uris: List<Uri>): List<Track> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val imported = mutableListOf<Track>()
        for (uri in uris) {
            try {
                resolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }

            val track = extractTrackFromUri(context, uri)
            if (track != null) {
                imported.add(track)
            }
        }
        imported
    }

    private fun extractTrackFromUri(context: Context, uri: Uri): Track? {
        val resolver = context.contentResolver
        var displayName = "Pista de audio"
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        displayName = cursor.getString(idx) ?: displayName
                    }
                }
            }
        } catch (_: Exception) {
        }

        val extBadge = detectFormatBadge(displayName, resolver.getType(uri))
        val id = "uri_" + sha1Short(uri.toString())
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: displayName.substringBeforeLast('.')
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: "Artista desconocido"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?: "Archivo local"
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val bitrate = ((retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull() ?: 0) / 1000).coerceAtLeast(0)
            val sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                ?.toIntOrNull()?.takeIf { it > 0 } ?: 44100

            val artworkBytes = retriever.embeddedPicture
            val artworkUri = if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                val artDir = File(context.filesDir, "art_cache").apply { mkdirs() }
                val artFile = File(artDir, "$id.jpg")
                FileOutputStream(artFile).use { it.write(artworkBytes) }
                Uri.fromFile(artFile).toString()
            } else {
                null
            }

            Track(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                uri = uri.toString(),
                artworkUri = artworkUri,
                formatBadge = extBadge,
                bitrateKbps = bitrate,
                sampleRateHz = sampleRate,
                folderName = "Importados",
                addedAt = System.currentTimeMillis(),
                isDemo = false
            )
        } catch (_: Exception) {
            Track(
                id = id,
                title = displayName.substringBeforeLast('.').ifBlank { displayName },
                artist = "Archivo local",
                album = extBadge,
                durationMs = 0L,
                uri = uri.toString(),
                artworkUri = null,
                formatBadge = extBadge,
                folderName = "Importados",
                addedAt = System.currentTimeMillis(),
                isDemo = false
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun scanDeviceMediaStore(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} > 5000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, mediaId)
                    val albumId = cursor.getLong(albumIdCol)
                    val artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else null
                    val displayName = cursor.getString(nameCol) ?: ""
                    val mime = cursor.getString(mimeCol)
                    val formatBadge = detectFormatBadge(displayName, mime)

                    tracks.add(
                        Track(
                            id = "ms_$mediaId",
                            title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: displayName,
                            artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" && it.isNotBlank() }
                                ?: "Artista desconocido",
                            album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Dispositivo",
                            durationMs = cursor.getLong(durationCol).coerceAtLeast(0L),
                            uri = contentUri.toString(),
                            artworkUri = artworkUri,
                            formatBadge = formatBadge,
                            folderName = "Dispositivo",
                            addedAt = System.currentTimeMillis(),
                            isDemo = false
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        }
        tracks
    }

    fun buildStreamTrack(url: String, title: String, artist: String): Track {
        val cleanUrl = url.trim()
        val extBadge = when {
            cleanUrl.contains(".flac", ignoreCase = true) -> "FLAC"
            cleanUrl.contains(".m3u8", ignoreCase = true) -> "HLS"
            cleanUrl.contains(".mpd", ignoreCase = true) -> "DASH"
            cleanUrl.contains(".opus", ignoreCase = true) -> "OPUS"
            cleanUrl.contains(".ogg", ignoreCase = true) -> "OGG"
            cleanUrl.contains(".aac", ignoreCase = true) -> "AAC"
            cleanUrl.contains(".wav", ignoreCase = true) -> "WAV"
            cleanUrl.contains(".mp3", ignoreCase = true) -> "MP3"
            else -> "STREAM"
        }
        return Track(
            id = "stream_" + sha1Short(cleanUrl),
            title = title.trim().ifBlank { cleanUrl.substringAfterLast('/').substringBefore('?').ifBlank { "Stream en vivo" } },
            artist = artist.trim().ifBlank { "Audio en red" },
            album = "Transmisión en línea",
            durationMs = 0L,
            uri = cleanUrl,
            formatBadge = extBadge,
            folderName = "Streams",
            addedAt = System.currentTimeMillis(),
            isDemo = false
        )
    }

    private fun detectFormatBadge(fileName: String, mimeType: String?): String {
        val ext = fileName.substringAfterLast('.', "").uppercase()
        if (ext in knownExtensions) return ext
        if (ext.isNotBlank() && ext.length <= 5 && ext.all { it.isLetterOrDigit() }) return ext
        return when {
            mimeType?.contains("flac", true) == true -> "FLAC"
            mimeType?.contains("opus", true) == true -> "OPUS"
            mimeType?.contains("ogg", true) == true -> "OGG"
            mimeType?.contains("wav", true) == true -> "WAV"
            mimeType?.contains("aac", true) == true -> "AAC"
            mimeType?.contains("alac", true) == true -> "ALAC"
            mimeType?.contains("aiff", true) == true -> "AIFF"
            mimeType?.contains("mp4", true) == true || mimeType?.contains("m4a", true) == true -> "M4A"
            mimeType?.contains("mpeg", true) == true -> "MP3"
            mimeType?.contains("webm", true) == true -> "WEBM"
            mimeType?.contains("matroska", true) == true -> "MKA"
            else -> "AUDIO"
        }
    }

    private fun sha1Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}
