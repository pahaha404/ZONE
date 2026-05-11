package com.zone.android.feature.session

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zone.android.core.model.StrictnessLevel
import com.zone.android.core.model.displayName
import com.zone.android.core.ui.StatusCard
import com.zone.android.core.ui.ZoneBackground
import com.zone.android.core.ui.ZoneHeader
import com.zone.android.core.ui.ZoneMediaFrame
import com.zone.android.core.ui.ZonePill

/**
 * Screen for choosing strictness and granting required permission before calibration.
 */
@Composable
fun SessionSetupScreen(
    viewModel: SessionSetupViewModel,
    onStartCalibration: (Long, StrictnessLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    var permissionRefreshTick by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        permissionRefreshTick += 1
        if (!granted) {
            viewModel.markCameraPermissionRequested()
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionRefreshTick += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionGranted = remember(permissionRefreshTick, context) {
        hasCameraPermission(context)
    }
    val hasFrontCamera = remember(permissionRefreshTick, context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }
    val activity = context.findActivity()
    val permissionPermanentlyDenied = !permissionGranted &&
        uiState.cameraPermissionAsked &&
        activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    val permissionDenied = !permissionGranted && uiState.cameraPermissionAsked && !permissionPermanentlyDenied

    ZoneBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ZoneHeader(
                eyebrow = "세션 준비",
                title = uiState.video?.title ?: "세션 준비 중",
                subtitle = "휴대폰을 세워 두고 얼굴이 보이게 맞춘 뒤, ZONE이 재생 가능 여부를 판단합니다.",
            )

            ZoneMediaFrame(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ZonePill(text = "로컬 세션", emphasized = true)
                        ZonePill(text = uiState.selectedStrictness.displayName())
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "한 자세로 끝까지 봅니다.",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "기준 자세 측정으로 현재 자세를 먼저 기록합니다. 재생 중에는 얼굴 노출과 기기 안정성이 계속 확인됩니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            StatusCard(title = "강도 설정") {
                Text(
                    text = "세션을 얼마나 엄격하게 관리할지 선택합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StrictnessLevel.entries.forEach { level ->
                        FilterChip(
                            selected = uiState.selectedStrictness == level,
                            onClick = { viewModel.selectStrictness(level) },
                            label = { Text(level.displayName()) },
                        )
                    }
                }
            }

            StatusCard(title = "세션 목표") {
                Text(
                    text = "이번 세션에서 끝까지 지킬 목표를 한 줄로 적어 주세요. 비워 두면 세션이 시작되지 않습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.sessionIntent,
                    onValueChange = viewModel::updateSessionIntent,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 3,
                    label = { Text("이번 세션 목표") },
                    placeholder = { Text("예: 10분 동안 영상 끝까지 보기") },
                )
            }

            StatusCard(title = "카메라 권한") {
                val permissionMessage = when {
                    !hasFrontCamera -> "이 기기에서는 전면 카메라가 필요합니다."
                    permissionGranted -> "카메라가 준비되었습니다. ZONE은 기기 안에서 얼굴 노출과 머리 방향만 확인합니다."
                    permissionPermanentlyDenied -> "카메라 권한이 영구적으로 거부되었습니다. 앱 설정에서 다시 허용해 주세요."
                    permissionDenied -> "카메라 권한이 거부되었습니다. 기준 자세 측정 전에 허용해 주세요."
                    else -> "모니터링 세션을 시작하려면 카메라 권한이 필요합니다."
                }
                Text(text = permissionMessage, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                if (!permissionGranted && hasFrontCamera && !permissionPermanentlyDenied) {
                    Button(onClick = {
                        viewModel.markCameraPermissionRequested()
                        launcher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("카메라 권한 허용")
                    }
                }
                if (permissionPermanentlyDenied) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { context.openAppSettings() }) {
                        Text("앱 설정 열기")
                    }
                }
            }

            StatusCard(title = "선택 가드레일") {
                Text(
                    text = "아래 설정은 선택 사항입니다. 핵심 제어는 세션 자체가 수행합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    },
                ) {
                    Text("방해 금지 설정")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                    },
                ) {
                    Text("화면 고정 설정")
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                enabled = permissionGranted &&
                    hasFrontCamera &&
                    uiState.video != null &&
                    uiState.sessionIntent.isNotBlank(),
                onClick = {
                    val video = uiState.video ?: return@Button
                    viewModel.rememberVideoSelection()
                    onStartCalibration(video.id, uiState.selectedStrictness)
                },
            ) {
                Text("3초 기준 자세 측정 시작")
            }
        }
    }
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ),
    )
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
