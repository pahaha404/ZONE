package com.zone.android.feature.session

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zone.android.core.model.SessionPhase
import com.zone.android.core.model.displayName
import com.zone.android.core.ui.ScreenOrientationLock
import com.zone.android.core.ui.ZonePill
import com.zone.android.feature.player.ExoPlayerCoordinator
import com.zone.android.feature.player.ZonePlayerSurface
import kotlin.math.ceil

/**
 * Playback screen with live ZONE enforcement overlays.
 */
@Composable
fun ActiveSessionScreen(
    controller: DefaultSessionController,
    playerCoordinator: ExoPlayerCoordinator,
    onFinished: (Long) -> Unit,
    onRecalibrationRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uiState by controller.uiState.collectAsState()
    val completedSessionId by controller.completedSessionId.collectAsState()

    ScreenOrientationLock(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)

    BackHandler(enabled = completedSessionId == null) {
        Unit
    }

    LaunchedEffect(controller) {
        controller.start()
    }

    DisposableEffect(controller) {
        onDispose {
            controller.finish("user_left_session")
        }
    }

    completedSessionId?.let { sessionId ->
        LaunchedEffect(sessionId) {
            onFinished(sessionId)
        }
    }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == SessionPhase.WARNING) {
            vibrateWarning(context)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        ZonePlayerSurface(
            coordinator = playerCoordinator,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.56f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ZONE.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                ZonePill(
                    text = "${elapsedWholeSeconds(uiState.playback.positionMs)}초",
                    modifier = Modifier.align(Alignment.CenterEnd),
                    emphasized = true,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZonePill(text = uiState.phase.displayName())
                if (uiState.phase == SessionPhase.WARNING) {
                    ZonePill(
                        text = uiState.warningMessage ?: "자세 조정",
                        emphasized = true,
                    )
                }
            }
        }

        when {
            uiState.phase == SessionPhase.READY || uiState.phase == SessionPhase.READY_COUNTDOWN -> {
                ReadyCountdownOverlay(
                    uiState = uiState,
                    onPrimaryAction = controller::onPrimaryAction,
                )
            }

            uiState.fatalErrorMessage != null ||
                uiState.phase in setOf(
                    SessionPhase.PAUSED_MOVEMENT,
                    SessionPhase.PAUSED_ATTENTION,
                    SessionPhase.RECOVERING,
                    SessionPhase.LOCKED_OUT,
                    SessionPhase.REQUIRES_RECALIBRATION,
                ) -> {
                EnforcementOverlay(
                    uiState = uiState,
                    onPrimaryAction = controller::onPrimaryAction,
                    onRecalibrationRequested = onRecalibrationRequested,
                )
            }
        }

        if (uiState.phase !in setOf(SessionPhase.READY, SessionPhase.READY_COUNTDOWN)) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ZonePill(text = "움직임 ${uiState.movementViolationCount}회")
                ZonePill(text = "집중 이탈 ${uiState.attentionViolationCount}회")
            }
        }
    }
}

@Composable
private fun ReadyCountdownOverlay(
    uiState: com.zone.android.core.model.SessionUiState,
    onPrimaryAction: () -> Unit,
) {
    val primaryActionLabel = uiState.primaryActionLabel
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ZonePill(
                text = if (uiState.phase == SessionPhase.READY_COUNTDOWN) "시작 카운트다운" else "시작 준비",
                emphasized = true,
            )
            Text(
                text = "영상을 시청할 준비가 됐나요?",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
            )
            Text(
                text = uiState.warningMessage ?: if (uiState.phase == SessionPhase.READY_COUNTDOWN) {
                    "10초 후에 영상이 시작됩니다"
                } else {
                    "화면을 바라보고 휴대폰을 고정하면 카운트다운이 시작됩니다"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            if (uiState.phase == SessionPhase.READY_COUNTDOWN) {
                Text(
                    text = countdownWholeSeconds(uiState.countdownRemainingMs).toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )
            }
            if (primaryActionLabel != null) {
                Button(
                    onClick = onPrimaryAction,
                    enabled = uiState.primaryActionEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White.copy(alpha = 0.28f),
                        disabledContentColor = Color.Black.copy(alpha = 0.55f),
                    ),
                ) {
                    Text(primaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun EnforcementOverlay(
    uiState: com.zone.android.core.model.SessionUiState,
    onPrimaryAction: () -> Unit,
    onRecalibrationRequested: () -> Unit,
) {
    val primaryActionLabel = uiState.primaryActionLabel
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ZonePill(
                    text = when {
                        uiState.fatalErrorMessage != null -> "세션 중단"
                        uiState.phase == SessionPhase.RECOVERING -> "복귀 확인 중"
                        uiState.phase == SessionPhase.LOCKED_OUT -> "일시 잠금"
                        uiState.phase == SessionPhase.REQUIRES_RECALIBRATION -> "재보정 필요"
                        else -> "재생 일시정지"
                    },
                    emphasized = true,
                )
                Text(
                    text = uiState.fatalErrorMessage ?: uiState.warningMessage ?: "일시정지됨",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                if (uiState.phase == SessionPhase.RECOVERING) {
                    Text(
                        text = "정면을 보고 가만히 유지해 주세요. ${countdownWholeSeconds(uiState.recoverRemainingMs)}초 후 다시 재생됩니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                }
                if (uiState.phase == SessionPhase.LOCKED_OUT) {
                    Text(
                        text = "복귀를 시작하려면 ${countdownWholeSeconds(uiState.lockoutRemainingMs)}초 기다려 주세요.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                }
                if (primaryActionLabel != null) {
                    Button(
                        onClick = onPrimaryAction,
                        enabled = uiState.primaryActionEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.White.copy(alpha = 0.28f),
                            disabledContentColor = Color.Black.copy(alpha = 0.55f),
                        ),
                    ) {
                        Text(primaryActionLabel)
                    }
                }
                if (uiState.fatalErrorMessage != null || uiState.phase == SessionPhase.REQUIRES_RECALIBRATION) {
                    Button(
                        onClick = onRecalibrationRequested,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text("다시 보정하기")
                    }
                }
            }
        }
    }
}

private fun vibrateWarning(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    vibrator.vibrate(VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE))
}

private fun elapsedWholeSeconds(durationMs: Long): Int =
    if (durationMs <= 0L) 0 else (durationMs / 1000L).toInt()

private fun countdownWholeSeconds(durationMs: Long): Int =
    if (durationMs <= 0L) 0 else ceil(durationMs / 1000.0).toInt()
