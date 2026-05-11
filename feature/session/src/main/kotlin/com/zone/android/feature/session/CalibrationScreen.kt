package com.zone.android.feature.session

import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zone.android.core.model.CalibrationProfile
import com.zone.android.core.model.displayName
import com.zone.android.core.ui.ScreenOrientationLock
import com.zone.android.core.ui.StatusCard
import com.zone.android.core.ui.ZoneBackground
import com.zone.android.core.ui.ZoneHeader
import com.zone.android.core.ui.ZoneMediaFrame
import com.zone.android.core.ui.ZoneMetricTile
import com.zone.android.core.ui.ZonePill
import kotlin.math.ceil

/**
 * 3-second stability gate shown before playback starts.
 */
@Composable
fun CalibrationScreen(
    viewModel: CalibrationViewModel,
    onCalibrationCompleted: (CalibrationProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage = uiState.errorMessage

    ScreenOrientationLock(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)

    LaunchedEffect(viewModel) {
        viewModel.startMonitoring()
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.stopMonitoring()
        }
    }

    uiState.savedProfile?.let { savedProfile ->
        LaunchedEffect(savedProfile.id) {
            onCalibrationCompleted(savedProfile)
        }
    }

    ZoneBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CalibrationSummary(
                            uiState = uiState,
                            errorMessage = errorMessage,
                            compact = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    CalibrationStage(
                        uiState = uiState,
                        errorMessage = errorMessage,
                        onRetry = viewModel::retryMonitoring,
                        compact = true,
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    CalibrationStage(
                        uiState = uiState,
                        errorMessage = errorMessage,
                        onRetry = viewModel::retryMonitoring,
                        compact = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                    )
                    CalibrationSummary(
                        uiState = uiState,
                        errorMessage = errorMessage,
                        compact = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalibrationStage(
    uiState: CalibrationUiState,
    errorMessage: String?,
    onRetry: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val remainingCalibrationMs = (uiState.targetProgressMs - uiState.progressMs).coerceAtLeast(0L)
    ZoneMediaFrame(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 20.dp else 22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZonePill(text = if (uiState.faceDetected) "얼굴 감지됨" else "얼굴 없음", emphasized = uiState.faceDetected)
                ZonePill(text = if (uiState.stable) "안정적" else "조정 중")
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (errorMessage == null) "정면 자세를 유지해 주세요." else "카메라 확인이 필요합니다.",
                    style = if (compact) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = errorMessage ?: "ZONE은 움직임, 얼굴 크기, 머리 방향으로 기준 자세를 기록합니다. 실제로 영상을 볼 자세 그대로 측정해 주세요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (errorMessage == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LinearProgressIndicator(
                    progress = if (uiState.targetProgressMs == 0L) 0f else {
                        (uiState.progressMs.toFloat() / uiState.targetProgressMs.toFloat()).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = if (errorMessage == null) {
                        "기준 자세 유지 ${remainingWholeSeconds(remainingCalibrationMs)}초 남음"
                    } else {
                        "카메라 상태를 확인한 뒤 다시 시작해 주세요."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (errorMessage != null) {
                    Button(onClick = onRetry) {
                        Text("카메라 다시 시작")
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationSummary(
    uiState: CalibrationUiState,
    errorMessage: String?,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ZoneHeader(
            eyebrow = "기준 자세 측정",
            title = "가로 화면에서 기준 자세를 기록합니다.",
            subtitle = if (compact) {
                "전면 카메라와 센서가 모두 안정적으로 잡히면 자동으로 다음 단계로 넘어갑니다."
            } else {
                "세션 시작 전 3초 동안 얼굴을 중앙에 두고 휴대폰을 안정적으로 유지해 주세요."
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ZoneMetricTile(
                label = "얼굴",
                value = if (uiState.faceDetected) "감지됨" else "없음",
                modifier = Modifier.weight(1f),
            )
            ZoneMetricTile(
                label = "중앙 정렬",
                value = if (uiState.centered) "정렬됨" else "조정 필요",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ZoneMetricTile(
                label = "움직임",
                value = "%.2f".format(uiState.motionEnergy),
                modifier = Modifier.weight(1f),
            )
            ZoneMetricTile(
                label = "얼굴 비율",
                value = "%.3f".format(uiState.faceSizeRatio),
                modifier = Modifier.weight(1f),
            )
        }

        StatusCard(title = "카메라 상태") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = uiState.cameraStatus.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (errorMessage == null) {
                    Text(
                        text = "기준 자세가 기록되면 곧바로 영상 시작 카운트다운으로 넘어갑니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun remainingWholeSeconds(durationMs: Long): Int =
    if (durationMs <= 0L) 0 else ceil(durationMs / 1000.0).toInt()
