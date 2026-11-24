package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.LibraryBrowseTab
import com.example.data.model.LibraryFilter
import com.example.data.model.LibrarySortOrder
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.Track
import com.example.data.prefs.LibraryLayoutMode
import com.example.data.prefs.UserCustomization
import com.example.ui.components.AlbumArtworkView
import com.example.ui.components.FormatBadgeChip
import com.example.ui.components.ListeningStatsDialog
import com.example.ui.components.LyricsEditorDialog
import com.example.ui.components.MetadataEditorDialog
import com.example.ui.components.TrackTechnicalInfoDialog
import com.example.ui.components.formatDurationMs
import com.example.ui.components.shareTrackIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tracks: List<Track>,
    allTracks: List<Track>,
    playlists: List<PlaylistWithTracks>,
    currentTrackId: String?,
    isPlaying: Boolean,
    searchQuery: String,
    selectedFilter: LibraryFilter,
    customization: UserCustomization,
    onSearchChange: (String) -> Unit,
    onSelectFilter: (LibraryFilter) -> Unit,
    onSortOrderChange: (LibrarySortOrder) -> Unit,
    onLayoutModeChange: (LibraryLayoutMode) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onPlayGroup: (List<Track>) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onImportAudioUris: (List<Uri>) -> Unit,
    onScanDeviceMediaStore: () -> Unit,
    onAddStreamUrl: (String, String, String) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onUpdateMetadata: (String, String, String, String, String) -> Unit,
    onUpdateLyrics: (String, String) -> Unit,
    onImportLrcFile: (String, Uri) -> Unit,
    onExportLrcFile: (Track, Uri) -> Unit,
    onDeleteTrack: (Track) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var browseTab by remember { mutableStateOf(LibraryBrowseTab.TRACKS) }
    var showStreamDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var playlistTargetTrack by remember { mutableStateOf<Track?>(null) }
    var lyricsTargetTrack by remember { mutableStateOf<Track?>(null) }
    var metadataTargetTrack by remember { mutableStateOf<Track?>(null) }
    var techInfoTrack by remember { mutableStateOf<Track?>(null) }

    val openAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) onImportAudioUris(uris) }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val scanPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onScanDeviceMediaStore() else openAudioLauncher.launch(arrayOf("audio/*", "*/*"))
    }

    val groupedItems = remember(tracks, browseTab) {
        when (browseTab) {
            LibraryBrowseTab.ALBUMS -> tracks.groupBy { it.album.ifBlank { "Álbum desconocido" } }.entries.sortedBy { it.key.lowercase() }
            LibraryBrowseTab.ARTISTS -> tracks.groupBy { it.artist.ifBlank { "Artista desconocido" } }.entries.sortedBy { it.key.lowercase() }
            LibraryBrowseTab.FOLDERS -> tracks.groupBy { it.folderName.ifBlank { "Biblioteca" } }.entries.sortedBy { it.key.lowercase() }
            LibraryBrowseTab.TRACKS -> emptyList()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tu Biblioteca Sonora", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${allTracks.size} pistas · Multiformato sin límites",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showStatsDialog = true },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("open_stats_button")
                        ) {
                            Icon(Icons.Rounded.BarChart, contentDescription = "Estadísticas de escucha")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onShuffleAll,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("shuffle_all_button")
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Aleatorio")
                        }
                        FilledTonalButton(
                            onClick = onOpenEqualizer,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("hero_eq_button")
                        ) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ecualizador")
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Buscar canción, artista, formato") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Buscar") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1f).testTag("library_search_input")
                )
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("sort_order_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Ordenar")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        LibrarySortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        order.label,
                                        fontWeight = if (customization.sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                        color = if (customization.sortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onSortOrderChange(order)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        val next = if (customization.libraryLayout == LibraryLayoutMode.LIST) LibraryLayoutMode.GRID else LibraryLayoutMode.LIST
                        onLayoutModeChange(next)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("toggle_layout_button")
                ) {
                    Icon(
                        imageVector = if (customization.libraryLayout == LibraryLayoutMode.LIST) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = "Cambiar vista lista/cuadrícula"
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { openAudioLauncher.launch(arrayOf("audio/*", "*/*")) },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("import_audio_button")
                ) {
                    Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Importar", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { scanPermissionLauncher.launch(audioPermission) },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("scan_device_button")
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Escanear", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { showStreamDialog = true },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("add_stream_button")
                ) {
                    Icon(Icons.Rounded.AddLink, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("URL", maxLines = 1)
                }
            }
        }

        item {
            PrimaryTabRow(selectedTabIndex = browseTab.ordinal) {
                LibraryBrowseTab.entries.forEach { tab ->
                    Tab(
                        selected = browseTab == tab,
                        onClick = { browseTab = tab },
                        text = { Text(tab.label, maxLines = 1) }
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LibraryFilter.entries) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onSelectFilter(filter) },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                EmptyLibraryCard(onImportClick = { openAudioLauncher.launch(arrayOf("audio/*", "*/*")) })
            }
        } else if (browseTab != LibraryBrowseTab.TRACKS) {
            items(groupedItems, key = { "${browseTab.name}_${it.key}" }) { group ->
                val groupIcon = when (browseTab) {
                    LibraryBrowseTab.ALBUMS -> Icons.Rounded.Album
                    LibraryBrowseTab.ARTISTS -> Icons.Rounded.Person
                    LibraryBrowseTab.FOLDERS -> Icons.Rounded.Folder
                    LibraryBrowseTab.TRACKS -> Icons.Rounded.MusicNote
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().clickable { onPlayGroup(group.value) }
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(groupIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(group.key, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${group.value.size} pistas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onPlayGroup(group.value) }) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = "Reproducir colección", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        } else if (customization.libraryLayout == LibraryLayoutMode.GRID) {
            val rows = tracks.chunked(2)
            items(rows, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { track ->
                        TrackGridCard(
                            track = track,
                            isActive = track.id == currentTrackId,
                            showFormatBadge = customization.showFormatBadges,
                            onClick = { if (track.id == currentTrackId) onOpenNowPlaying() else onPlayTrack(track) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            items(tracks, key = { it.id }) { track ->
                TrackRowCard(
                    track = track,
                    isActive = track.id == currentTrackId,
                    isPlaying = track.id == currentTrackId && isPlaying,
                    compact = customization.compactDensity,
                    showFormatBadge = customization.showFormatBadges,
                    onClick = { if (track.id == currentTrackId) onOpenNowPlaying() else onPlayTrack(track) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { playlistTargetTrack = track },
                    onEditMetadata = { metadataTargetTrack = track },
                    onEditLyrics = { lyricsTargetTrack = track },
                    onShowTechInfo = { techInfoTrack = track },
                    onShare = { shareTrackIntent(context, track) },
                    onDelete = { onDeleteTrack(track) }
                )
            }
        }
    }

    if (showStreamDialog) {
        AddStreamDialog(
            onDismiss = { showStreamDialog = false },
            onConfirm = { url, title, artist ->
                onAddStreamUrl(url, title, artist)
                showStreamDialog = false
            }
        )
    }

    if (showStatsDialog) {
        ListeningStatsDialog(
            tracks = allTracks,
            onPlayTrack = onPlayTrack,
            onDismiss = { showStatsDialog = false }
        )
    }

    metadataTargetTrack?.let { target ->
        MetadataEditorDialog(
            track = target,
            onDismiss = { metadataTargetTrack = null },
            onSave = { title, artist, album, folder ->
                onUpdateMetadata(target.id, title, artist, album, folder)
                metadataTargetTrack = null
            }
        )
    }

    techInfoTrack?.let { target ->
        TrackTechnicalInfoDialog(track = target, onDismiss = { techInfoTrack = null })
    }

    lyricsTargetTrack?.let { target ->
        LyricsEditorDialog(
            initialLrc = target.lyricsLrc,
            currentPositionMs = 0L,
            onDismiss = { lyricsTargetTrack = null },
            onSave = { newLrc ->
                onUpdateLyrics(target.id, newLrc)
                lyricsTargetTrack = null
            },
            onImportLrcUri = { uri -> onImportLrcFile(target.id, uri) },
            onExportLrcUri = { uri -> onExportLrcFile(target, uri) }
        )
    }

    playlistTargetTrack?.let { target ->
        ModalBottomSheet(onDismissRequest = { playlistTargetTrack = null }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Añadir a lista de reproducción", style = MaterialTheme.typography.titleLarge)
                Text("${target.title} — ${target.artist}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (playlists.isEmpty()) {
                    Text("Aún no tienes listas creadas. Ve a la pestaña Listas para crear una.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(playlists, key = { it.playlist.id }) { pwt ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onAddTrackToPlaylist(pwt.playlist.id, target.id)
                                    playlistTargetTrack = null
                                },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(pwt.playlist.colorHex)))
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pwt.playlist.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${pwt.tracks.size} canciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun TrackGridCard(
    track: Track,
    isActive: Boolean,
    showFormatBadge: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AlbumArtworkView(
                artworkUri = track.artworkUri,
                contentDescription = track.title,
                cornerRadius = 12.dp,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
            Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (showFormatBadge) FormatBadgeChip(badge = track.formatBadge)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(formatDurationMs(track.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (track.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackRowCard(
    track: Track,
    isActive: Boolean,
    isPlaying: Boolean,
    compact: Boolean,
    showFormatBadge: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditMetadata: () -> Unit,
    onEditLyrics: () -> Unit,
    onShowTechInfo: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("track_card_${track.id}").clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (isActive) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                AlbumArtworkView(
                    artworkUri = track.artworkUri,
                    contentDescription = track.title,
                    cornerRadius = 10.dp,
                    modifier = Modifier.size(if (compact) 44.dp else 52.dp)
                )
                if (isActive) {
                    Box(
                        modifier = Modifier.size(if (compact) 44.dp else 52.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow, contentDescription = "Pista activa", tint = Color.White)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (showFormatBadge) FormatBadgeChip(badge = track.formatBadge)
                    if (track.lyricsLrc.isNotBlank()) {
                        Icon(Icons.Rounded.Lyrics, contentDescription = "Letra disponible", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                    }
                }
            }
            if (track.durationMs > 0L) {
                Text(formatDurationMs(track.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.minimumInteractiveComponentSize().testTag("favorite_button_${track.id}")) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Alternar favorito",
                    tint = if (track.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones de pista")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Reproducir siguiente") }, leadingIcon = { Icon(Icons.Rounded.QueuePlayNext, null) }, onClick = { menuExpanded = false; onPlayNext() })
                    DropdownMenuItem(text = { Text("Añadir a la cola") }, leadingIcon = { Icon(Icons.Rounded.AddToQueue, null) }, onClick = { menuExpanded = false; onAddToQueue() })
                    DropdownMenuItem(text = { Text("Añadir a lista") }, leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) }, onClick = { menuExpanded = false; onAddToPlaylist() })
                    DropdownMenuItem(text = { Text("Editar metadatos") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menuExpanded = false; onEditMetadata() })
                    DropdownMenuItem(text = { Text("Editar / exportar letra") }, leadingIcon = { Icon(Icons.Rounded.EditNote, null) }, onClick = { menuExpanded = false; onEditLyrics() })
                    DropdownMenuItem(text = { Text("Ficha técnica") }, leadingIcon = { Icon(Icons.Rounded.Info, null) }, onClick = { menuExpanded = false; onShowTechInfo() })
                    DropdownMenuItem(text = { Text("Compartir") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = { menuExpanded = false; onShare() })
                    DropdownMenuItem(text = { Text("Eliminar de biblioteca") }, leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) }, onClick = { menuExpanded = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(onImportClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Text("Sin resultados", style = MaterialTheme.typography.titleLarge)
            Text("Cambia el filtro de búsqueda o importa archivos de audio en cualquier formato.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onImportClick) { Text("Importar archivos de audio") }
        }
    }
}

@Composable
private fun AddStreamDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir URL / Stream de audio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL directa (MP3, FLAC, OGG, HLS...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artista o emisora") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onConfirm(url, title, artist) }, enabled = url.startsWith("http", ignoreCase = true)) { Text("Añadir") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
