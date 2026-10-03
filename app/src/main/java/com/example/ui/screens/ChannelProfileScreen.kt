package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.ShortThumbnailCard
import com.example.ui.components.VideoCard
import com.example.ui.components.formatViews
import com.example.ui.theme.VidoCyan
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChannelProfileScreen(
    channelId: String,
    viewModel: VidoMixViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToShorts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val allVideos by viewModel.rawLongVideos.collectAsStateWithLifecycle()
    val allShorts by viewModel.shortsList.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val channel = remember(channels, channelId) { channels.find { it.id == channelId } }
    val channelVideos = remember(allVideos, channelId) { allVideos.filter { it.channelId == channelId } }
    val channelShorts = remember(allShorts, channelId) { allShorts.filter { it.channelId == channelId } }

    var isFollowing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(channelId, currentUser) {
        currentUser?.let { user ->
            viewModel.repository.isFollowing(user.id, channelId).collect {
                isFollowing = it
            }
        }
    }

    if (channel == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(channel.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("channel_profile_scaffold")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Channel Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = channel.bannerUrl,
                        contentDescription = "غلاف القناة",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Channel Header: Avatar, Name, Handle, Bio, Stats, Subscribe
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar (offset slightly into banner)
                    AsyncImage(
                        model = channel.avatarUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .offset(y = (-40).dp)
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )

                    Spacer(modifier = Modifier.height((-30).dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (channel.isVerified) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "موثق",
                                tint = VidoCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "${channel.handle} • ${formatViews(channel.subscriberCount.toLong())} مشترك • ${channel.videoCount} فيديو",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = channel.bio,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subscribe / Follow Button
                    Button(
                        onClick = {
                            viewModel.toggleFollowChannel(channel.id)
                            isFollowing = !isFollowing
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(44.dp)
                            .testTag("channel_subscribe_button")
                    ) {
                        Text(
                            text = if (isFollowing) "مشترك بالفعل ✓" else "اشتراك في القناة +",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tabs: Videos, Shorts, About
            item {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("الفيديوهات (${channelVideos.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("المقاطع القصيرة (${channelShorts.size})") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("حول القناة") }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    if (channelVideos.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد فيديوهات منشورة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(channelVideos, key = { it.id }) { video ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                VideoCard(
                                    video = video,
                                    channel = channel,
                                    currentUser = currentUser,
                                    onVideoClick = onNavigateToVideo,
                                    onChannelClick = {}
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (channelShorts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد مقاطع قصيرة منشورة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        item {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                channelShorts.forEach { shortItem ->
                                    ShortThumbnailCard(
                                        video = shortItem,
                                        channel = channel,
                                        currentUser = currentUser,
                                        onClick = onNavigateToShorts
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("الوصف:", fontWeight = FontWeight.Bold)
                                Text(channel.bio, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("تاريخ الانضمام: 2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("الموقع: الشرق الأوسط والعالم العربي", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
