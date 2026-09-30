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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MonetizationProfileEntity
import com.example.ui.components.formatViews
import com.example.ui.theme.VidoCoral
import com.example.ui.theme.VidoCyan
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel

enum class AnalyticsPeriod(val label: String, val factor: Double) {
    TODAY("اليوم", 0.08),
    DAYS_7("7 أيام", 0.25),
    DAYS_28("28 يوماً", 0.60),
    DAYS_90("90 يوماً", 0.85),
    LIFETIME("مدى الحياة", 1.0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorStudioScreen(
    viewModel: VidoMixViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToMonetization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val monetizationProfile by viewModel.currentUserMonetization.collectAsStateWithLifecycle()
    val allVideos by viewModel.rawLongVideos.collectAsStateWithLifecycle()
    val allShorts by viewModel.shortsList.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.DAYS_28) }

    val user = currentUser
    val profile = monetizationProfile ?: MonetizationProfileEntity(
        userId = user?.id ?: "",
        channelId = "chan_${user?.id ?: ""}",
        followersCount = user?.followersCount ?: 0,
        currentBalance = 0.0,
        lifetimeEarnings = 0.0
    )

    val channelMap = remember(channels) { channels.associateBy { it.id } }
    val userVideos = remember(allVideos, allShorts, user) {
        (allVideos + allShorts).filter { vid ->
            val chan = channelMap[vid.channelId]
            chan?.userId == user?.id
        }
    }

    val totalRawViews = remember(userVideos) { userVideos.sumOf { it.viewsCount } }
    val totalLikes = remember(userVideos) { userVideos.sumOf { it.likesCount } }
    val totalComments = remember(userVideos) { userVideos.sumOf { it.commentsCount } }

    val scaledViews = (totalRawViews * selectedPeriod.factor).toLong().coerceAtLeast(1)
    val scaledWatchHours = (profile.watchHours * selectedPeriod.factor).coerceAtLeast(0.5)
    val scaledLikes = (totalLikes * selectedPeriod.factor).toLong()
    val scaledComments = (totalComments * selectedPeriod.factor).toLong()
    val scaledShares = (scaledViews * 0.04).toLong()
    val scaledImpressions = (profile.adImpressionsCount * selectedPeriod.factor).toLong()
    val scaledRevenue = profile.lifetimeEarnings * selectedPeriod.factor

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استوديو المنشئين (Creator Studio)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToPayments) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "المحفظة والسحب")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("creator_studio_scaffold")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Selector Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AnalyticsPeriod.values()) { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.label) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Earnings Card Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00C853).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF00C853),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = "💰 قسم الأرباح المقدرة (${selectedPeriod.label})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            if (profile.status != "APPROVED") {
                                TextButton(onClick = onNavigateToMonetization) {
                                    Text("الأهلية ❯", fontSize = 12.sp)
                                }
                            }
                        }

                        // Grid of earnings figures
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("اليوم", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$${String.format("%.2f", profile.lifetimeEarnings * 0.04)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Column {
                                Text("هذا الشهر", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$${String.format("%.2f", profile.lifetimeEarnings * 0.45)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Column {
                                Text("إجمالي الأرباح", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$${String.format("%.2f", profile.lifetimeEarnings)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF00C853)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("مشاهدات الإعلانات (Impressions)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatViews(scaledImpressions),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Column {
                                Text("الإعلانات المؤهلة للربح", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatViews((profile.monetizedPlaybacksCount * selectedPeriod.factor).toLong()),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        if (profile.status == "APPROVED") {
                            Button(
                                onClick = onNavigateToPayments,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("سحب الأرباح ($${String.format("%.2f", profile.currentBalance)} متاح)")
                            }
                        }
                    }
                }
            }

            // Analytics Key Performance Metrics
            item {
                Text(
                    text = "📊 إحصائيات الأداء والتفاعل (Analytics)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudioMetricCard(
                        title = "المشاهدات (Views)",
                        value = formatViews(scaledViews),
                        icon = Icons.Default.Visibility,
                        color = VidoPurple,
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricCard(
                        title = "وقت المشاهدة (Watch Time)",
                        value = "${String.format("%.1f", scaledWatchHours)} س",
                        icon = Icons.Default.Schedule,
                        color = VidoCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudioMetricCard(
                        title = "المتابعون (Followers)",
                        value = formatViews(profile.followersCount.toLong()),
                        icon = Icons.Default.Group,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricCard(
                        title = "الإعجابات (Likes)",
                        value = formatViews(scaledLikes),
                        icon = Icons.Default.Favorite,
                        color = VidoCoral,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudioMetricCard(
                        title = "التعليقات (Comments)",
                        value = formatViews(scaledComments),
                        icon = Icons.Default.Comment,
                        color = Color(0xFFFFB300),
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricCard(
                        title = "مشاركات الفيديو (Shares)",
                        value = formatViews(scaledShares),
                        icon = Icons.Default.Share,
                        color = Color(0xFF00B0FF),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Content Performance Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ملخص المحتوى المنشور",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي الفيديوهات والمقاطع: ${userVideos.size}", fontSize = 13.sp)
                            Text("الفيديوهات العامة: ${userVideos.count { it.isPublic }}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudioMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
