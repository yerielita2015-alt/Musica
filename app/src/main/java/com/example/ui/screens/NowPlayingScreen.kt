package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.lyrics.LrcParser
import com.example.data.model.LyricLine
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import com.example.data.model.TrackBookmark
import com.example.data.prefs.NowPlayingStyle
import com.example.data.prefs.UserCustomization
import com.example.player.PlaybackUiState
import com.example.ui.components.AlbumArtworkView
import com.example.ui.components.AudioSpectrumVisualizer
import com.example.ui.components.FormatBadgeChip
import com.example.ui.components.LyricsEditorDialog
import com.example.ui.components.MetadataEditorDialog
import com.example.ui.components.TrackTechnicalInfoDialog
import com.example.ui.components.VinylDiscView
import com.example.ui.components.formatDurationMs
import com.example.ui.components.shareTrackIntent

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    lyrics: List<LyricLine>,
    bookmarks: List<TrackBookmark>,
    spectrumBands: List<Float>,
    customization: UserCustomization,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBySeconds: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onUpdateMetadata: (String, String, String, String, String) -> Unit,
    onUpdateLyrics: (String, String) -> Unit,
    onShiftLyricsOffset: (Track, Long) -> Unit,
    onImportLrcFile: (String, Uri) -> Unit,
    onExportLrcFile: (Track, Uri) -> Unit,
    onAddBookmark: (String, Long, String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onSetAbLoopA: () -> Unit,
    onSetAbLoopB: () -> Unit,
    onClearAbLoop: () -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onSleepAfterTrack: (Boolean) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onShuffleQueue: () -> Unit,
    onClearQueue: () -> Unit,
    onJumpToQueueIndex: (Int) -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val track = playbackState.currentTrack
    var showLyricsPane by remember { mutableStateOf(false) }
    var showLyricsEditor by remember { mutableStateOf(false) }
    var showMetadataEditor by remember { mutableStateOf(false) }
    var showTechInfo by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showAbLoopBar by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }

    var isScrubbing by remember(track?.id) { mutableStateOf(false) }
    var scrubPosition by remember(track?.id) { mutableFloatStateOf(0f) }

    val lrcFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null && track != null) onImportLrcFile(track.id, uri) }

    if (track == null) {
        Box(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nada reproduciéndose", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = onBack) { Text("Ir a la Biblioteca") }
            }
        }
        return
    }

    val primary = MaterialTheme.colorScheme.primary
    val duration = playbackState.durationMs.coerceAtLeast(track.durationMs).coerceAtLeast(1L)
    val displayPos = if (isScrubbing) scrubPosition.toLong().coerceIn(0L, duration) else playbackState.positionMs.coerceIn(0L, duration)
    val abActive = playbackState.abLoopStartMs != null || playbackState.abLoopEndMs != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize().testTag("now_playing_close_button")) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Minimizar reproductor")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REPRODUCIENDO", style = MaterialTheme.typography.labelSmall, color = primary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormatBadgeChip(badge = track.formatBadge)
                    if (track.sampleRateHz > 0) {
                        Text("${track.sampleRateHz / 1000.0} kHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row {
                IconButton(
                    onClick = { showLyricsPane = !showLyricsPane },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("toggle_lyrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = "Letras en tiempo real",
                        tint = if (showLyricsPane) primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("now_playing_eq_button")
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = "Abrir ecualizador")
                }
                Box {
                    IconButton(onClick = { showTopMenu = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                        DropdownMenuItem(text = { Text("Bucle A-B") }, leadingIcon = { Icon(Icons.Rounded.Repeat, null) }, onClick = { showTopMenu = false; showAbLoopBar = !showAbLoopBar })
                        DropdownMenuItem(text = { Text("Marcadores (${bookmarks.size})") }, leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, null) }, onClick = { showTopMenu = false; showBookmarksSheet = true })
                        DropdownMenuItem(text = { Text("Editar metadatos") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { showTopMenu = false; showMetadataEditor = true })
                        DropdownMenuItem(text = { Text("Editar / exportar letra") }, leadingIcon = { Icon(Icons.Rounded.EditNote, null) }, onClick = { showTopMenu = false; showLyricsEditor = true })
                        DropdownMenuItem(text = { Text("Ficha técnica") }, leadingIcon = { Icon(Icons.Rounded.Info, null) }, onClick = { showTopMenu = false; showTechInfo = true })
                        DropdownMenuItem(text = { Text("Compartir pista") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = { showTopMenu = false; shareTrackIntent(context, track) })
                    }
                }
            }
        }

        // Optional A-B Loop Bar
        if (showAbLoopBar || abActive) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bucle A-B: ${playbackState.abLoopStartMs?.let { formatDurationMs(it) } ?: "--"} → ${playbackState.abLoopEndMs?.let { formatDurationMs(it) } ?: "--"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = playbackState.abLoopStartMs != null, onClick = onSetAbLoopA, label = { Text("A") })
                        FilterChip(selected = playbackState.abLoopEndMs != null, onClick = onSetAbLoopB, label = { Text("B") })
                        IconButton(onClick = { onClearAbLoop(); showAbLoopBar = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar bucle A-B", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Center Visual Stage OR Synced Real-Time Lyrics
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 6.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = if (customization.useTonalContainerBackdrop) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.background,
            tonalElevation = if (customization.useTonalContainerBackdrop) 2.dp else 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(targetState = showLyricsPane, label = "center_stage") { lyricsMode ->
                    if (lyricsMode) {
                        SyncedLyricsView(
                            lyrics = lyrics,
                            positionMs = displayPos,
                            lyricsSize = customization.lyricsSize,
                            lyricsAlign = customization.lyricsAlign,
                            keepScreenOn = customization.keepScreenOnLyrics,
                            onSeekToLine = onSeekTo,
                            onShiftOffset = { delta -> onShiftLyricsOffset(track, delta) },
                            onEditLyrics = { showLyricsEditor = true },
                            onImportLrc = { lrcFilePicker.launch(arrayOf("*/*")) }
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (customization.nowPlayingStyle) {
                                NowPlayingStyle.VINYL_DISC -> {
                                    VinylDiscView(
                                        artworkUri = track.artworkUri,
                                        isPlaying = playbackState.isPlaying,
                                        useTonalRing = customization.useTonalContainerBackdrop,
                                        modifier = Modifier.size(236.dp)
                                    )
                                }
                                NowPlayingStyle.COVER_CARD -> {
                                    AlbumArtworkView(
                                        artworkUri = track.artworkUri,
                                        contentDescription = track.title,
                                        cornerRadius = customization.artworkShape.cornerDp.dp,
                                        modifier = Modifier.size(236.dp)
                                    )
                                }
                                NowPlayingStyle.SPECTRUM -> {
                                    Box(modifier = Modifier.size(236.dp), contentAlignment = Alignment.Center) {
                                        AudioSpectrumVisualizer(
                                            bands = spectrumBands,
                                            style = customization.visualizerStyle,
                                            isPlaying = playbackState.isPlaying,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        AlbumArtworkView(
                                            artworkUri = track.artworkUri,
                                            contentDescription = track.title,
                                            cornerRadius = 80.dp,
                                            modifier = Modifier.size(100.dp)
                                        )
                                    }
                                }
                            }

                            if (customization.nowPlayingStyle != NowPlayingStyle.SPECTRUM) {
                                Spacer(Modifier.height(12.dp))
                                AudioSpectrumVisualizer(
                                    bands = spectrumBands,
                                    style = customization.visualizerStyle,
                                    isPlaying = playbackState.isPlaying,
                                    modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 12.dp)
                                )
                            }

                            val activeIdx = LrcParser.findActiveLineIndex(lyrics, displayPos)
                            if (activeIdx in lyrics.indices) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = lyrics[activeIdx].text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = primary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { showLyricsPane = true }.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Track Title + Favorite
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${track.artist} · ${track.album}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(
                onClick = { onToggleFavorite(track) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("now_playing_favorite_button")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (track.isFavorite) primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Seekbar + Timestamps
        Slider(
            value = displayPos.toFloat(),
            onValueChange = { isScrubbing = true; scrubPosition = it },
            onValueChangeFinished = { onSeekTo(scrubPosition.toLong()); isScrubbing = false },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth().testTag("now_playing_seek_slider")
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDurationMs(displayPos), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDurationMs(duration), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(6.dp))

        // Main Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Rounded.Shuffle, contentDescription = "Aleatorio", tint = if (playbackState.shuffleEnabled) primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (customization.showQuickSeekButtons) {
                IconButton(onClick = { onSeekBySeconds(-customization.seekStepSeconds) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.Replay10, contentDescription = "Retroceder ${customization.seekStepSeconds}s")
                }
            }
            IconButton(onClick = onPrevious, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(32.dp))
            }
            FilledIconButton(
                onClick = onTogglePlayPause,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                modifier = Modifier.size(62.dp).testTag("now_playing_play_pause")
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(32.dp))
            }
            if (customization.showQuickSeekButtons) {
                IconButton(onClick = { onSeekBySeconds(customization.seekStepSeconds) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.Forward10, contentDescription = "Adelantar ${customization.seekStepSeconds}s")
                }
            }
            IconButton(onClick = onCycleRepeat, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(
                    imageVector = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repetir",
                    tint = if (playbackState.repeatMode != RepeatMode.OFF) primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Bottom Utility Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showSleepTimerSheet = true }) {
                Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                val rem = playbackState.sleepTimerRemainingSec
                Text(
                    text = when {
                        rem != null -> "${rem / 60}:${"%02d".format(rem % 60)}"
                        playbackState.sleepAfterCurrentTrack -> "Fin pista"
                        else -> "Temporizador"
                    }
                )
            }

            TextButton(onClick = { showBookmarksSheet = true }) {
                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Marcas (${bookmarks.size})")
            }

            TextButton(onClick = { showQueueSheet = true }) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cola (${playbackState.queue.size})")
            }
        }
    }

    if (showLyricsEditor) {
        LyricsEditorDialog(
            initialLrc = track.lyricsLrc,
            currentPositionMs = displayPos,
            onDismiss = { showLyricsEditor = false },
            onSave = { newLrc -> onUpdateLyrics(track.id, newLrc); showLyricsEditor = false },
            onImportLrcUri = { uri -> onImportLrcFile(track.id, uri) },
            onExportLrcUri = { uri -> onExportLrcFile(track, uri) }
        )
    }

    if (showMetadataEditor) {
        MetadataEditorDialog(
            track = track,
            onDismiss = { showMetadataEditor = false },
            onSave = { title, artist, album, folder ->
                onUpdateMetadata(track.id, title, artist, album, folder)
                showMetadataEditor = false
            }
        )
    }

    if (showTechInfo) {
        TrackTechnicalInfoDialog(track = track, onDismiss = { showTechInfo = false })
    }

    if (showBookmarksSheet) {
        BookmarksSheet(
            bookmarks = bookmarks,
            currentPositionMs = displayPos,
            onDismiss = { showBookmarksSheet = false },
            onAddBookmark = { timeMs, label -> onAddBookmark(track.id, timeMs, label) },
            onJumpTo = onSeekTo,
            onDeleteBookmark = onDeleteBookmark
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerSheet(
            onDismiss = { showSleepTimerSheet = false },
            onStartSleepTimer = onStartSleepTimer,
            onSleepAfterTrack = onSleepAfterTrack
        )
    }

    if (showQueueSheet) {
        QueueSheet(
            playbackState = playbackState,
            onDismiss = { showQueueSheet = false },
            onJumpToIndex = onJumpToQueueIndex,
            onMoveItem = onMoveQueueItem,
            onRemoveItem = onRemoveFromQueue,
            onShuffleQueue = onShuffleQueue,
            onClearQueue = onClearQueue
        )
    }
}
