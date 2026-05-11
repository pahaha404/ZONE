package com.zone.android.feature.report

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zone.android.core.model.SessionEvent
import com.zone.android.core.model.SessionEventType
import com.zone.android.core.model.SessionReport
import com.zone.android.core.model.SessionStanding
import com.zone.android.core.model.displayName
import com.zone.android.core.model.sessionEndReasonLabel
import com.zone.android.core.ui.StatusCard
import com.zone.android.core.ui.ScreenOrientationLock
import com.zone.android.core.ui.ZoneBackground
import com.zone.android.core.ui.ZoneHeader
import com.zone.android.core.ui.ZoneMetricTile
import com.zone.android.core.ui.ZonePill

/**
 * Session report screen shown after playback ends.
 */
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    ScreenOrientationLock(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)

    ZoneBackground(modifier = modifier) {
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.report == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ZoneHeader(
                        eyebrow = "리포트",
                        title = "리포트를 불러오지 못했습니다.",
                        subtitle = uiState.errorMessage ?: "세션 요약을 읽는 중 문제가 발생했습니다.",
                    )
                    Button(onClick = onDone) {
                        Text("라이브러리로 돌아가기")
                    }
                }
            }

            else -> {
                val report = checkNotNull(uiState.report)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        ZoneHeader(
                            eyebrow = "세션 리포트",
                            title = report.video.title,
                            subtitle = "이번 세션에서 얼마나 안정적으로 영상을 끝까지 봤는지 정리한 결과입니다.",
                            trailing = {
                                ZonePill(text = "점수 ${report.session.focusScore}", emphasized = true)
                            },
                        )
                    }

                    uiState.standing?.let { standing ->
                        item {
                            StatusCard(title = "로컬 랭킹") {
                                Text(
                                    text = "로컬 순위 ${standing.rank}위 / ${standing.totalSessions}회",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = standingTag(standing.rank, standing.totalSessions),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ZoneMetricTile(
                                label = "실재생",
                                value = "${report.session.actualPlaybackMs / 1000}초",
                                modifier = Modifier.weight(1f),
                            )
                            ZoneMetricTile(
                                label = "완주율",
                                value = "${(report.session.completionRate * 100).toInt()}%",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ZoneMetricTile(
                                label = "움직임",
                                value = report.session.movementViolationCount.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            ZoneMetricTile(
                                label = "집중 이탈",
                                value = report.session.attentionViolationCount.toString(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    item {
                        StatusCard(title = "집중 그래프") {
                            val focusRatio = report.session.focusScore / 100f
                            val playbackRatio = report.session.completionRate.coerceIn(0f, 1f)
                            val movementPenalty = (1f - (report.session.movementViolationCount / 3f)).coerceIn(0f, 1f)
                            val attentionPenalty = (1f - (report.session.attentionViolationCount / 3f)).coerceIn(0f, 1f)

                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                MetricBar(label = "집중 점수", value = "${report.session.focusScore}%", ratio = focusRatio)
                                MetricBar(label = "재생 진행", value = "${(playbackRatio * 100).toInt()}%", ratio = playbackRatio)
                                MetricBar(label = "움직임 제어", value = report.session.movementViolationCount.toString(), ratio = movementPenalty)
                                MetricBar(label = "시선 유지", value = report.session.attentionViolationCount.toString(), ratio = attentionPenalty)
                            }
                        }
                    }

                    item {
                        StatusCard(title = "세부 지표") {
                            Text(
                                text = "총 세션 시간 ${report.session.totalDurationMs / 1000}초",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "평균 안정 구간 ${report.session.averageStableSegmentMs / 1000f}초",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    item {
                        StatusCard(title = "이벤트 기록") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                report.events.forEach { event ->
                                    ZonePill(text = eventSummary(event))
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, buildShareText(report, uiState.standing))
                                            },
                                            "리포트 공유",
                                        ),
                                    )
                                },
                            ) {
                                Text("공유")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = onDone,
                            ) {
                                Text("라이브러리로")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBar(
    label: String,
    value: String,
    ratio: Float,
    barHeight: Dp = 10.dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .height(barHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

private fun standingTag(rank: Int, total: Int): String {
    if (total <= 1) return "첫 번째 기록 세션입니다"
    val percentile = rank.toFloat() / total.toFloat()
    return when {
        percentile <= 0.1f -> "로컬 상위 10%입니다"
        percentile <= 0.25f -> "로컬 상위 25%입니다"
        percentile <= 0.5f -> "로컬 상위 절반입니다"
        else -> "집중 루틴을 더 쌓아가고 있습니다"
    }
}

private fun eventSummary(event: SessionEvent): String {
    val message = event.message
    val detail = when {
        !message.isNullOrBlank() && event.eventType == SessionEventType.COMPLETED ->
            sessionEndReasonLabel(message)
        !message.isNullOrBlank() -> message
        else -> event.violation.displayName()
    }
    return "${event.eventType.displayName()}: $detail"
}

private fun buildShareText(
    report: SessionReport,
    standing: SessionStanding?,
): String = buildString {
    appendLine("ZONE 세션 리포트")
    appendLine("영상: ${report.video.title}")
    appendLine("점수: ${report.session.focusScore}")
    appendLine("완주율: ${(report.session.completionRate * 100).toInt()}%")
    appendLine("움직임 위반: ${report.session.movementViolationCount}")
    appendLine("집중 이탈: ${report.session.attentionViolationCount}")
    standing?.let {
        appendLine("로컬 순위: ${it.rank}위 / ${it.totalSessions}회")
    }
}
