package com.zone.android.feature.library

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zone.android.core.model.VideoItem
import com.zone.android.core.ui.StatusCard
import com.zone.android.core.ui.ZoneBackground
import com.zone.android.core.ui.ZoneHeader
import com.zone.android.core.ui.ZoneMediaFrame
import com.zone.android.core.ui.ZonePill
import java.util.concurrent.TimeUnit

/**
 * Library entry screen for importing and choosing local videos.
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onVideoSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val launcher = rememberLauncherForActivityResult(OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.importVideo(uri.toString())
        }
    }

    ZoneBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                ZoneHeader(
                    eyebrow = "ZONE",
                    title = "영상에만 남습니다.",
                    subtitle = "움직임과 얼굴 방향을 확인하면서 끝까지 보게 만드는 집중 세션 플레이어입니다.",
                    trailing = {
                        ZonePill(text = "영상 ${uiState.videos.size}개", emphasized = uiState.videos.isNotEmpty())
                    },
                )
            }

            item {
                StatusCard(title = "영상 가져오기") {
                    Text(
                        text = "휴대폰 로컬 영상을 가져와서 세션을 시작합니다. 한번 시작하면 중간에 흐름이 끊기지 않도록 강하게 관리됩니다.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { launcher.launch(arrayOf("video/*")) },
                        enabled = !uiState.isImporting,
                    ) {
                        if (uiState.isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(text = "영상 선택")
                    }
                    uiState.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(10.dp))
                        ZonePill(text = message)
                    }
                }
            }

            if (uiState.videos.isEmpty()) {
                item {
                    StatusCard(title = "대기 중") {
                        Text(
                            text = "아직 가져온 영상이 없습니다. 영상을 하나 불러오면 ZONE 세션으로 바로 전환됩니다.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            } else {
                items(uiState.videos, key = VideoItem::id) { video ->
                    VideoItemCard(
                        video = video,
                        onClick = { onVideoSelected(video.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoItemCard(
    video: VideoItem,
    onClick: () -> Unit,
) {
    val bitmap = video.thumbnailBytes?.let {
        BitmapFactory.decodeByteArray(it, 0, it.size)
    }

    ZoneMediaFrame(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
            .clickable(onClick = onClick),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF262831),
                                Color(0xFF0C0D12),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.84f),
                        ),
                    ),
                ),
        )

        Text(
            text = "ZONE.",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )

        ZonePill(
            text = formatDuration(video.durationMs),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            emphasized = true,
        )

        if (bitmap != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp)
                    .size(62.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.72f),
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZonePill(text = "탭해서 시작")
                ZonePill(text = "로컬 파일")
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
