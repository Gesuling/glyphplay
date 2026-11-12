package com.glyphplay.player.ui

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.glyphplay.player.data.MusicRepository
import com.glyphplay.player.data.Track
import com.glyphplay.player.player.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = true,
    val permissionNeeded: Boolean = false,
    val current: Track? = null,
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val sleepMinutes: Int = 0,
    val tab: Tab = Tab.Library,
)

enum class Tab { Library, Now }

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MusicRepository(app)
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val c = controller ?: return
            _state.update {
                it.copy(
                    positionMs = c.currentPosition.coerceAtLeast(0),
                    durationMs = c.duration.coerceAtLeast(0),
                    playing = c.isPlaying,
                )
            }
            handler.postDelayed(this, 200)
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromPlayer(player)
        }
    }

    fun onPermission(granted: Boolean) {
        if (!granted) {
            _state.update { it.copy(permissionNeeded = true, loading = false) }
            return
        }
        _state.update { it.copy(permissionNeeded = false) }
        connect()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val tracks = repo.loadTracks()
            _state.update { it.copy(tracks = tracks, loading = false) }
        }
    }

    fun openTab(tab: Tab) {
        _state.update { it.copy(tab = tab) }
    }

    fun play(track: Track) {
        val c = controller ?: return
        val items = _state.value.tracks.map { it.toMediaItem() }
        val index = _state.value.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        c.setMediaItems(items, index, 0)
        c.prepare()
        c.play()
        _state.update { it.copy(tab = Tab.Now, current = track) }
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seek(fraction: Float) {
        val c = controller ?: return
        val dur = c.duration
        if (dur > 0) c.seekTo((dur * fraction).toLong())
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
        _state.update { it.copy(shuffle = c.shuffleModeEnabled) }
    }

    fun cycleRepeat() {
        val c = controller ?: return
        val next = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        c.repeatMode = next
        _state.update { it.copy(repeat = next) }
    }

    fun setSleepTimer(minutes: Int) {
        val c = controller ?: return

        val args = Bundle().apply {
            putInt(PlaybackService.EXTRA_MINUTES, minutes)
        }

        val command = SessionCommand(
            PlaybackService.COMMAND_SLEEP,
            Bundle(),
        )

        c.sendCustomCommand(command, args)

        _state.update {
            it.copy(sleepMinutes = minutes)
        }
    }

    private fun connect() {
        if (controller != null) return
        val token = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlaybackService::class.java),
        )
        val future = MediaController.Builder(getApplication(), token).buildAsync()
        future.addListener(
            {
                val c = future.get()
                controller = c
                c.addListener(listener)
                syncFromPlayer(c)
                handler.post(tick)
            },
            ContextCompat.getMainExecutor(getApplication()),
        )
    }

    private fun syncFromPlayer(player: Player) {
        val id = player.currentMediaItem?.mediaId?.toLongOrNull()
        val current = _state.value.tracks.find { it.id == id }
        _state.update {
            it.copy(
                current = current ?: it.current,
                playing = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0),
                durationMs = player.duration.coerceAtLeast(0),
                shuffle = player.shuffleModeEnabled,
                repeat = player.repeatMode,
            )
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artworkUri)
                    .build(),
            )
            .build()
    }

    override fun onCleared() {
        handler.removeCallbacks(tick)
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
