package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.lyrics.LrcParser
import com.example.data.model.Track
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LyricsEditorDialog(
    initialLrc: String,
    currentPositionMs: Long,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onImportLrcUri: ((Uri) -> Unit)? = null,
    onExportLrcUri: ((Uri) -> Unit)? = null
) {
    var lrcText by remember(initialLrc) { mutableStateOf(initialLrc) }
    val lrcPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportLrcUri?.invoke(uri)
            onDismiss()
        }
    }
    val lrcExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            onExportLrcUri?.invoke(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editor de Letra (.LRC)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Usa marcas [mm:ss.xx] al inicio de cada línea o inserta el tiempo actual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onImportLrcUri != null) {
                        OutlinedButton(
                            onClick = { lrcPicker.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Importar", maxLines = 1)
                        }
                    }
                    if (onExportLrcUri != null && lrcText.isNotBlank()) {
                        OutlinedButton(
                            onClick = { lrcExporter.launch("letra_sonora.lrc") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Exportar", maxLines = 1)
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        val stamp = LrcParser.formatTimestamp(currentPositionMs)
                        lrcText = if (lrcText.isBlank()) "$stamp " else "$lrcText\n$stamp "
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Insertar marca ${LrcParser.formatTimestamp(currentPositionMs)}")
                }
                OutlinedTextField(
                    value = lrcText,
                    onValueChange = { lrcText = it },
                    label = { Text("Letra (.lrc o texto)") },
                    minLines = 5,
                    maxLines = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(lrcText) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun MetadataEditorDialog(
    track: Track,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, album: String, folderName: String) -> Unit
) {
    var title by remember(track.id) { mutableStateOf(track.title) }
    var artist by remember(track.id) { mutableStateOf(track.artist) }
    var album by remember(track.id) { mutableStateOf(track.album) }
    var folderName by remember(track.id) { mutableStateOf(track.folderName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar metadatos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Álbum") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Carpeta / Colección") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, artist, album, folderName) },
                enabled = title.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun TrackTechnicalInfoDialog(
    track: Track,
    onDismiss: () -> Unit
) {
    val dateStr = remember(track.addedAt) {
        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(track.addedAt))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ficha Técnica de Audio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TechRow("Título", track.title)
                TechRow("Artista", track.artist)
                TechRow("Álbum", track.album)
                TechRow("Formato / Códec", track.formatBadge)
                TechRow("Frecuencia de muestreo", if (track.sampleRateHz > 0) "${track.sampleRateHz} Hz" else "Automática")
                TechRow("Bitrate", if (track.bitrateKbps > 0) "${track.bitrateKbps} kbps" else "Variable / Sin pérdida")
                TechRow("Duración", formatDurationMs(track.durationMs))
                TechRow("Carpeta", track.folderName)
                TechRow("Reproducciones", "${track.playCount}")
                TechRow("Letra", if (track.lyricsLrc.isNotBlank()) "Disponible (.LRC)" else "Sin letra")
                TechRow("Añadida", dateStr)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ListeningStatsDialog(
    tracks: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    val totalPlays = remember(tracks) { tracks.sumOf { it.playCount } }
    val totalDurationMs = remember(tracks) { tracks.sumOf { it.durationMs.coerceAtLeast(0L) } }
    val formatCounts = remember(tracks) {
        tracks.groupingBy { it.formatBadge }.eachCount().entries.sortedByDescending { it.value }.take(5)
    }
    val topTracks = remember(tracks) {
        tracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(5)
    }
    val totalMins = (totalDurationMs / 60_000L)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.BarChart, contentDescription = null) },
        title = { Text("Estadísticas de Biblioteca") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatPill("${tracks.size}", "Pistas")
                            StatPill("$totalPlays", "Escuchas")
                            StatPill("${totalMins}m", "Duración")
                        }
                    }
                }
                if (formatCounts.isNotEmpty()) {
                    item {
                        Text("Formatos", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            formatCounts.forEach { (fmt, count) ->
                                val ratio = (count.toFloat() / tracks.size.coerceAtLeast(1)).coerceIn(0f, 1f)
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(fmt, style = MaterialTheme.typography.labelMedium)
                                        Text("$count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier.fillMaxWidth().height(5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (topTracks.isNotEmpty()) {
                    item {
                        Text("Más escuchadas", style = MaterialTheme.typography.labelLarge)
                    }
                    items(topTracks, key = { it.id }) { t ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPlayTrack(t)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${t.artist} · ${t.playCount} rep.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun StatPill(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun shareTrackIntent(context: Context, track: Track) {
    val lyricsPreview = if (track.lyricsLrc.isNotBlank()) {
        val lines = LrcParser.parse(track.lyricsLrc).take(4).joinToString("\n") { it.text }
        "\n\n\"$lines\""
    } else {
        ""
    }
    val text = "Escuchando ${track.title} — ${track.artist} (${track.formatBadge}) en Sonora$lyricsPreview"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${track.title} — ${track.artist}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir pista"))
}
