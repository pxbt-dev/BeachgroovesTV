package com.beachgrooves.tv

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Plays the BeachGrooves Shoutcast stream in a media session, so the radio keeps playing
 * when the user presses Home and the remote's play/pause keys work from anywhere.
 *
 * It's a live stream, so "pause" drops the connection; play reconnects at the live point
 * instead of resuming stale buffered audio. Network errors retry every few seconds.
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        // The show name comes from the stream's ICY metadata as the title, so it's left unset here
        // (MediaItem metadata would override it).
        val metadata = MediaMetadata.Builder()
            .setStation(getString(R.string.app_name))
            .setArtist(getString(R.string.tagline))
            .setArtworkUri(Uri.parse("android.resource://$packageName/drawable/logo"))
            .build()
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(STREAM_URL)
                .setMediaMetadata(metadata)
                .build()
        )

        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    handler.removeCallbacksAndMessages(null)
                    player.stop()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({
                    if (player.playWhenReady && player.playbackState == Player.STATE_IDLE) {
                        player.prepare()
                    }
                }, RETRY_DELAY_MS)
            }
        })

        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        const val STREAM_URL = "https://stream.beachgrooves.com:9000/stream"
        private const val RETRY_DELAY_MS = 5_000L
    }
}
