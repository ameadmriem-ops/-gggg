package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ads.AdMobManager
import com.example.player.VideoPlayerView
import com.example.ui.components.*
import com.example.ui.theme.VidoCoral
import com.example.ui.theme.VidoCyan
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel

@Composable
fun VideoDetailScreen(
    videoId: String,
    viewModel: VidoMixViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChannel: (String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeVideo by viewModel.activeVideo.collectAsStateWithLifecycle()
    val activeChannel by viewModel.activeChannel.collectAsStateWithLifecycle()
    val comments by viewModel.activeComments.collectAsStateWithLifecycle()
    val isLiked by viewModel.isCurrentVideoLiked.collectAsStateWithLifecycle()
    val isSaved by viewModel.isCurrentVideoSaved.collectAsStateWithLifecycle()
    val isFollowed by viewModel.isCurrentChannelFollowed.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allVideos by viewModel.rawLongVideos.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    val channelMap = remember(channels) { channels.associateBy { it.id } }
    val userMap = remember(users) { users.associateBy { it.id } }

    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Prepare and play the video
    LaunchedEffect(videoId) {
        viewModel.openVideoDetail(videoId)
    }

    var hasPreRollShown by remember(videoId) { mutableStateOf(false) }

    LaunchedEffect(activeVideo?.videoUrl) {
        val url = activeVideo?.videoUrl ?: return@LaunchedEffect
        if (!hasPreRollShown) {
            hasPreRollShown = true
            val activity = context as? Activity
            if (activity != null) {
                AdMobManager.showPreRollAd(
                    activity = activity,
                    onDismiss = {
                        viewModel.playerController.prepareAndPlay(url, autoPlay = true)
                    },
                    onPaidEvent = { res ->
                        viewModel.recordAdImpression(
                            adUnitId = AdMobManager.VIDEO_PREROLL_AD_UNIT_ID,
                            videoId = videoId,
                            adFormat = "PRE_ROLL",
                            revenueValue = res.revenueValue,
                            currency = res.currency,
                            precision = res.precision
                        )
                    }
                )
            } else {
                viewModel.playerController.prepareAndPlay(url, autoPlay = true)
            }
        } else {
            viewModel.playerController.prepareAndPlay(url, autoPlay = true)
        }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onNavigateBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.playerController.getPlayer().pause()
        }
    }

    if (activeVideo == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val video = activeVideo!!
    val recommendedList = remember(allVideos, videoId) {
        allVideos.filter { it.id != videoId }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("video_detail_screen")
    ) {
        // Video Player Box (Adapts to fullscreen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFullscreen) Modifier.fillMaxHeight()
                    else Modifier.aspectRatio(16f / 9f)
                )
                .background(Color.Black)
        ) {
            VideoPlayerView(
                controller = viewModel.playerController,
                modifier = Modifier.fillMaxSize(),
                isFullscreen = isFullscreen,
                onToggleFullscreen = { isFullscreen = !isFullscreen }
            )

            // Top Bar with Back Button (Visible when not in full ExoPlayer overlay)
            IconButton(
                onClick = {
                    if (isFullscreen) isFullscreen = false
                    else onNavigateBack()
                },
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("player_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (!isFullscreen) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_detail_scroll_content"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video Title
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            color = Color(0xFFFFB300).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "إعلان مدعوم • Ad Supported",
                                color = Color(0xFFFFB300),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 22.sp
                            )
                        )
                    }
                }

                // Stats & Expandable Description
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${formatViews(video.viewsCount)} مشاهدة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "•",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTimeAgo(video.uploadTimestamp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = if (isDescriptionExpanded) "إخفاء" else "المزيد...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = video.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            ),
                            maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2
                        )

                        if (isDescriptionExpanded && video.tags.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = video.tags.split(",").joinToString(" ") { "#${it.trim()}" },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Channel Info & Follow/Subscribe Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    activeChannel?.let { onNavigateToChannel(it.id) }
                                }
                        ) {
                            AsyncImage(
                                model = activeChannel?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                                contentDescription = activeChannel?.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = activeChannel?.name ?: "قناة VidoMix",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (activeChannel?.isVerified == true) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "موثق",
                                            tint = VidoCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${formatViews(activeChannel?.subscriberCount?.toLong() ?: 0)} مشترك",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.toggleFollowActiveChannel() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                contentColor = if (isFollowed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("channel_follow_button")
                        ) {
                            Text(
                                text = if (isFollowed) "مشترك ✓" else "اشتراك +",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Action Buttons: Like, Dislike, Share, Save, Report
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleLikeActiveVideo() }
                                .padding(8.dp)
                                .testTag("detail_like_button")
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "إعجاب",
                                tint = if (isLiked) VidoCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatViews(video.likesCount),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Save
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleSaveActiveVideo() }
                                .padding(8.dp)
                                .testTag("detail_save_button")
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "حفظ",
                                tint = if (isSaved) VidoCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isSaved) "محفوظ" else "حفظ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Share
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "شاهد فيديو: ${video.title} على VidoMix")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة الفيديو"))
                                }
                                .padding(8.dp)
                                .testTag("detail_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "مشاركة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Report
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showReportDialog = true }
                                .padding(8.dp)
                                .testTag("detail_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = "إبلاغ",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "إبلاغ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Comments Preview Section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCommentsSheet = true }
                            .testTag("comments_preview_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "التعليقات (${comments.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "فتح الكل ❯",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (comments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val topComment = comments.first()
                                val author = userMap[topComment.userId]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AsyncImage(
                                        model = author?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                    )
                                    Text(
                                        text = topComment.content,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Recommended Next Videos Header
                item {
                    Text(
                        text = "فيديوهات مقترحة تالية",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                // Recommended Next Videos
                items(recommendedList, key = { it.id }) { recVideo ->
                    val recChannel = channelMap[recVideo.channelId]
                    VideoCard(
                        video = recVideo,
                        channel = recChannel,
                        onVideoClick = { nextId -> onNavigateToVideo(nextId) },
                        onChannelClick = onNavigateToChannel,
                        onSaveToggle = { viewModel.toggleSaveVideoById(it) },
                        onShareClick = { /* Handle share */ },
                        onReportClick = { showReportDialog = true }
                    )
                }
            }
        }
    }

    // Comments Sheet
    if (showCommentsSheet) {
        CommentsBottomSheet(
            comments = comments,
            currentUser = currentUser,
            onDismiss = { showCommentsSheet = false },
            onSendComment = { content, parentId ->
                viewModel.addCommentToActiveVideo(content, parentId)
            },
            getUserForComment = { userId -> userMap[userId] }
        )
    }

    // Report Dialog
    if (showReportDialog) {
        ReportDialog(
            targetType = "فيديو",
            onDismiss = { showReportDialog = false },
            onSubmitReport = { reason, details ->
                viewModel.submitReport("VIDEO", video.id, reason, details)
                showReportDialog = false
            }
        )
    }
}
