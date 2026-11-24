package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioScanner
import com.example.data.audio.DemoTrackGenerator
import com.example.data.backup.CloudBackupManager
import com.example.data.local.SonoraDatabase
import com.example.data.local.SonoraRepository
import com.example.data.lyrics.LrcParser
import com.example.data.model.EqPreset
import com.example.data.model.LibraryFilter
import com.example.data.model.LibrarySortOrder
import com.example.data.model.LyricLine
import com.example.data.model.Playlist
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SmartPlaylistType
import com.example.data.model.SyncLogEntry
import com.example.data.model.Track
import com.example.data.model.TrackBookmark
import com.example.data.prefs.AccentTheme
import com.example.data.prefs.AppFontChoice
import com.example.data.prefs.ArtworkShapeStyle
import com.example.data.prefs.CornerStyle
import com.example.data.prefs.LibraryLayoutMode
import com.example.data.prefs.LyricsAlignOption
import com.example.data.prefs.LyricsSizeOption
import com.example.data.prefs.MiniPlayerStyle
import com.example.data.prefs.NavLabelMode
import com.example.data.prefs.NowPlayingStyle
import com.example.data.prefs.ThemeMode
import com.example.data.prefs.UserCustomization
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.prefs.VisualizerStyle
import com.example.player.AudioFxController
import com.example.player.AudioSpectrumBus
import com.example.player.EqUiState
import com.example.player.PlaybackConnectionManager
import com.example.player.PlaybackUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SonoraViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = SonoraRepository(SonoraDatabase.getInstance(appContext).sonoraDao())
    private val prefsRepo = UserPreferencesRepository(appContext)

    private val playbackManager = PlaybackConnectionManager(
        context = appContext,
        scope = viewModelScope,
        onTrackStarted = { track ->
            viewModelScope.launch { repository.recordPlay(track.id) }
        },
        onDurationDiscovered = { trackId, durationMs ->
            viewModelScope.launch { repository.updateDurationIfMissing(trackId, durationMs) }
        }
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    val selectedFilter: StateFlow<LibraryFilter> = _selectedFilter.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val customization: StateFlow<UserCustomization> = prefsRepo.customizationFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserCustomization())

    val allTracks: StateFlow<List<Track>> = repository.tracksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val filteredTracks: StateFlow<List<Track>> = combine(
        allTracks,
        _searchQuery,
        _selectedFilter,
        customization.map { it.sortOrder }.distinctUntilChanged()
    ) { tracks, query, filter, sortOrder ->
        val q = query.trim().lowercase()
        val filtered = tracks.filter { track ->
            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.FAVORITES -> track.isFavorite
                LibraryFilter.LOSSLESS -> track.formatBadge in setOf("FLAC", "WAV", "ALAC", "AIFF", "DSF")
                LibraryFilter.STREAMS -> track.formatBadge in setOf("STREAM", "HLS", "DASH") || track.uri.startsWith("http")
                LibraryFilter.RECENT -> track.lastPlayedAt > 0L
            }
            val matchesQuery = q.isEmpty() ||
                track.title.lowercase().contains(q) ||
                track.artist.lowercase().contains(q) ||
                track.album.lowercase().contains(q) ||
                track.folderName.lowercase().contains(q) ||
                track.formatBadge.lowercase().contains(q)
            matchesFilter && matchesQuery
        }

        if (filter == LibraryFilter.RECENT) {
            filtered.sortedByDescending { it.lastPlayedAt }
        } else {
            when (sortOrder) {
                LibrarySortOrder.RECENT_ADDED -> filtered.sortedByDescending { it.addedAt }
                LibrarySortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
                LibrarySortOrder.ARTIST_ASC -> filtered.sortedBy { it.artist.lowercase() }
                LibrarySortOrder.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
                LibrarySortOrder.MOST_PLAYED -> filtered.sortedByDescending { it.playCount }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val playlists: StateFlow<List<PlaylistWithTracks>> = repository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customEqPresets: StateFlow<List<EqPreset>> = repository.customEqPresetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val syncHistory: StateFlow<List<SyncLogEntry>> = repository.syncHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val playbackState: StateFlow<PlaybackUiState> = playbackManager.state
    val eqState: StateFlow<EqUiState> = AudioFxController.state
    val spectrumBands: StateFlow<List<Float>> = AudioSpectrumBus.bands

    val currentLyrics: StateFlow<List<LyricLine>> = playbackState
        .combine(allTracks) { pb, tracks ->
            val track = pb.currentTrack?.let { cur -> tracks.firstOrNull { it.id == cur.id } ?: cur }
            if (track == null || track.lyricsLrc.isBlank()) emptyList()
            else LrcParser.parse(track.lyricsLrc, pb.durationMs.takeIf { it > 0 } ?: track.durationMs)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val currentTrackBookmarks: StateFlow<List<TrackBookmark>> = playbackState
        .map { it.currentTrack?.id }
        .distinctUntilChanged()
        .flatMapLatest { trackId ->
            if (trackId == null) flowOf(emptyList()) else repository.observeBookmarksForTrack(trackId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        playbackManager.connect()
        viewModelScope.launch {
            val prefs = prefsRepo.customizationFlow.first()
            AudioFxController.restoreFromPreferences(
                enabled = prefs.eqEnabled,
                presetName = prefs.activeEqPresetName,
                bandLevelsCsv = prefs.eqBandLevelsCsv,
                bassStrength = prefs.bassStrength,
                virtualizerStrength = prefs.virtualizerStrength,
                loudnessGainMb = prefs.loudnessGainMb,
                preampDb = prefs.preampDb,
                stereoBalance = prefs.stereoBalance
            )
            AudioSpectrumBus.monoAudio = prefs.monoAudio
            AudioSpectrumBus.softLimiter = prefs.softLimiter
            AudioSpectrumBus.setSkipSilence(prefs.skipSilence)
            playbackManager.setSpeedAndPitch(prefs.playbackSpeed, prefs.playbackPitch)
            playbackManager.setCrossfadeSeconds(prefs.crossfadeSeconds)

            if (!prefs.demoSeeded) {
                val demoTracks = DemoTrackGenerator.generateStudioTracksIfNeeded(appContext)
                repository.upsertTracks(demoTracks)
                val playlistId = repository.createPlaylist(
                    name = "Sesiones de Estudio",
                    description = "Mezcla inicial de estudio en alta fidelidad con letras sincronizadas",
                    colorHex = 0xFF6750A4,
                    iconKey = "headphones"
                )
                demoTracks.forEach { repository.addTrackToPlaylist(playlistId, it.id) }
                prefsRepo.updateCustomization { it.copy(demoSeeded = true) }
            }
        }

        viewModelScope.launch {
            allTracks.collect { tracks ->
                playbackManager.updateLibrarySnapshot(tracks)
            }
        }
    }

    override fun onCleared() {
        playbackManager.release()
        super.onCleared()
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun selectFilter(filter: LibraryFilter) { _selectedFilter.value = filter }

    fun playTrackFromList(track: Track, sourceList: List<Track> = filteredTracks.value) {
        val list = sourceList.ifEmpty { listOf(track) }
        val idx = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playbackManager.playQueue(list, idx)
    }

    fun playNext(track: Track) {
        playbackManager.playNext(track)
        emitToast("Se reproducirá a continuación: ${track.title}")
    }

    fun addToQueue(track: Track) {
        playbackManager.addToQueue(track)
        emitToast("Añadida a la cola: ${track.title}")
    }

    fun moveQueueItem(from: Int, to: Int) = playbackManager.moveQueueItem(from, to)
    fun shuffleUpcomingQueue() {
        playbackManager.shuffleUpcomingQueue()
        emitToast("Cola mezclada")
    }
    fun clearQueueExceptCurrent() {
        playbackManager.clearQueueExceptCurrent()
        emitToast("Cola limpiada")
    }

    fun playPlaylist(playlistWithTracks: PlaylistWithTracks, startIndex: Int = 0) {
        if (playlistWithTracks.tracks.isEmpty()) {
            emitToast("Esta lista está vacía. Añade canciones primero.")
            return
        }
        playbackManager.playQueue(playlistWithTracks.tracks, startIndex.coerceIn(0, playlistWithTracks.tracks.lastIndex))
    }

    fun smartPlaylistTracks(type: SmartPlaylistType): List<Track> {
        val tracks = allTracks.value
        return when (type) {
            SmartPlaylistType.MOST_PLAYED -> tracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
            SmartPlaylistType.RECENTLY_ADDED -> tracks.sortedByDescending { it.addedAt }.take(25)
            SmartPlaylistType.FAVORITES -> tracks.filter { it.isFavorite }
            SmartPlaylistType.WITH_LYRICS -> tracks.filter { it.lyricsLrc.isNotBlank() }
        }
    }

    fun playSmartPlaylist(type: SmartPlaylistType) {
        val list = smartPlaylistTracks(type)
        if (list.isEmpty()) {
            emitToast("No hay canciones en \"${type.title}\" todavía.")
            return
        }
        playbackManager.playQueue(list, 0)
        emitToast("Reproduciendo ${type.title}")
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun seekTo(ms: Long) = playbackManager.seekTo(ms)
    fun seekBySeconds(seconds: Int) = playbackManager.seekBy(seconds * 1000L)
    fun nextTrack() = playbackManager.next()
    fun previousTrack() = playbackManager.previous()
    fun toggleShuffle() = playbackManager.toggleShuffle()
    fun cycleRepeatMode() = playbackManager.cycleRepeatMode()
    fun removeFromQueue(index: Int) = playbackManager.removeFromQueue(index)
    fun setAbLoopPointA() {
        playbackManager.setAbLoopPointA()
        emitToast("Punto A del bucle fijado")
    }
    fun setAbLoopPointB() {
        playbackManager.setAbLoopPointB()
        emitToast("Bucle A-B activado")
    }
    fun clearAbLoop() {
        playbackManager.clearAbLoop()
        emitToast("Bucle A-B desactivado")
    }

    fun startSleepTimer(minutes: Int) {
        playbackManager.startSleepTimer(minutes)
        emitToast(if (minutes > 0) "Temporizador activado: $minutes min (con atenuación)" else "Temporizador desactivado")
    }

    fun setSleepAfterCurrentTrack(enabled: Boolean) {
        playbackManager.setSleepAfterCurrentTrack(enabled)
        if (enabled) emitToast("Se pausará al terminar esta canción")
    }

    fun clearPlaybackError() = playbackManager.clearError()

    fun setSpeedAndPitch(speed: Float, pitch: Float) {
        playbackManager.setSpeedAndPitch(speed, pitch)
        updatePref { it.copy(playbackSpeed = speed, playbackPitch = pitch) }
    }

    fun importAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val imported = AudioScanner.importDocumentUris(appContext, uris)
            if (imported.isNotEmpty()) {
                repository.upsertTracks(imported)
                emitToast("Se importaron ${imported.size} pistas de audio")
            } else {
                emitToast("No se pudieron leer las pistas seleccionadas")
            }
        }
    }

    fun scanDeviceMediaStore() {
        viewModelScope.launch {
            val found = AudioScanner.scanDeviceMediaStore(appContext)
            if (found.isNotEmpty()) {
                repository.upsertTracks(found)
                emitToast("Biblioteca actualizada: ${found.size} archivos encontrados")
            } else {
                emitToast("No se encontraron archivos de audio en el dispositivo")
            }
        }
    }

    fun addStreamTrack(url: String, title: String, artist: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            val track = AudioScanner.buildStreamTrack(url, title, artist)
            repository.upsertTrack(track)
            emitToast("Stream añadido: ${track.title}")
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch { repository.toggleFavorite(track.id, !track.isFavorite) }
    }

    fun updateTrackMetadata(trackId: String, title: String, artist: String, album: String, folderName: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.updateTrackMetadata(trackId, title, artist, album, folderName)
            emitToast("Metadatos actualizados")
        }
    }

    fun addTrackBookmark(trackId: String, timeMs: Long, label: String) {
        viewModelScope.launch {
            repository.addBookmark(trackId, timeMs, label)
            emitToast("Marcador guardado")
        }
    }

    fun deleteTrackBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
            emitToast("Marcador eliminado")
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            repository.deleteTrack(track.id)
            emitToast("Eliminada: ${track.title}")
        }
    }

    fun updateTrackLyrics(trackId: String, lrcText: String) {
        viewModelScope.launch {
            repository.updateLyrics(trackId, lrcText)
            emitToast("Letra guardada y sincronizada")
        }
    }

    fun shiftTrackLyricsOffset(track: Track, deltaMs: Long) {
        if (track.lyricsLrc.isBlank()) return
        viewModelScope.launch {
            val shifted = LrcParser.shiftTimestamps(track.lyricsLrc, deltaMs, track.durationMs)
            repository.updateLyrics(track.id, shifted)
            val sign = if (deltaMs >= 0) "+" else ""
            emitToast("Sincronización ajustada ${sign}${deltaMs} ms")
        }
    }

    fun importLrcFileForTrack(trackId: String, uri: Uri) {
        viewModelScope.launch {
            try {
                val text = appContext.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.readText()
                    .orEmpty()
                if (text.isNotBlank()) {
                    repository.updateLyrics(trackId, text)
                    emitToast("Archivo .LRC importado correctamente")
                } else {
                    emitToast("El archivo de letra está vacío")
                }
            } catch (_: Exception) {
                emitToast("No se pudo leer el archivo .LRC")
            }
        }
    }

    fun exportLrcFileForTrack(track: Track, uri: Uri) {
        viewModelScope.launch {
            val content = track.lyricsLrc.ifBlank { return@launch }
            CloudBackupManager.exportLrcText(appContext, uri, content)
                .onSuccess { emitToast("Letra exportada como .LRC") }
                .onFailure { emitToast("No se pudo exportar el archivo .LRC") }
        }
    }

    fun createPlaylist(name: String, description: String, colorHex: Long, artworkUri: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name, description, colorHex, artworkUri)
            emitToast("Lista \"$name\" creada")
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.updatePlaylist(playlist)
            emitToast("Lista actualizada")
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            emitToast("Lista eliminada")
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            emitToast("Canción añadida a la lista")
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            emitToast("Canción quitada de la lista")
        }
    }

    fun exportPlaylistAsM3u(playlistWithTracks: PlaylistWithTracks, uri: Uri) {
        viewModelScope.launch {
            CloudBackupManager.exportM3uPlaylist(appContext, uri, playlistWithTracks)
                .onSuccess { emitToast("Lista exportada en formato .M3U8") }
                .onFailure { emitToast("No se pudo exportar la lista") }
        }
    }

    // Equalizer & Audio FX
    fun setEqEnabled(enabled: Boolean) { AudioFxController.setEnabled(enabled); persistEqState() }
    fun setEqBandLevel(bandIndex: Int, levelMb: Int) { AudioFxController.setBandLevel(bandIndex, levelMb); persistEqState() }
    fun setBassStrength(strength: Int) { AudioFxController.setBassStrength(strength); persistEqState() }
    fun setVirtualizerStrength(strength: Int) { AudioFxController.setVirtualizerStrength(strength); persistEqState() }
    fun setLoudnessGain(gainMb: Int) { AudioFxController.setLoudnessGain(gainMb); persistEqState() }
    fun setPreampDb(preampDb: Float) { AudioFxController.setPreampDb(preampDb); persistEqState() }
    fun setStereoBalance(balance: Float) { AudioFxController.setStereoBalance(balance); persistEqState() }
    fun applyEqPreset(preset: EqPreset) { AudioFxController.applyPreset(preset); persistEqState() }

    fun setSkipSilence(enabled: Boolean) {
        AudioSpectrumBus.setSkipSilence(enabled)
        updatePref { it.copy(skipSilence = enabled) }
    }

    fun setMonoAudio(enabled: Boolean) {
        AudioSpectrumBus.monoAudio = enabled
        updatePref { it.copy(monoAudio = enabled) }
    }

    fun setSoftLimiter(enabled: Boolean) {
        AudioSpectrumBus.softLimiter = enabled
        updatePref { it.copy(softLimiter = enabled) }
    }

    fun saveCurrentAsCustomPreset(name: String) {
        if (name.isBlank()) return
        val eq = AudioFxController.state.value
        viewModelScope.launch {
            val preset = EqPreset(
                name = name.trim(),
                bandLevelsMb = eq.bandLevelsMb,
                bassStrength = eq.bassStrength,
                virtualizerStrength = eq.virtualizerStrength,
                loudnessGainMb = eq.loudnessGainMb,
                preampDb = eq.preampDb,
                stereoBalance = eq.stereoBalance,
                isCustom = true
            )
            repository.saveCustomEqPreset(preset)
            AudioFxController.applyPreset(preset)
            persistEqState()
            emitToast("Perfil de sonido \"${preset.name}\" guardado")
        }
    }

    fun deleteCustomEqPreset(name: String) {
        viewModelScope.launch {
            repository.deleteCustomEqPreset(name)
            emitToast("Perfil eliminado")
        }
    }

    private fun persistEqState() {
        val eq = AudioFxController.state.value
        updatePref {
            it.copy(
                eqEnabled = eq.enabled,
                activeEqPresetName = eq.activePresetName,
                eqBandLevelsCsv = eq.bandLevelsMb.joinToString(","),
                bassStrength = eq.bassStrength,
                virtualizerStrength = eq.virtualizerStrength,
                loudnessGainMb = eq.loudnessGainMb,
                preampDb = eq.preampDb,
                stereoBalance = eq.stereoBalance
            )
        }
    }

    // Personalization Setters
    fun setThemeMode(mode: ThemeMode) = updatePref { it.copy(themeMode = mode) }
    fun setAccentTheme(accent: AccentTheme) = updatePref { it.copy(accentTheme = accent) }
    fun setFontChoice(font: AppFontChoice) = updatePref { it.copy(fontChoice = font) }
    fun setNowPlayingStyle(style: NowPlayingStyle) = updatePref { it.copy(nowPlayingStyle = style) }
    fun setVisualizerStyle(style: VisualizerStyle) = updatePref { it.copy(visualizerStyle = style) }
    fun setCornerStyle(style: CornerStyle) = updatePref { it.copy(cornerStyle = style) }
    fun setArtworkShape(shape: ArtworkShapeStyle) = updatePref { it.copy(artworkShape = shape) }
    fun setNavLabelMode(mode: NavLabelMode) = updatePref { it.copy(navLabelMode = mode) }
    fun setLyricsSize(size: LyricsSizeOption) = updatePref { it.copy(lyricsSize = size) }
    fun setLyricsAlign(align: LyricsAlignOption) = updatePref { it.copy(lyricsAlign = align) }
    fun setMiniPlayerStyle(style: MiniPlayerStyle) = updatePref { it.copy(miniPlayerStyle = style) }
    fun setLibraryLayout(layout: LibraryLayoutMode) = updatePref { it.copy(libraryLayout = layout) }
    fun setSortOrder(order: LibrarySortOrder) = updatePref { it.copy(sortOrder = order) }
    fun setFontScale(scale: Float) = updatePref { it.copy(fontScale = scale.coerceIn(0.85f, 1.25f)) }
    fun setCompactDensity(compact: Boolean) = updatePref { it.copy(compactDensity = compact) }
    fun setShowFormatBadges(show: Boolean) = updatePref { it.copy(showFormatBadges = show) }
    fun setUseTonalContainerBackdrop(enabled: Boolean) = updatePref { it.copy(useTonalContainerBackdrop = enabled) }
    fun setShowQuickSeekButtons(show: Boolean) = updatePref { it.copy(showQuickSeekButtons = show) }
    fun setSeekStepSeconds(sec: Int) = updatePref { it.copy(seekStepSeconds = sec.coerceIn(5, 30)) }
    fun setKeepScreenOnLyrics(keepOn: Boolean) = updatePref { it.copy(keepScreenOnLyrics = keepOn) }
    fun setShowAlbumArtInMiniPlayer(show: Boolean) = updatePref { it.copy(showAlbumArtInMiniPlayer = show) }
    fun setCrossfadeSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 12)
        playbackManager.setCrossfadeSeconds(clamped)
        updatePref { it.copy(crossfadeSeconds = clamped) }
    }

    private fun updatePref(block: (UserCustomization) -> UserCustomization) {
        viewModelScope.launch { prefsRepo.updateCustomization(block) }
    }

    // Cloud Backup / Export & Restore
    fun exportCloudBackup(uri: Uri) {
        viewModelScope.launch {
            val tracks = repository.getAllTracksOnce()
            val lists = repository.getAllPlaylistsOnce()
            val presets = repository.getCustomEqPresetsOnce()
            val bookmarks = repository.getAllBookmarksOnce()
            val custom = prefsRepo.customizationFlow.first()
            CloudBackupManager.exportToUri(
                context = appContext,
                uri = uri,
                tracks = tracks,
                playlists = lists,
                eqPresets = presets,
                bookmarks = bookmarks,
                customization = custom
            ).onSuccess { count ->
                val now = System.currentTimeMillis()
                prefsRepo.updateCustomization { it.copy(lastSyncTimestampMs = now) }
                repository.addSyncLog(
                    "Exportación a Nube / SAF",
                    "${tracks.size} pistas, $count listas, ${presets.size} perfiles EQ"
                )
                emitToast("Respaldo exportado con éxito")
            }.onFailure { err ->
                emitToast("Error al exportar: ${err.localizedMessage ?: "desconocido"}")
            }
        }
    }

    fun importCloudBackup(uri: Uri) {
        viewModelScope.launch {
            CloudBackupManager.importFromUri(appContext, uri).onSuccess { backup ->
                if (backup.tracks.isNotEmpty()) repository.upsertTracks(backup.tracks)
                for (bp in backup.playlists) {
                    val newId = repository.createPlaylist(
                        name = bp.playlist.name,
                        description = bp.playlist.description,
                        colorHex = bp.playlist.colorHex,
                        artworkUri = bp.playlist.artworkUri,
                        iconKey = bp.playlist.iconKey
                    )
                    bp.trackIds.forEach { tid -> repository.addTrackToPlaylist(newId, tid) }
                }
                backup.eqPresets.forEach { repository.saveCustomEqPreset(it) }
                backup.bookmarks.forEach { bm -> repository.addBookmark(bm.trackId, bm.timeMs, bm.label) }
                prefsRepo.updateCustomization {
                    backup.customization.copy(
                        demoSeeded = true,
                        lastSyncTimestampMs = System.currentTimeMillis()
                    )
                }
                AudioFxController.restoreFromPreferences(
                    enabled = backup.customization.eqEnabled,
                    presetName = backup.customization.activeEqPresetName,
                    bandLevelsCsv = backup.customization.eqBandLevelsCsv,
                    bassStrength = backup.customization.bassStrength,
                    virtualizerStrength = backup.customization.virtualizerStrength,
                    loudnessGainMb = backup.customization.loudnessGainMb,
                    preampDb = backup.customization.preampDb,
                    stereoBalance = backup.customization.stereoBalance
                )
                AudioSpectrumBus.monoAudio = backup.customization.monoAudio
                AudioSpectrumBus.softLimiter = backup.customization.softLimiter
                AudioSpectrumBus.setSkipSilence(backup.customization.skipSilence)
                repository.addSyncLog(
                    "Restauración desde Nube / SAF",
                    "${backup.tracks.size} pistas y ${backup.playlists.size} listas restauradas"
                )
                emitToast("Respaldo restaurado correctamente")
            }.onFailure { err ->
                emitToast("Error al importar respaldo: ${err.localizedMessage ?: "formato inválido"}")
            }
        }
    }

    private fun emitToast(msg: String) {
        _snackbarEvents.tryEmit(msg)
    }
}
