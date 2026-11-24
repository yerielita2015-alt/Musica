package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.SmartPlaylistType
import com.example.data.prefs.NavLabelMode
import com.example.ui.SonoraViewModel
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PersonalizeScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.theme.SonoraTheme
import kotlinx.serialization.Serializable

@Serializable object LibraryRoute
@Serializable object PlaylistsRoute
@Serializable object EqualizerRoute
@Serializable object PersonalizeRoute
@Serializable object NowPlayingRoute
@Serializable object CloudSyncRoute
@Serializable data class PlaylistDetailRoute(val playlistId: Long)

private data class TopTab(
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SonoraViewModel = viewModel()
            val customization by viewModel.customization.collectAsStateWithLifecycle()

            SonoraTheme(
                themeMode = customization.themeMode,
                accentTheme = customization.accentTheme,
                fontChoice = customization.fontChoice,
                cornerStyle = customization.cornerStyle,
                fontScale = customization.fontScale
            ) {
                SonoraAppShell(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun SonoraAppShell(viewModel: SonoraViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val customization by viewModel.customization.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val filteredTracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val eqState by viewModel.eqState.collectAsStateWithLifecycle()
    val customEqPresets by viewModel.customEqPresets.collectAsStateWithLifecycle()
    val spectrumBands by viewModel.spectrumBands.collectAsStateWithLifecycle()
    val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val currentBookmarks by viewModel.currentTrackBookmarks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val syncHistory by viewModel.syncHistory.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    LaunchedEffect(playbackState.errorMessage) {
        val err = playbackState.errorMessage
        if (err != null) {
            snackbarHostState.showSnackbar(err)
            viewModel.clearPlaybackError()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val isNowPlaying = currentDest?.hasRoute<NowPlayingRoute>() == true

    val topTabs = listOf(
        TopTab("Biblioteca", LibraryRoute, Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "tab_library"),
        TopTab("Listas", PlaylistsRoute, Icons.Rounded.QueueMusic, Icons.Outlined.QueueMusic, "tab_playlists"),
        TopTab("Ecualizador", EqualizerRoute, Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq, "tab_equalizer"),
        TopTab("Estilo", PersonalizeRoute, Icons.Rounded.Palette, Icons.Outlined.Palette, "tab_personalize")
    )

    val smartCounts = remember(allTracks) {
        SmartPlaylistType.entries.associateWith { type -> viewModel.smartPlaylistTracks(type).size }
    }

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isNowPlaying && !useNavRail) {
                    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                        playbackState.currentTrack?.let { current ->
                            MiniPlayerBar(
                                track = current,
                                isPlaying = playbackState.isPlaying,
                                positionMs = playbackState.positionMs,
                                durationMs = playbackState.durationMs,
                                style = customization.miniPlayerStyle,
                                showArtwork = customization.showAlbumArtInMiniPlayer,
                                onOpenNowPlaying = { navController.navigate(NowPlayingRoute) },
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onNext = viewModel::nextTrack
                            )
                        }
                        NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                            topTabs.forEach { tab ->
                                val selected = when (tab.route) {
                                    is LibraryRoute -> currentDest?.hasRoute<LibraryRoute>() == true
                                    is PlaylistsRoute -> currentDest?.hasRoute<PlaylistsRoute>() == true || currentDest?.hasRoute<PlaylistDetailRoute>() == true
                                    is EqualizerRoute -> currentDest?.hasRoute<EqualizerRoute>() == true
                                    is PersonalizeRoute -> currentDest?.hasRoute<PersonalizeRoute>() == true || currentDest?.hasRoute<CloudSyncRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    alwaysShowLabel = customization.navLabelMode == NavLabelMode.ALWAYS,
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = if (customization.navLabelMode == NavLabelMode.ICONS_ONLY) null else {
                                        { Text(tab.label) }
                                    },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (useNavRail && !isNowPlaying) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        topTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                is LibraryRoute -> currentDest?.hasRoute<LibraryRoute>() == true
                                is PlaylistsRoute -> currentDest?.hasRoute<PlaylistsRoute>() == true || currentDest?.hasRoute<PlaylistDetailRoute>() == true
                                is EqualizerRoute -> currentDest?.hasRoute<EqualizerRoute>() == true
                                is PersonalizeRoute -> currentDest?.hasRoute<PersonalizeRoute>() == true || currentDest?.hasRoute<CloudSyncRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                alwaysShowLabel = customization.navLabelMode == NavLabelMode.ALWAYS,
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = if (customization.navLabelMode == NavLabelMode.ICONS_ONLY) null else {
                                    { Text(tab.label) }
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    val contentModifier = if (isNowPlaying) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxSize().widthIn(max = 720.dp).statusBarsPadding()
                    }

                    NavHost(
                        navController = navController,
                        startDestination = LibraryRoute,
                        modifier = contentModifier
                    ) {
                        composable<LibraryRoute> {
                            LibraryScreen(
                                tracks = filteredTracks,
                                allTracks = allTracks,
                                playlists = playlists,
                                currentTrackId = playbackState.currentTrack?.id,
                                isPlaying = playbackState.isPlaying,
                                searchQuery = searchQuery,
                                selectedFilter = selectedFilter,
                                customization = customization,
                                onSearchChange = viewModel::updateSearchQuery,
                                onSelectFilter = viewModel::selectFilter,
                                onSortOrderChange = viewModel::setSortOrder,
                                onLayoutModeChange = viewModel::setLibraryLayout,
                                onPlayTrack = { track -> viewModel.playTrackFromList(track, filteredTracks) },
                                onPlayGroup = { groupTracks ->
                                    if (groupTracks.isNotEmpty()) viewModel.playTrackFromList(groupTracks.first(), groupTracks)
                                },
                                onPlayNext = viewModel::playNext,
                                onAddToQueue = viewModel::addToQueue,
                                onShuffleAll = {
                                    val pool = filteredTracks.ifEmpty { allTracks }
                                    if (pool.isNotEmpty()) {
                                        val shuffled = pool.shuffled()
                                        viewModel.playTrackFromList(shuffled.first(), shuffled)
                                    }
                                },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onImportAudioUris = viewModel::importAudioFiles,
                                onScanDeviceMediaStore = viewModel::scanDeviceMediaStore,
                                onAddStreamUrl = viewModel::addStreamTrack,
                                onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                onUpdateMetadata = viewModel::updateTrackMetadata,
                                onUpdateLyrics = viewModel::updateTrackLyrics,
                                onImportLrcFile = viewModel::importLrcFileForTrack,
                                onExportLrcFile = viewModel::exportLrcFileForTrack,
                                onDeleteTrack = viewModel::deleteTrack,
                                onOpenNowPlaying = { navController.navigate(NowPlayingRoute) },
                                onOpenEqualizer = { navigateTopTab(EqualizerRoute) }
                            )
                        }

                        composable<PlaylistsRoute> {
                            PlaylistsScreen(
                                playlists = playlists,
                                smartPlaylistCounts = smartCounts,
                                onPlaySmartPlaylist = viewModel::playSmartPlaylist,
                                onCreatePlaylist = viewModel::createPlaylist,
                                onOpenPlaylist = { id -> navController.navigate(PlaylistDetailRoute(id)) },
                                onPlayPlaylist = { pwt -> viewModel.playPlaylist(pwt) },
                                onDeletePlaylist = viewModel::deletePlaylist
                            )
                        }

                        composable<PlaylistDetailRoute> { entry ->
                            val route = entry.toRoute<PlaylistDetailRoute>()
                            val found = playlists.firstOrNull { it.playlist.id == route.playlistId }
                            PlaylistDetailScreen(
                                playlistWithTracks = found,
                                allTracks = allTracks,
                                onBack = { navController.popBackStack() },
                                onPlayTrackAt = { idx -> found?.let { viewModel.playPlaylist(it, idx) } },
                                onUpdatePlaylist = viewModel::updatePlaylist,
                                onExportM3u = viewModel::exportPlaylistAsM3u,
                                onAddTrack = { trackId -> viewModel.addTrackToPlaylist(route.playlistId, trackId) },
                                onRemoveTrack = { trackId -> viewModel.removeTrackFromPlaylist(route.playlistId, trackId) }
                            )
                        }

                        composable<EqualizerRoute> {
                            EqualizerScreen(
                                eqState = eqState,
                                customPresets = customEqPresets,
                                playbackSpeed = playbackState.playbackSpeed,
                                playbackPitch = playbackState.playbackPitch,
                                skipSilence = customization.skipSilence,
                                monoAudio = customization.monoAudio,
                                softLimiter = customization.softLimiter,
                                onToggleEq = viewModel::setEqEnabled,
                                onBandLevelChange = viewModel::setEqBandLevel,
                                onApplyPreset = viewModel::applyEqPreset,
                                onSaveCustomPreset = viewModel::saveCurrentAsCustomPreset,
                                onDeleteCustomPreset = viewModel::deleteCustomEqPreset,
                                onBassChange = viewModel::setBassStrength,
                                onVirtualizerChange = viewModel::setVirtualizerStrength,
                                onLoudnessChange = viewModel::setLoudnessGain,
                                onPreampChange = viewModel::setPreampDb,
                                onBalanceChange = viewModel::setStereoBalance,
                                onSpeedPitchChange = viewModel::setSpeedAndPitch,
                                onSkipSilenceChange = viewModel::setSkipSilence,
                                onMonoAudioChange = viewModel::setMonoAudio,
                                onSoftLimiterChange = viewModel::setSoftLimiter
                            )
                        }

                        composable<PersonalizeRoute> {
                            PersonalizeScreen(
                                customization = customization,
                                onThemeModeChange = viewModel::setThemeMode,
                                onAccentThemeChange = viewModel::setAccentTheme,
                                onFontChoiceChange = viewModel::setFontChoice,
                                onNowPlayingStyleChange = viewModel::setNowPlayingStyle,
                                onVisualizerStyleChange = viewModel::setVisualizerStyle,
                                onCornerStyleChange = viewModel::setCornerStyle,
                                onArtworkShapeChange = viewModel::setArtworkShape,
                                onNavLabelModeChange = viewModel::setNavLabelMode,
                                onMiniPlayerStyleChange = viewModel::setMiniPlayerStyle,
                                onLibraryLayoutChange = viewModel::setLibraryLayout,
                                onSortOrderChange = viewModel::setSortOrder,
                                onLyricsSizeChange = viewModel::setLyricsSize,
                                onLyricsAlignChange = viewModel::setLyricsAlign,
                                onFontScaleChange = viewModel::setFontScale,
                                onCompactDensityChange = viewModel::setCompactDensity,
                                onShowFormatBadgesChange = viewModel::setShowFormatBadges,
                                onUseTonalBackdropChange = viewModel::setUseTonalContainerBackdrop,
                                onShowQuickSeekChange = viewModel::setShowQuickSeekButtons,
                                onSeekStepChange = viewModel::setSeekStepSeconds,
                                onKeepScreenOnLyricsChange = viewModel::setKeepScreenOnLyrics,
                                onShowMiniPlayerArtChange = viewModel::setShowAlbumArtInMiniPlayer,
                                onCrossfadeChange = viewModel::setCrossfadeSeconds,
                                onOpenCloudSync = { navController.navigate(CloudSyncRoute) }
                            )
                        }

                        composable<CloudSyncRoute> {
                            CloudSyncScreen(
                                lastSyncMs = customization.lastSyncTimestampMs,
                                trackCount = allTracks.size,
                                playlistCount = playlists.size,
                                customEqCount = customEqPresets.size,
                                syncHistory = syncHistory,
                                onBack = { navController.popBackStack() },
                                onExportBackup = viewModel::exportCloudBackup,
                                onImportBackup = viewModel::importCloudBackup
                            )
                        }

                        composable<NowPlayingRoute> {
                            NowPlayingScreen(
                                playbackState = playbackState,
                                lyrics = currentLyrics,
                                bookmarks = currentBookmarks,
                                spectrumBands = spectrumBands,
                                customization = customization,
                                onBack = { navController.popBackStack() },
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSeekTo = viewModel::seekTo,
                                onSeekBySeconds = viewModel::seekBySeconds,
                                onNext = viewModel::nextTrack,
                                onPrevious = viewModel::previousTrack,
                                onToggleShuffle = viewModel::toggleShuffle,
                                onCycleRepeat = viewModel::cycleRepeatMode,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onUpdateMetadata = viewModel::updateTrackMetadata,
                                onUpdateLyrics = viewModel::updateTrackLyrics,
                                onShiftLyricsOffset = viewModel::shiftTrackLyricsOffset,
                                onImportLrcFile = viewModel::importLrcFileForTrack,
                                onExportLrcFile = viewModel::exportLrcFileForTrack,
                                onAddBookmark = viewModel::addTrackBookmark,
                                onDeleteBookmark = viewModel::deleteTrackBookmark,
                                onSetAbLoopA = viewModel::setAbLoopPointA,
                                onSetAbLoopB = viewModel::setAbLoopPointB,
                                onClearAbLoop = viewModel::clearAbLoop,
                                onStartSleepTimer = viewModel::startSleepTimer,
                                onSleepAfterTrack = viewModel::setSleepAfterCurrentTrack,
                                onRemoveFromQueue = viewModel::removeFromQueue,
                                onMoveQueueItem = viewModel::moveQueueItem,
                                onShuffleQueue = viewModel::shuffleUpcomingQueue,
                                onClearQueue = viewModel::clearQueueExceptCurrent,
                                onJumpToQueueIndex = { idx ->
                                    playbackState.queue.getOrNull(idx)?.let { t ->
                                        viewModel.playTrackFromList(t, playbackState.queue)
                                    }
                                },
                                onOpenEqualizer = {
                                    navController.popBackStack()
                                    navigateTopTab(EqualizerRoute)
                                }
                            )
                        }
                    }

                    if (useNavRail && !isNowPlaying) {
                        playbackState.currentTrack?.let { current ->
                            MiniPlayerBar(
                                track = current,
                                isPlaying = playbackState.isPlaying,
                                positionMs = playbackState.positionMs,
                                durationMs = playbackState.durationMs,
                                style = customization.miniPlayerStyle,
                                showArtwork = customization.showAlbumArtInMiniPlayer,
                                onOpenNowPlaying = { navController.navigate(NowPlayingRoute) },
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onNext = viewModel::nextTrack,
                                modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = 560.dp).navigationBarsPadding()
                            )
                        }
                    }
                }
            }
        }
    }
}
