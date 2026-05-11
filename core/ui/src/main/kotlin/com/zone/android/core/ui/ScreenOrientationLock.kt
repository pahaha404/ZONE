package com.zone.android.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Temporarily locks the hosting activity to the requested orientation while the current screen is visible.
 */
@Composable
fun ScreenOrientationLock(
    orientation: Int,
) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, orientation) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = orientation
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
