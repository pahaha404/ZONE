package com.zone.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zone.android.core.model.CalibrationProfile
import com.zone.android.core.model.StrictnessLevel
import com.zone.android.core.model.VideoItem
import com.zone.android.core.model.calibrationRuntimeConfig
import com.zone.android.core.model.sessionRuntimeConfig
import com.zone.android.core.ui.ZoneBackground
import com.zone.android.core.ui.ZoneHeader
import com.zone.android.feature.library.LibraryScreen
import com.zone.android.feature.library.LibraryViewModel
import com.zone.android.feature.report.ReportScreen
import com.zone.android.feature.report.ReportViewModel
import com.zone.android.feature.session.ActiveSessionScreen
import com.zone.android.feature.session.CalibrationScreen
import com.zone.android.feature.session.CalibrationViewModel
import com.zone.android.feature.session.DefaultSessionController
import com.zone.android.feature.session.SessionSetupScreen
import com.zone.android.feature.session.SessionSetupViewModel
import kotlinx.coroutines.flow.first

/**
 * Top-level navigation graph for the ZONE MVP flow.
 */
@Composable
fun ZoneNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    NavHost(
        navController = navController,
        startDestination = "library",
        modifier = modifier,
    ) {
        composable("library") {
            val viewModel: LibraryViewModel = viewModel(
                factory = SingleViewModelFactory {
                    LibraryViewModel(appContainer.videoRepository)
                },
            )
            LibraryScreen(
                viewModel = viewModel,
                onVideoSelected = { videoId ->
                    navController.navigate("setup/$videoId")
                },
            )
        }

        composable(
            route = "setup/{videoId}",
            arguments = listOf(navArgument("videoId") { type = NavType.LongType }),
        ) { entry ->
            val videoId = entry.arguments?.getLong("videoId") ?: return@composable
            val viewModel: SessionSetupViewModel = viewModel(
                key = "setup-$videoId",
                factory = SingleViewModelFactory {
                    SessionSetupViewModel(
                        videoId = videoId,
                        videoRepository = appContainer.videoRepository,
                        settingsRepository = appContainer.settingsRepository,
                    )
                },
            )
            SessionSetupScreen(
                viewModel = viewModel,
                onStartCalibration = { chosenVideoId, strictness ->
                    navController.navigate("calibration/$chosenVideoId/${strictness.name}")
                },
            )
        }

        composable(
            route = "calibration/{videoId}/{strictness}",
            arguments = listOf(
                navArgument("videoId") { type = NavType.LongType },
                navArgument("strictness") { type = NavType.StringType },
            ),
        ) { entry ->
            val videoId = entry.arguments?.getLong("videoId") ?: return@composable
            val strictness = entry.arguments?.getString("strictness")
                ?.let(StrictnessLevel::valueOf)
                ?: StrictnessLevel.STANDARD
            val runtimeConfig = remember(videoId, strictness) {
                strictness.calibrationRuntimeConfig()
            }
            val sensorFocusEngine = remember(videoId, strictness) {
                appContainer.createSensorFocusEngine(runtimeConfig)
            }
            val faceAttentionAnalyzer = remember(videoId, strictness, lifecycleOwner) {
                appContainer.createFaceAttentionAnalyzer(
                    lifecycleOwner = lifecycleOwner,
                    runtimeConfig = runtimeConfig,
                )
            }
            val viewModel: CalibrationViewModel = viewModel(
                key = "calibration-$videoId-${strictness.name}",
                factory = SingleViewModelFactory {
                    CalibrationViewModel(
                        videoId = videoId,
                        strictnessLevel = strictness,
                        runtimeConfig = runtimeConfig,
                        sessionRepository = appContainer.sessionRepository,
                        sensorFocusEngine = sensorFocusEngine,
                        faceAttentionAnalyzer = faceAttentionAnalyzer,
                    )
                },
            )
            CalibrationScreen(
                viewModel = viewModel,
                onCalibrationCompleted = {
                    navController.navigate("session/$videoId/${strictness.name}") {
                        popUpTo("library")
                    }
                },
            )
        }

        composable(
            route = "session/{videoId}/{strictness}",
            arguments = listOf(
                navArgument("videoId") { type = NavType.LongType },
                navArgument("strictness") { type = NavType.StringType },
            ),
        ) { entry ->
            val videoId = entry.arguments?.getLong("videoId") ?: return@composable
            val strictness = entry.arguments?.getString("strictness")
                ?.let(StrictnessLevel::valueOf)
                ?: StrictnessLevel.STANDARD

            val runtimeState by produceState<SessionRouteState>(
                initialValue = SessionRouteState.Loading,
                key1 = videoId,
                key2 = strictness,
            ) {
                val video = appContainer.videoRepository.getVideo(videoId)
                val profile = appContainer.sessionRepository.getLatestCalibrationProfile(videoId, strictness)
                val sessionIntent = appContainer.settingsRepository.sessionIntent.first().trim()
                value = if (video != null && profile != null && sessionIntent.isNotBlank()) {
                    SessionRouteState.Ready(video, profile, sessionIntent)
                } else {
                    SessionRouteState.Error("영상, 기준 자세 프로필, 세션 목표 중 하나가 비어 있습니다.")
                }
            }

            when (val state = runtimeState) {
                SessionRouteState.Loading -> LoadingSurface()
                is SessionRouteState.Error -> ErrorSurface(
                    message = state.message,
                    onAction = { navController.popBackStack("library", inclusive = false) },
                )
                is SessionRouteState.Ready -> {
                    val runtimeConfig = remember(videoId, strictness, state.profile.id) {
                        state.profile.sessionRuntimeConfig()
                    }
                    val sensorFocusEngine = remember(videoId, strictness, state.profile.id) {
                        appContainer.createSensorFocusEngine(runtimeConfig)
                    }
                    val faceAttentionAnalyzer = remember(videoId, strictness, lifecycleOwner) {
                        appContainer.createFaceAttentionAnalyzer(
                            lifecycleOwner = lifecycleOwner,
                            runtimeConfig = runtimeConfig,
                        )
                    }
                    val playerCoordinator = remember(videoId, strictness) { appContainer.createPlayerCoordinator() }
                    val controller = remember(videoId, strictness, state.profile.id) {
                        appContainer.createSessionController(
                            videoItem = state.video,
                            strictnessLevel = strictness,
                            runtimeConfig = runtimeConfig,
                            sessionIntent = state.sessionIntent,
                            sensorFocusEngine = sensorFocusEngine,
                            faceAttentionAnalyzer = faceAttentionAnalyzer,
                            playerCoordinator = playerCoordinator,
                        )
                    }
                    ActiveSessionScreen(
                        controller = controller,
                        playerCoordinator = playerCoordinator,
                        onFinished = { sessionId ->
                            navController.navigate("report/$sessionId") {
                                popUpTo("library")
                            }
                        },
                        onRecalibrationRequested = {
                            controller.finish("recalibration_requested")
                            navController.navigate("calibration/$videoId/${strictness.name}")
                        },
                    )
                }
            }
        }

        composable(
            route = "report/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) { entry ->
            val sessionId = entry.arguments?.getLong("sessionId") ?: return@composable
            val viewModel: ReportViewModel = viewModel(
                key = "report-$sessionId",
                factory = SingleViewModelFactory {
                    ReportViewModel(
                        sessionId = sessionId,
                        sessionRepository = appContainer.sessionRepository,
                    )
                },
            )
            ReportScreen(
                viewModel = viewModel,
                onDone = {
                    navController.navigate("library") {
                        popUpTo("library") { inclusive = true }
                    }
                },
            )
        }
    }
}

private sealed interface SessionRouteState {
    data object Loading : SessionRouteState
    data class Ready(
        val video: VideoItem,
        val profile: CalibrationProfile,
        val sessionIntent: String,
    ) : SessionRouteState

    data class Error(val message: String) : SessionRouteState
}

@Composable
private fun LoadingSurface() {
    ZoneBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "ZONE.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ErrorSurface(
    message: String,
    onAction: () -> Unit,
) {
    ZoneBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ZoneHeader(
                eyebrow = "오류",
                title = "세션 준비에 실패했습니다.",
                subtitle = message,
            )
            Button(onClick = onAction) {
                Text("뒤로 가기")
            }
        }
    }
}
