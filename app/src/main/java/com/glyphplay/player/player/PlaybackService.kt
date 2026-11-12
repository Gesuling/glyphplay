package com.glyphplay.player.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.glyphplay.player.MainActivity
import com.glyphplay.player.data.LocalStore
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.atomic.AtomicBoolean

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var store: LocalStore

    private val persist = object : Runnable {
        override fun run() {
            val player = mediaSession?.player ?: return
            val id = player.currentMediaItem?.mediaId?.toLongOrNull()
            if (id != null && player.playbackState != Player.STATE_IDLE) {
                store.lastTrackId = id
                store.lastPositionMs = player.currentPosition.coerceAtLeast(0)
                store.shuffle = player.shuffleModeEnabled
                store.repeatMode = player.repeatMode
            }
            handler.postDelayed(this, 4_000)
        }
    }

    private val sleepTick = object : Runnable {
        override fun run() {
            val until = sleepUntilElapsed
            if (until <= 0L) return
            val left = until - SystemClock.elapsedRealtime()
            if (left <= 0L) {
                fadeAndStop()
            } else {
                handler.postDelayed(this, 1_000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = LocalStore(this)
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                skipUnplayable(player)
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId?.toLongOrNull() ?: return
                store.lastTrackId = id
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    store.recordPlayed(id)
                }
            }
        })

        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(launch)
            .setCallback(SessionCallback())
            .build()
        handler.post(persist)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        sleepUntilElapsed = 0L
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun scheduleSleep(minutes: Int) {
        handler.removeCallbacks(sleepTick)
        if (minutes <= 0) {
            sleepUntilElapsed = 0L
            return
        }
        sleepUntilElapsed = SystemClock.elapsedRealtime() + minutes * 60_000L
        handler.post(sleepTick)
    }

    private fun fadeAndStop() {
        val player = mediaSession?.player ?: return
        sleepUntilElapsed = 0L
        val start = player.volume.coerceIn(0.05f, 1f)
        var step = 0
        val steps = 12
        val fade = object : Runnable {
            override fun run() {
                step++
                val p = mediaSession?.player
                if (p == null) return
                if (step >= steps) {
                    p.volume = 1f
                    p.pause()
                    p.stop()
                    stoppedForSleep.set(true)
                    stopSelf()
                } else {
                    p.volume = start * (1f - step / steps.toFloat())
                    handler.postDelayed(this, 90)
                }
            }
        }
        handler.post(fade)
    }

    private fun skipUnplayable(player: Player) {
        try {
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            } else {
                player.pause()
            }
        } catch (_: Exception) {
            try {
                player.pause()
            } catch (_: Exception) {
            }
        }
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val extra = SessionCommand(COMMAND_SLEEP, Bundle())
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(extra)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == COMMAND_SLEEP) {
                scheduleSleep(args.getInt(EXTRA_MINUTES, 0))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    companion object {
        const val COMMAND_SLEEP = "com.glyphplay.player.SLEEP"
        const val EXTRA_MINUTES = "minutes"
        @Volatile
        var sleepUntilElapsed: Long = 0L
        val stoppedForSleep = AtomicBoolean(false)
    }
}
