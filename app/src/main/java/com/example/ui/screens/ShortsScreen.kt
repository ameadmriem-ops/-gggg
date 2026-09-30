package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
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
import com.example.core.clickableDebounced
import com.example.data.model.VideoEntity
import com.example.player.VideoPlayerView
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.ReportDialog
import com.example.ui.components.formatViews
import com.example.ui.theme.VidoCoral
import com.example.ui.theme.VidoCyan
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel

sealed class ShortsFeedItem {
    data class VideoItem(val video: VideoEntity) : ShortsFeedItem()
    data class AdItem(
        val adId: String,
        val sponsorName: String,
        val sponsorLogo: String,
        val title: String,
        val description: String,
        val bannerUrl: String,
        val ctaText: String,
        val targetUrl: String
    ) : ShortsFeedItem()
}

val sampleSponsors = listOf(
    ShortsFeedItem.AdItem(
        adId = "ad_game_1",
        sponsorName = "Cyber Legends 2026",
        sponsorLogo = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=200&auto=format&fit=crop&q=80",
        title = "انضم إلى معركة المستقبل! العب مجاناً الآن على هاتفك",
        description = "أقوى لعبة مغامرات وتقمص أدوار لعام 2026. رسومات خارقة وملايين اللاعبين حول العالم!",
        bannerUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600&auto=format&fit=crop&q=80",
        ctaText = "تثبيت اللعبة مجاناً 🎮",
        targetUrl = "https://play.google.com"
    ),
    ShortsFeedItem.AdItem(
        adId = "ad_tech_2",
        sponsorName = "AI Cloud Studio Pro",
        sponsorLogo = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200&auto=format&fit=crop&q=80",
        title = "صمم تطبيقاتك بضغطة زر باستخدام الذكاء الاصطناعي",
        description = "منصة سحابية متكاملة لرواد الأعمال والمبرمجين لإنشاء وتطوير المشاريع بسرعة قياسية.",
        bannerUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&auto=format&fit=crop&q=80",
        ctaText = "تجربة مجانية الآن ⚡",
        targetUrl = "https://google.com"
    )
)

@Composable
fun ShortsScreen(
    viewModel: VidoMixViewModel,
    onNavigateToChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shortsList by viewModel.shortsList.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val comments by viewModel.activeComments.collectAsStateWithLifecycle()

    val channelMap = remember(channels) { channels.associateBy { it.id } }
    val userMap = remember(users) { users.associateBy { it.id } }

    var showCommentsSheet by remember { mutableStateOf(false) }
    var reportingShortId by remember { mutableStateOf<String?>(null) }
    var activeShortVideo by remember { mutableStateOf<VideoEntity?>(null) }

    val adInterval = platformSettings.shortsAdInterval.coerceAtLeast(2)

    val feedItems = remember(shortsList, adInterval) {
        val items = mutableListOf<ShortsFeedItem>()
        var count = 0
        shortsList.forEach { video ->
            items.add(ShortsFeedItem.VideoItem(video))
            count++
            if (count % adInterval == 0) {
                val sponsorIndex = (count / adInterval - 1) % sampleSponsors.size
                items.add(sampleSponsors[sponsorIndex])
            }
        }
        items
    }

    if (feedItems.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = VidoPurple)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { feedItems.size })

    // Play video or record ad impression on page change
    LaunchedEffect(pagerState.currentPage, feedItems) {
        if (feedItems.isNotEmpty() && pagerState.currentPage in feedItems.indices) {
            when (val item = feedItems[pagerState.currentPage]) {
                is ShortsFeedItem.VideoItem -> {
                    activeShortVideo = item.video
                    viewModel.playerController.prepareAndPlay(item.video.videoUrl, autoPlay = true, loop = true)
                }
                is ShortsFeedItem.AdItem -> {
                    viewModel.playerController.getPlayer().pause()
                    viewModel.recordAdImpression(
                        adUnitId = AdMobManager.SHORTS_AD_UNIT_ID,
                        videoId = null,
                        adFormat = "SHORTS_FEED",
                        revenueValue = 0.024,
                        currency = "USD",
                        precision = "ESTIMATED"
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.playerController.getPlayer().pause()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("shorts_fullscreen_pager")
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (val feedItem = feedItems[page]) {
                is ShortsFeedItem.VideoItem -> {
                    val shortItem = feedItem.video
                    val channel = channelMap[shortItem.channelId]
                    val isCurrentPage = pagerState.currentPage == page

                    ShortsPageItem(
                        shortVideo = shortItem,
                        channel = channel,
                        isActive = isCurrentPage,
                        viewModel = viewModel,
                        onChannelClick = { channel?.let { onNavigateToChannel(it.id) } },
                        onCommentsClick = {
                            activeShortVideo = shortItem
                            showCommentsSheet = true
                        },
                        onShareClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "شاهد هذا المقطع القصير الممتع على VidoMix: ${shortItem.title}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة المقطع"))
                        },
                        onReportClick = { reportingShortId = shortItem.id }
                    )
                }
                is ShortsFeedItem.AdItem -> {
                    ShortsAdPageItem(
                        adItem = feedItem,
                        onCtaClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(feedItem.targetUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
        }

        // Top Brand Header Badge in Shorts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Vido",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Mix",
                        color = VidoCoral,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shorts",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Comments Bottom Sheet for Short
    if (showCommentsSheet && activeShortVideo != null) {
        CommentsBottomSheet(
            comments = comments,
            currentUser = currentUser,
            onDismiss = { showCommentsSheet = false },
            onSendComment = { content, parentId ->
                viewModel.addCommentToVideo(activeShortVideo!!.id, content, parentId)
            },
            getUserForComment = { userId -> userMap[userId] }
        )
    }

    // Report Dialog
    reportingShortId?.let { shortId ->
        ReportDialog(
            targetType = "مقطع قصير",
            onDismiss = { reportingShortId = null },
            onSubmitReport = { reason, details ->
                viewModel.submitReport("SHORT", shortId, reason, details)
                reportingShortId = null
            }
        )
    }
}

@Composable
fun ShortsAdPageItem(
    adItem: ShortsFeedItem.AdItem,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A))
    ) {
        // Background banner with dark gradient overlay
        AsyncImage(
            model = adItem.bannerUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Ad Badge at Top Right
        Surface(
            color = Color(0xFFFFB300),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                text = "إعلان ممول • Ad",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Sponsor details & CTA at Bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = adItem.sponsorLogo,
                    contentDescription = adItem.sponsorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFFFB300), CircleShape)
                )

                Column {
                    Text(
                        text = adItem.sponsorName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Google AdMob Network Sponsor",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = adItem.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            )

            Text(
                text = adItem.description,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Button(
                onClick = onCtaClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("shorts_ad_cta_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = adItem.ctaText,
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShortsPageItem(
    shortVideo: VideoEntity,
    channel: com.example.data.model.ChannelEntity?,
    isActive: Boolean,
    viewModel: VidoMixViewModel,
    onChannelClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var areControlsVisible by remember { mutableStateOf(true) }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableStateOf(shortVideo.likesCount) }
    var isSaved by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }

    // Auto disc rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
                if (!areControlsVisible) {
                    viewModel.playerController.togglePlayPause()
                }
            }
            .testTag("short_page_${shortVideo.id}")
    ) {
        if (isActive) {
            VideoPlayerView(
                controller = viewModel.playerController,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = shortVideo.thumbnailUrl,
                contentDescription = shortVideo.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay Controls & Metadata
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Right Action Bar (Vertical buttons: Like, Comment, Save, Share, Report, Sound Disc)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Like Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickableDebounced {
                                isLiked = !isLiked
                                likesCount = if (isLiked) likesCount + 1 else (likesCount - 1).coerceAtLeast(0)
                            }
                            .padding(6.dp)
                            .testTag("short_like_button_${shortVideo.id}")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "إعجاب",
                            tint = if (isLiked) VidoCoral else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatViews(likesCount),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Comments Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickableDebounced(onClick = onCommentsClick)
                            .padding(6.dp)
                            .testTag("short_comment_button_${shortVideo.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = "التعليقات",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatViews(shortVideo.commentsCount.toLong()),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Save / Bookmark Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickableDebounced {
                                isSaved = !isSaved
                                viewModel.toggleSaveVideoById(shortVideo.id)
                            }
                            .padding(6.dp)
                            .testTag("short_save_button_${shortVideo.id}")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "حفظ",
                            tint = if (isSaved) VidoCyan else Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSaved) "محفوظ" else "حفظ",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }

                    // Share Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickableDebounced(onClick = onShareClick)
                            .padding(6.dp)
                            .testTag("short_share_button_${shortVideo.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "مشاركة",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }

                    // Report Button
                    IconButton(
                        onClick = onReportClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "المزيد والتبليغ",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // Rotating Sound Vinyl Disc
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .rotate(angle)
                            .clip(CircleShape)
                            .background(Color.DarkGray)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = channel?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                // Bottom Left Channel & Video Details
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 24.dp, end = 80.dp)
                ) {
                    // Creator Info & Follow Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = channel?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                            contentDescription = channel?.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onChannelClick)
                        )

                        Text(
                            text = channel?.name ?: "VidoMix Creator",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (channel?.isVerified == true) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "موثق",
                                tint = VidoCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Title & Description
                    Text(
                        text = shortVideo.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = shortVideo.description,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Soundtrack Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = shortVideo.soundTrackTitle,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
