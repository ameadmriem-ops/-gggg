package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ChannelEntity
import com.example.data.model.VideoEntity
import com.example.ui.components.*
import com.example.ui.theme.VidoCoral
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: VidoMixViewModel,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToShorts: () -> Unit,
    onNavigateToChannel: (String) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToLive: (String) -> Unit = {},
    onStartLiveClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val homeVideos by viewModel.recommendedHomeVideos.collectAsStateWithLifecycle()
    val trendingVideos by viewModel.trendingVideos.collectAsStateWithLifecycle()
    val shortsList by viewModel.shortsList.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val activeLiveStreams by viewModel.activeLiveStreams.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val savedVideos by viewModel.savedVideos.collectAsStateWithLifecycle()

    val channelMap = remember(channels) { channels.associateBy { it.id } }
    val savedVideoIds = remember(savedVideos) { savedVideos.map { it.id }.toSet() }
    val hasUnread = notifications.any { !it.isRead }

    val filteredLiveStreams = remember(activeLiveStreams, selectedCategory) {
        if (selectedCategory == "الكل") activeLiveStreams
        else activeLiveStreams.filter { it.category == selectedCategory }
    }

    var reportingVideoId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            VidoMixTopBar(
                onSearchClick = onSearchClick,
                onNotificationsClick = onNotificationsClick,
                userAvatarUrl = currentUser?.avatarUrl,
                onAvatarClick = onProfileClick,
                hasUnreadNotifications = hasUnread,
                onLiveClick = onStartLiveClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag("home_screen_scaffold")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_feed_list"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Filter Chips
            item {
                CategoryFilterChips(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.selectedCategory.value = it }
                )
            }

            // Live Streams Carousel Shelf (مباشر الآن)
            if (filteredLiveStreams.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE91E63))
                                )
                                Text(
                                    text = "مباشر الآن (LIVE)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                            TextButton(onClick = onStartLiveClick) {
                                Text("بدء بث +", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }

                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredLiveStreams, key = { it.id }) { stream ->
                                LiveStreamCard(
                                    stream = stream,
                                    onClick = { onNavigateToLive(stream.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Featured Hero Banner (Top Trending Video)
            if (trendingVideos.isNotEmpty()) {
                val hero = trendingVideos.first()
                val heroChannel = channelMap[hero.channelId]
                item {
                    FeaturedHeroCard(
                        video = hero,
                        channel = heroChannel,
                        onClick = { onNavigateToVideo(hero.id) }
                    )
                }
            }

            // Shorts Shelf Section
            if (shortsList.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = VidoCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "مقاطع VidoMix القصيرة (Shorts)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            TextButton(onClick = onNavigateToShorts) {
                                Text("عرض الكل", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(shortsList.take(6)) { shortVideo ->
                                ShortThumbnailCard(
                                    video = shortVideo,
                                    onClick = onNavigateToShorts
                                )
                            }
                        }
                    }
                }
            }

            // Recommended Feed Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = VidoPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (selectedCategory == "الكل") "فيديوهات مقترحة لك" else "فيديوهات في $selectedCategory",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Recommended Videos Feed
            items(homeVideos, key = { it.id }) { video ->
                val channel = channelMap[video.channelId]
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    VideoCard(
                        video = video,
                        channel = channel,
                        onVideoClick = onNavigateToVideo,
                        onChannelClick = onNavigateToChannel,
                        onSaveToggle = {
                            viewModel.toggleSaveVideoById(it)
                            scope.launch { snackbarHostState.showSnackbar(if (savedVideoIds.contains(video.id)) "تمت إزالة الفيديو من المحفوظات" else "تم حفظ الفيديو في المحفوظات") }
                        },
                        onShareClick = { /* Handled in VideoCard */ },
                        onReportClick = { reportingVideoId = it },
                        onNotInterested = {
                            viewModel.markVideoNotInterested(video) { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onBlockCreator = { creatorId ->
                            viewModel.blockCreator(creatorId) { _, msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onDislikeCategory = { cat ->
                            viewModel.dislikeCategory(cat) { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onDownload = {
                            scope.launch { snackbarHostState.showSnackbar("جاري بدء تنزيل الفيديو للمشاهدة بدون إنترنت...") }
                        },
                        onEditVideo = { t, d, c, tg ->
                            viewModel.editVideoDetails(video.id, t, d, c, tg) { _, _ ->
                                scope.launch { snackbarHostState.showSnackbar("تم حفظ التعديلات بنجاح") }
                            }
                        },
                        onToggleVisibility = { pub ->
                            viewModel.toggleVideoVisibility(video.id, pub) { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onDeleteVideo = {
                            viewModel.deleteVideo(video.id)
                            scope.launch { snackbarHostState.showSnackbar("تم حذف الفيديو بنجاح") }
                        },
                        currentUser = currentUser,
                        isSaved = savedVideoIds.contains(video.id)
                    )
                }
            }
        }
    }

    // Report Dialog
    reportingVideoId?.let { videoId ->
        val video = homeVideos.find { it.id == videoId }
        ReportContentDialog(
            targetTitle = video?.title ?: "فيديو",
            reportType = "VIDEO",
            onDismissRequest = { reportingVideoId = null },
            onSubmitReport = { reason, details ->
                viewModel.submitReport("VIDEO", videoId, reason, details)
                reportingVideoId = null
                scope.launch { snackbarHostState.showSnackbar("تم إرسال البلاغ بنجاح إلى فريق الإشراف") }
            }
        )
    }
}

@Composable
fun FeaturedHeroCard(
    video: VideoEntity,
    channel: ChannelEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("featured_hero_card"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(VidoPurple)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "مميز اليوم ★",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Info at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${channel?.name ?: "VidoMix"} • ${formatViews(video.viewsCount)} مشاهدة",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(VidoCoral),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
