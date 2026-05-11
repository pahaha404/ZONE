package com.zone.android.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater
import androidx.media3.ui.PlayerView

/**
 * Compose wrapper around Media3's [PlayerView].
 */
@Composable
fun ZonePlayerSurface(
    coordinator: ExoPlayerCoordinator,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                (LayoutInflater.from(context).inflate(R.layout.view_zone_player, null, false) as PlayerView).apply {
                    player = coordinator.player
                }
            },
            update = { view ->
                view.player = coordinator.player
            },
        )
        overlay()
    }
}
