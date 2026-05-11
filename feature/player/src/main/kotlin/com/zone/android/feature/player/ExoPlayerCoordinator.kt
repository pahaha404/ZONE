package com.zone.android.feature.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import com.zone.android.core.model.PlaybackSnapshot
import com.zone.android.core.model.PlayerCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Media3-backed [PlayerCoordinator] implementation.
 */
class ExoPlayerCoordinator(
    context: Context,
) : PlayerCoordinator {
    val player: ExoPlayer = ExoPlayer.Builder(
        context,
        DefaultRenderersFactory(context).setEnableDecoderFallback(true),
    ).build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _playback = MutableStateFlow(PlaybackSnapshot())
    private var tickerJob: Job? = null

    override val playback: StateFlow<PlaybackSnapshot> = _playback.asStateFlow()

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    updateSnapshot()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateSnapshot()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateSnapshot()
                }
            },
        )
        tickerJob = scope.launch {
            while (isActive) {
                updateSnapshot()
                delay(250)
            }
        }
    }

    override fun prepare(contentUri: String) {
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(contentUri))
        player.prepare()
        updateSnapshot()
    }

    override fun play() {
        player.playWhenReady = true
        player.play()
        updateSnapshot()
    }

    override fun pause() {
        player.pause()
        updateSnapshot()
    }

    override fun stop() {
        player.stop()
        updateSnapshot()
    }

    override fun release() {
        tickerJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        player.release()
    }

    private fun updateSnapshot() {
        val duration = player.duration.takeIf { it >= 0 } ?: 0L
        _playback.value = PlaybackSnapshot(
            positionMs = player.currentPosition,
            durationMs = duration,
            isPlaying = player.isPlaying,
            isEnded = player.playbackState == Player.STATE_ENDED,
        )
    }
}
