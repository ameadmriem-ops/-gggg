package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CategoryFilterChips
import com.example.ui.components.ReportDialog
import com.example.ui.components.VideoCard
import com.example.ui.viewmodel.VidoMixViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    viewModel: VidoMixViewModel,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToChannel: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val longVideos by viewModel.rawLongVideos.collectAsStateWithLifecycle()
    val trendingVideos by viewModel.trendingVideos.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val savedVideos by viewModel.savedVideos.collectAsStateWithLifecycle()

    val channelMap = remember(channels) { channels.associateBy { it.id } }
    val savedVideoIds = remember(savedVideos) { savedVideos.map { it.id }.toSet() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var reportingVideoId by remember { mutableStateOf<String?>(null) }

    val filteredVideos = remember(longVideos, trendingVideos, selectedTab, selectedCategory) {
        val baseList = when (selectedTab) {
            0 -> longVideos
            1 -> trendingVideos
            else -> longVideos.sortedByDescending { it.uploadTimestamp }
        }
        if (selectedCategory == "الكل") baseList
        else baseList.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مكتبة الفيديوهات",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.testTag("videos_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "بحث")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("videos_screen_scaffold")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sort Tabs: المقترحة, الرائجة, الأحدث
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("المقترحة", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("الرائجة 🔥", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("الأحدث ⏰", fontWeight = FontWeight.Bold) }
                )
            }

            // Category Filter Chips
            CategoryFilterChips(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            // Video Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("long_videos_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredVideos, key = { it.id }) { video ->
                    val channel = channelMap[video.channelId]
                    VideoCard(
                        video = video,
                        channel = channel,
                        onVideoClick = onNavigateToVideo,
                        onChannelClick = onNavigateToChannel,
                        onSaveToggle = { viewModel.toggleSaveVideoById(it) },
                        onShareClick = { /* Handle share */ },
                        onReportClick = { reportingVideoId = it },
                        isSaved = savedVideoIds.contains(video.id)
                    )
                }
            }
        }
    }

    // Report Dialog
    reportingVideoId?.let { videoId ->
        ReportDialog(
            targetType = "فيديو",
            onDismiss = { reportingVideoId = null },
            onSubmitReport = { reason, details ->
                viewModel.submitReport("VIDEO", videoId, reason, details)
                reportingVideoId = null
            }
        )
    }
}
