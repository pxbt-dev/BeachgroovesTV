package com.beachgrooves.tv

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * Full-screen "now playing" page for the TV.
 *
 * Starts the radio on launch. OK (or the remote's play/pause key) toggles playback.
 * Home leaves the radio playing in the background; Back pressed twice stops it and exits.
 */
class MainActivity : Activity() {

    private lateinit var playButton: ImageButton
    private lateinit var nowPlaying: TextView
    private lateinit var status: TextView

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var autoPlayed = false
    private var lastBackAt = 0L

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playButton = findViewById(R.id.play_button)
        nowPlaying = findViewById(R.id.now_playing)
        status = findViewById(R.id.status)

        playButton.setOnClickListener { togglePlayback() }
        playButton.requestFocus()
        autoPlayed = savedInstanceState?.getBoolean(KEY_AUTO_PLAYED) ?: false
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            if (future.isCancelled) return@addListener
            val c = future.get()
            controller = c
            c.addListener(listener)
            if (!autoPlayed) {
                autoPlayed = true
                c.play()
            }
            render()
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AUTO_PLAYED, autoPlayed)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastBackAt < BACK_WINDOW_MS) {
                controller?.pause()
                finish()
            } else {
                lastBackAt = now
                Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun togglePlayback() {
        val c = controller ?: return
        if (c.playWhenReady) c.pause() else c.play()
    }

    private fun render() {
        val c = controller ?: return
        val wantsToPlay = c.playWhenReady

        playButton.setImageResource(if (wantsToPlay) R.drawable.ic_pause else R.drawable.ic_play)
        playButton.contentDescription = getString(if (wantsToPlay) R.string.pause else R.string.play)

        val show = c.mediaMetadata.title?.toString()?.trim()
        nowPlaying.text = show.orEmpty()
        nowPlaying.visibility = if (show.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE

        status.setText(
            when {
                !wantsToPlay -> R.string.paused
                c.playerError != null -> R.string.reconnecting
                c.isPlaying -> R.string.live
                else -> R.string.connecting
            }
        )
    }

    companion object {
        private const val KEY_AUTO_PLAYED = "autoPlayed"
        private const val BACK_WINDOW_MS = 2_000L
    }
}
