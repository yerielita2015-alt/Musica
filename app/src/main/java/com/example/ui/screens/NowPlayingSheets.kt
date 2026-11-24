package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.lyrics.LrcParser
import com.example.data.model.LyricLine
import com.example.data.model.TrackBookmark
import com.example.data.prefs.LyricsAlignOption
import com.example.data.prefs.LyricsSizeOption
import com.example.player.PlaybackUiState
import com.example.ui.components.formatDurationMs

@Composable
fun SyncedLyricsView(
    lyrics: List<LyricLine>,
    positionMs: Long,
    lyricsSize: LyricsSizeOption,
    lyricsAlign: LyricsAlignOption,
    keepScreenOn: Boolean,
    onSeekToLine: (Long) -> Unit,
    onShiftOffset: (Long) -> Unit,
    onEditLyrics: () -> Unit,
    onImportLrc: () -> Unit
) {
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        val previous = view.keepScreenOn
        if (keepScreenOn) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = previous
        }
    }

    val listState = rememberLazyListState()
    val activeIdx = LrcParser.findActiveLineIndex(lyrics, positionMs)

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0 && lyrics.isNotEmpty()) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
        }
    }

    val textAlign = if (lyricsAlign == LyricsAlignOption.CENTER) TextAlign.Center else TextAlign.Start
    val activeSizeSp = (20f * lyricsSize.multiplier).sp
    val inactiveSizeSp = (16f * lyricsSize.multiplier).sp

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (lyrics.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = { onShiftOffset(-500L) }) {
                        Text("-0.5s")
                    }
                    TextButton(onClick = { onShiftOffset(500L) }) {
                        Text("+0.5s")
                    }
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Row {
                TextButton(onClick = onImportLrc) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(".LRC")
                }
                TextButton(onClick = onEditLyrics) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Editar")
                }
            }
        }

        if (lyrics.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.Lyrics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text("Sin letra sincronizada", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Importa un archivo .lrc o escribe la letra para seguirla en tiempo real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onEditLyrics) {
                        Text("Añadir letra")
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val isCurrent = index == activeIdx
                    Text(
                        text = line.text,
                        fontSize = if (isCurrent) activeSizeSp else inactiveSizeSp,
                        lineHeight = if (isCurrent) (27f * lyricsSize.multiplier).sp else (22f * lyricsSize.multiplier).sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        },
                        textAlign = textAlign,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onSeekToLine(line.timeMs) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksSheet(
    bookmarks: List<TrackBookmark>,
    currentPositionMs: Long,
    onDismiss: () -> Unit,
    onAddBookmark: (Long, String) -> Unit,
    onJumpTo: (Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit
) {
    var label by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Marcadores de tiempo", style = MaterialTheme.typography.titleLarge)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("Etiqueta (ej. Coro, Solo)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onAddBookmark(currentPositionMs, label.ifBlank { "Punto ${formatDurationMs(currentPositionMs)}" })
                        label = ""
                    }
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatDurationMs(currentPositionMs))
                }
            }

            if (bookmarks.isEmpty()) {
                Text(
                    "Guarda instantes exactos de la canción para volver a ellos con un toque.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bookmarks, key = { it.id }) { bm ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onJumpTo(bm.timeMs)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bm.label, style = MaterialTheme.typography.titleSmall)
                                    Text(formatDurationMs(bm.timeMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar marcador")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    onDismiss: () -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onSleepAfterTrack: (Boolean) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Temporizador de apagado", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "El volumen se atenuará suavemente en los últimos 10 segundos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 15, 30, 45, 60).forEach { min ->
                    FilledTonalButton(
                        onClick = {
                            onStartSleepTimer(min)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("${min}m", maxLines = 1)
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    onSleepAfterTrack(true)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Detener al terminar esta canción")
            }
            TextButton(
                onClick = {
                    onStartSleepTimer(0)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Desactivar temporizador")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    playbackState: PlaybackUiState,
    onDismiss: () -> Unit,
    onJumpToIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onShuffleQueue: () -> Unit,
    onClearQueue: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Cola (${playbackState.queue.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    TextButton(
                        onClick = onShuffleQueue,
                        enabled = playbackState.queue.size > 2
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mezclar")
                    }
                    TextButton(
                        onClick = onClearQueue,
                        enabled = playbackState.queue.size > 1
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Limpiar")
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(playbackState.queue) { idx, qTrack ->
                    val active = idx == playbackState.currentIndex
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (active) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onJumpToIndex(idx)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    qTrack.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    qTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (idx > 0) {
                                IconButton(
                                    onClick = { onMoveItem(idx, idx - 1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir", modifier = Modifier.size(18.dp))
                                }
                            }
                            if (idx < playbackState.queue.lastIndex) {
                                IconButton(
                                    onClick = { onMoveItem(idx, idx + 1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Bajar", modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = { onRemoveItem(idx) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Quitar de cola", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
