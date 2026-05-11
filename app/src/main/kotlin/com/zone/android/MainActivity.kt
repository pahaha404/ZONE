package com.zone.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.zone.android.core.ui.ZoneTheme

/**
 * Single-activity entry point for the ZONE MVP.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appContainer = remember { AppContainer(applicationContext) }
            ZoneTheme {
                ZoneNavHost(appContainer = appContainer)
            }
        }
    }
}
