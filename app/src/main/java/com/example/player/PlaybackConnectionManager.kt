package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val crossfadeSeconds: Int = 3,
    val sleepTimerRemainingSec: Int? = null,
    val sleepAfterCurrentTrack: Boolean = false,
    val abLoopStartMs: Long? = null,
    val abLoopEndMs: Long? = null,
    val errorMessage: String? = null
)

@OptIn(UnstableApi::class)
class PlaybackConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onTrackStarted: (Track) -> Unit,
    private val onDurationDiscovered: (String, Long) -> Unit = { _, _ -> }
) {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var trackMap: Map<String, Track> = emptyMap()
    private var lastReportedTrackId: String? = null
    private var pendingPlayRequest: Pair<List<Track>, Int>? = null

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, SonoraPlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val ctrl = future.get()
                controller = ctrl
                attachPlayerListener(ctrl)
                syncFromPlayer(ctrl)
                pendingPlayRequest?.let { (tracks, startIdx) ->
                    pendingPlayRequest = null
                    playQueue(tracks, startIdx)
                }
            } catch (_: Exception) {
            }
        }, MoreExecutors.directExecutor())
    }

    fun release() {
        positionJob?.cancel()
        sleepTimerJob?.cancel()
        AudioSpectrumBus.sleepFadeGain = 1f
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    fun updateLibrarySnapshot(tracks: List<Track>) {
        trackMap = tracks.associateBy { it.id }
        val current = _state.value
        val updatedQueue = current.queue.map { trackMap[it.id] ?: it }
        val updatedCurrent = current.currentTrack?.let { trackMap[it.id] ?: it }
        _state.value = current.copy(
            queue = updatedQueue,
            currentTrack = updatedCurrent
        )
    }

    fun playQueue(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty() || startIndex !in tracks.indices) return
        trackMap = trackMap + tracks.associateBy { it.id }
        val ctrl = controller
        if (ctrl == null) {
            pendingPlayRequest = tracks to startIndex
            connect()
            return
        }

        val mediaItems = tracks.map { it.toMediaItem() }
        ctrl.setMediaItems(mediaItems, startIndex, 0L)
        ctrl.playbackParameters = PlaybackParameters(_state.value.playbackSpeed, _state.value.playbackPitch)
        ctrl.prepare()
        ctrl.play()

        val selected = tracks[startIndex]
        _state.value = _state.value.copy(
            queue = tracks,
            currentIndex = startIndex,
            currentTrack = selected,
            positionMs = 0L,
            durationMs = selected.durationMs,
            abLoopStartMs = null,
            abLoopEndMs = null,
            errorMessage = null
        )
        notifyTrackStartedIfNeeded(selected)
    }

    fun playNext(track: Track) {
        trackMap = trackMap + (track.id to track)
        val ctrl = controller
        val currentQueue = _state.value.queue
        if (ctrl == null || currentQueue.isEmpty() || ctrl.mediaItemCount == 0) {
            playQueue(listOf(track), 0)
            return
        }
        val insertIdx = (ctrl.currentMediaItemIndex + 1).coerceIn(0, ctrl.mediaItemCount)
        ctrl.addMediaItem(insertIdx, track.toMediaItem())
        val updated = currentQueue.toMutableList().apply {
            add(insertIdx.coerceIn(0, size), track)
        }
        _state.value = _state.value.copy(queue = updated)
    }

    fun addToQueue(track: Track) {
        trackMap = trackMap + (track.id to track)
        val ctrl = controller
        val currentQueue = _state.value.queue
        if (ctrl == null || currentQueue.isEmpty() || ctrl.mediaItemCount == 0) {
            playQueue(listOf(track), 0)
            return
        }
        ctrl.addMediaItem(track.toMediaItem())
        _state.value = _state.value.copy(queue = currentQueue + track)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val ctrl = controller ?: return
        val currentQueue = _state.value.queue.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) return
        ctrl.moveMediaItem(fromIndex, toIndex)
        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)
        _state.value = _state.value.copy(
            queue = currentQueue,
            currentIndex = ctrl.currentMediaItemIndex
        )
    }

    fun clearQueueExceptCurrent() {
        val ctrl = controller ?: return
        val currentTrack = _state.value.currentTrack ?: return
        val idx = ctrl.currentMediaItemIndex
        if (idx !in 0 until ctrl.mediaItemCount) return
        for (i in (ctrl.mediaItemCount - 1) downTo 0) {
            if (i != idx) ctrl.removeMediaItem(i)
        }
        _state.value = _state.value.copy(
            queue = listOf(currentTrack),
            currentIndex = 0
        )
    }

    fun shuffleUpcomingQueue() {
        val ctrl = controller ?: return
        val currentQueue = _state.value.queue
        val currentIdx = ctrl.currentMediaItemIndex
        if (currentQueue.size <= 2 || currentIdx !in currentQueue.indices) return
        val currentTrack = currentQueue[currentIdx]
        val others = currentQueue.filterIndexed { i, _ -> i != currentIdx }.shuffled()
        val reordered = listOf(currentTrack) + others
        for (i in (ctrl.mediaItemCount - 1) downTo 0) {
            if (i != currentIdx) ctrl.removeMediaItem(i)
        }
        others.forEach { ctrl.addMediaItem(it.toMediaItem()) }
        _state.value = _state.value.copy(
            queue = reordered,
            currentIndex = 0
        )
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
        } else {
            if (ctrl.playbackState == Player.STATE_ENDED && ctrl.mediaItemCount > 0) {
                ctrl.seekTo(0, 0L)
            }
            ctrl.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val ctrl = controller ?: return
        ctrl.seekTo(positionMs.coerceAtLeast(0L))
        _state.value = _state.value.copy(positionMs = positionMs.coerceAtLeast(0L))
    }

    fun seekBy(deltaMs: Long) {
        val ctrl = controller ?: return
        val dur = ctrl.duration.takeIf { it > 0L } ?: _state.value.durationMs
        val target = (ctrl.currentPosition + deltaMs).coerceIn(0L, dur.coerceAtLeast(0L))
        seekTo(target)
    }

    fun next() {
        val ctrl = controller ?: return
        if (ctrl.hasNextMediaItem()) {
            ctrl.seekToNextMediaItem()
        } else if (ctrl.mediaItemCount > 0) {
            ctrl.seekTo(0, 0L)
        }
    }

    fun previous() {
        val ctrl = controller ?: return
        if (ctrl.currentPosition > 3500L) {
            ctrl.seekTo(0L)
        } else if (ctrl.hasPreviousMediaItem()) {
            ctrl.seekToPreviousMediaItem()
        }
    }

    fun toggleShuffle() {
        val ctrl = controller ?: return
        val next = !ctrl.shuffleModeEnabled
        ctrl.shuffleModeEnabled = next
        _state.value = _state.value.copy(shuffleEnabled = next)
    }

    fun cycleRepeatMode() {
        val ctrl = controller ?: return
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        ctrl.repeatMode = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        _state.value = _state.value.copy(repeatMode = nextMode)
    }

    fun setAbLoopPointA() {
        val pos = controller?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs
        val curB = _state.value.abLoopEndMs
        _state.value = _state.value.copy(
            abLoopStartMs = pos,
            abLoopEndMs = if (curB != null && curB <= pos) null else curB
        )
    }

    fun setAbLoopPointB() {
        val pos = controller?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs
        val start = _state.value.abLoopStartMs ?: 0L
        if (pos > start + 500L) {
            _state.value = _state.value.copy(
                abLoopStartMs = start,
                abLoopEndMs = pos
            )
            controller?.seekTo(start)
        }
    }

    fun clearAbLoop() {
        _state.value = _state.value.copy(abLoopStartMs = null, abLoopEndMs = null)
    }

    fun setSpeedAndPitch(speed: Float, pitch: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2.0f)
        val safePitch = pitch.coerceIn(0.5f, 2.0f)
        controller?.playbackParameters = PlaybackParameters(safeSpeed, safePitch)
        _state.value = _state.value.copy(playbackSpeed = safeSpeed, playbackPitch = safePitch)
    }

    fun setCrossfadeSeconds(seconds: Int) {
        _state.value = _state.value.copy(crossfadeSeconds = seconds.coerceIn(0, 12))
    }

    fun removeFromQueue(index: Int) {
        val ctrl = controller ?: return
        if (index !in 0 until ctrl.mediaItemCount) return
        ctrl.removeMediaItem(index)
        val updatedQueue = _state.value.queue.toMutableList()
        if (index in updatedQueue.indices) {
            updatedQueue.removeAt(index)
        }
        _state.value = _state.value.copy(
            queue = updatedQueue,
            currentIndex = ctrl.currentMediaItemIndex
        )
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        AudioSpectrumBus.sleepFadeGain = 1f
        if (minutes <= 0) {
            _state.value = _state.value.copy(sleepTimerRemainingSec = null, sleepAfterCurrentTrack = false)
            return
        }
        val totalSeconds = minutes * 60
        _state.value = _state.value.copy(sleepTimerRemainingSec = totalSeconds, sleepAfterCurrentTrack = false)
        sleepTimerJob = scope.launch(Dispatchers.Main) {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                if (remaining in 1..10) {
                    AudioSpectrumBus.sleepFadeGain = (remaining / 10f).coerceIn(0.08f, 1f)
                }
                _state.value = _state.value.copy(sleepTimerRemainingSec = remaining)
            }
            if (isActive && remaining <= 0) {
                controller?.pause()
                AudioSpectrumBus.sleepFadeGain = 1f
                _state.value = _state.value.copy(sleepTimerRemainingSec = null)
            }
        }
    }

    fun setSleepAfterCurrentTrack(enabled: Boolean) {
        sleepTimerJob?.cancel()
        AudioSpectrumBus.sleepFadeGain = 1f
        _state.value = _state.value.copy(
            sleepTimerRemainingSec = null,
            sleepAfterCurrentTrack = enabled
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun attachPlayerListener(ctrl: MediaController) {
        ctrl.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startPositionTicker()
                } else {
                    positionJob?.cancel()
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _state.value = _state.value.copy(shuffleEnabled = shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _state.value = _state.value.copy(
                    repeatMode = when (repeatMode) {
                        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                        else -> RepeatMode.OFF
                    }
                )
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (_state.value.sleepAfterCurrentTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    ctrl.pause()
                    _state.value = _state.value.copy(sleepAfterCurrentTrack = false)
                }
                _state.value = _state.value.copy(abLoopStartMs = null, abLoopEndMs = null)
                syncFromPlayer(ctrl)
                val track = mediaItem?.mediaId?.let { trackMap[it] }
                if (track != null) {
                    notifyTrackStartedIfNeeded(track)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncFromPlayer(ctrl)
                if (playbackState == Player.STATE_READY) {
                    val realDur = ctrl.duration
                    val currentId = ctrl.currentMediaItem?.mediaId
                    if (realDur > 0L && currentId != null) {
                        onDurationDiscovered(currentId, realDur)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    errorMessage = "No se pudo reproducir este archivo: ${error.errorCodeName}"
                )
            }
        })
    }

    private fun syncFromPlayer(ctrl: MediaController) {
        val index = ctrl.currentMediaItemIndex
        val currentId = ctrl.currentMediaItem?.mediaId
        val track = currentId?.let { trackMap[it] } ?: _state.value.queue.getOrNull(index)
        val dur = ctrl.duration.takeIf { it > 0L } ?: track?.durationMs ?: 0L
        _state.value = _state.value.copy(
            currentTrack = track,
            currentIndex = index,
            isPlaying = ctrl.isPlaying,
            positionMs = ctrl.currentPosition.coerceAtLeast(0L),
            durationMs = dur,
            shuffleEnabled = ctrl.shuffleModeEnabled,
            repeatMode = when (ctrl.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            }
        )
        if (ctrl.isPlaying) {
            startPositionTicker()
        }
    }

    private fun startPositionTicker() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val ctrl = controller
                if (ctrl != null && ctrl.isPlaying) {
                    val pos = ctrl.currentPosition.coerceAtLeast(0L)
                    val dur = ctrl.duration.takeIf { it > 0L } ?: _state.value.durationMs

                    val abA = _state.value.abLoopStartMs
                    val abB = _state.value.abLoopEndMs
                    if (abA != null && abB != null && abB > abA && pos >= abB) {
                        ctrl.seekTo(abA)
                    }

                    val crossfadeMs = _state.value.crossfadeSeconds * 1000L
                    if (crossfadeMs > 0L && dur > crossfadeMs * 2L) {
                        val remaining = (dur - pos).coerceAtLeast(0L)
                        AudioSpectrumBus.crossfadeGain = when {
                            remaining < crossfadeMs -> (remaining.toFloat() / crossfadeMs).coerceIn(0.15f, 1f)
                            pos < crossfadeMs / 2L -> (pos.toFloat() / (crossfadeMs / 2f)).coerceIn(0.25f, 1f)
                            else -> 1f
                        }
                    } else {
                        AudioSpectrumBus.crossfadeGain = 1f
                    }
                    _state.value = _state.value.copy(positionMs = pos, durationMs = dur)
                }
                delay(120L)
            }
        }
    }

    private fun notifyTrackStartedIfNeeded(track: Track) {
        if (lastReportedTrackId != track.id) {
            lastReportedTrackId = track.id
            onTrackStarted(track)
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri?.let { Uri.parse(it) })
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.parse(uri))
            .setMediaMetadata(metadata)
            .build()
    }
}
