package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChannelEntity
import com.example.data.model.UserEntity
import com.example.data.model.VideoEntity
import com.example.ui.theme.VidoCyan

@Composable
fun VideoCard(
    video: VideoEntity,
    channel: ChannelEntity?,
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    onSaveToggle: (String) -> Unit = {},
    onShareClick: (VideoEntity) -> Unit = {},
    onReportClick: (String) -> Unit = {},
    onMoreOptionsClick: ((VideoEntity) -> Unit)? = null,
    onNotInterested: (() -> Unit)? = null,
    onBlockCreator: ((String) -> Unit)? = null,
    onDislikeCategory: ((String) -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onEditVideo: ((String, String, String, String) -> Unit)? = null,
    onToggleVisibility: ((Boolean) -> Unit)? = null,
    onDeleteVideo: (() -> Unit)? = null,
    currentUser: UserEntity? = null,
    isSaved: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMoreSheet by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onVideoClick(video.id) }
            .testTag("video_card_${video.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail with Duration badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatSeconds(video.durationSeconds),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Info Row: Avatar + Title/Details + Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Channel Avatar
                AsyncImage(
                    model = channel?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                    contentDescription = channel?.name ?: "القناة",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { channel?.let { onChannelClick(it.id) } }
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Subtitle Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = channel?.name ?: "VidoMix Creator",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (channel?.isVerified == true) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "موثق",
                                tint = VidoCyan,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "${formatViews(video.viewsCount)} مشاهدة",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )

                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Text(
                            text = formatTimeAgo(video.uploadTimestamp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Options Menu button (Three dots ⋮)
                IconButton(
                    onClick = {
                        if (onMoreOptionsClick != null) {
                            onMoreOptionsClick(video)
                        } else {
                            showMoreSheet = true
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("video_options_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات إضافية",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Video More Menu Bottom Sheet
    if (showMoreSheet) {
        VideoMoreMenuBottomSheet(
            video = video,
            channel = channel,
            currentUser = currentUser,
            isSaved = isSaved,
            onDismiss = { showMoreSheet = false },
            onNotInterested = { onNotInterested?.invoke() ?: onReportClick(video.id) },
            onBlockCreator = { creatorId -> onBlockCreator?.invoke(creatorId) },
            onReport = { onReportClick(video.id) },
            onSaveToggle = { onSaveToggle(video.id) },
            onDislikeCategory = { cat -> onDislikeCategory?.invoke(cat) },
            onDownload = { onDownload?.invoke() },
            onEditVideo = { t, d, c, tg -> onEditVideo?.invoke(t, d, c, tg) },
            onToggleVisibility = { pub -> onToggleVisibility?.invoke(pub) },
            onDeleteVideo = { onDeleteVideo?.invoke() }
        )
    }
}

fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

fun formatViews(views: Long): String {
    return when {
        views >= 1_000_000 -> String.format("%.1f مليون", views / 1_000_000.0)
        views >= 1_000 -> String.format("%.1f ألف", views / 1_000.0)
        else -> views.toString()
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 30 -> "منذ شهر"
        days > 0 -> "منذ $days أيام"
        hours > 0 -> "منذ $hours ساعات"
        minutes > 0 -> "منذ $minutes دقيقة"
        else -> "الآن"
    }
}
