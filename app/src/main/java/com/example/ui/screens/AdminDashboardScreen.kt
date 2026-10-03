package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AdImpressionEntity
import com.example.data.model.AppealEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.MonetizationProfileEntity
import com.example.data.model.PayoutEntity
import com.example.data.model.ReportEntity
import com.example.data.model.UserEntity
import com.example.data.model.VideoEntity
import com.example.data.model.LiveStreamEntity
import com.example.data.model.LiveGiftEntity
import com.example.data.model.CoinPackageEntity
import com.example.ui.components.formatTimeAgo
import com.example.ui.components.formatViews
import com.example.ui.theme.VidoCoral
import com.example.ui.theme.VidoCyan
import com.example.ui.theme.VidoGold
import com.example.ui.theme.VidoPink
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: VidoMixViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val longVideos by viewModel.rawLongVideos.collectAsStateWithLifecycle()
    val shortsList by viewModel.shortsList.collectAsStateWithLifecycle()
    val reports by viewModel.allReports.collectAsStateWithLifecycle()
    val appeals by viewModel.allAppeals.collectAsStateWithLifecycle()
    val auditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()
    val monetizationProfiles by viewModel.allMonetizationProfiles.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()
    val allPayouts by viewModel.allPayouts.collectAsStateWithLifecycle()
    val flaggedImpressions by viewModel.flaggedImpressions.collectAsStateWithLifecycle()
    val recentImpressions by viewModel.recentImpressions.collectAsStateWithLifecycle()

    val activeLiveStreams by viewModel.activeLiveStreams.collectAsStateWithLifecycle()
    val allLiveStreams by viewModel.allLiveStreams.collectAsStateWithLifecycle()
    val endedLiveStreams by viewModel.endedLiveStreams.collectAsStateWithLifecycle()
    val availableGifts by viewModel.availableGifts.collectAsStateWithLifecycle()
    val availableCoinPackages by viewModel.availableCoinPackages.collectAsStateWithLifecycle()
    val allGiftTransactions by viewModel.allLiveGiftTransactions.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var liveStreamToBan by remember { mutableStateOf<LiveStreamEntity?>(null) }
    var liveBanReasonInput by remember { mutableStateOf("") }
    var showAddGiftDialog by remember { mutableStateOf(false) }
    var editingGift by remember { mutableStateOf<LiveGiftEntity?>(null) }
    var giftNameInput by remember { mutableStateOf("") }
    var giftEmojiInput by remember { mutableStateOf("🎁") }
    var giftPriceInput by remember { mutableStateOf("10") }
    var showAddPackageDialog by remember { mutableStateOf(false) }
    var pkgCoinsInput by remember { mutableStateOf("250") }
    var pkgBonusInput by remember { mutableStateOf("25") }
    var pkgTitleInput by remember { mutableStateOf("باقة جديدة") }
    var pkgPriceInput by remember { mutableStateOf("2.99") }
    var showLiveSettingsDialog by remember { mutableStateOf(false) }
    var livePlatformShareInput by remember { mutableStateOf("30") }
    var liveCreatorShareInput by remember { mutableStateOf("70") }

    var selectedTab by remember { mutableIntStateOf(0) }
    var reportFilterStatus by remember { mutableStateOf("ALL") }

    // Dialog state for confirm violation
    var reportForViolationDialog by remember { mutableStateOf<ReportEntity?>(null) }
    var violationCategoryInput by remember { mutableStateOf("") }
    var takeDownContentCheck by remember { mutableStateOf(true) }

    // Dialog state for appeal review
    var appealForReviewDialog by remember { mutableStateOf<AppealEntity?>(null) }
    var appealNotesInput by remember { mutableStateOf("") }
    var isApproveAppealAction by remember { mutableStateOf(true) }

    // Security Gate: Ensure user is authenticated and is a verified Admin
    val isAuthorizedAdmin = currentUser != null && (
        currentUser?.isAdmin == true ||
        currentUser?.email?.equals("ameadmriem@gmail.com", ignoreCase = true) == true
    )

    if (!isAuthorizedAdmin) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("غير مصرح بالدخول", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                )
            },
            modifier = modifier.testTag("admin_dashboard_unauthorized")
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "وصول مقيد (Access Denied)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "هذه اللوحة مخصصة فقط للمشرف الرئيسي المعتمد (ameadmriem@gmail.com) أو الحسابات المزودة بصلاحيات الإدارة عبر Firebase.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("العودة إلى التطبيق")
                        }
                    }
                }
            }
        }
        return
    }

    val totalVideos = longVideos.size + shortsList.size
    val totalViews = (longVideos + shortsList).sumOf { it.viewsCount }
    val pendingReports = reports.filter { it.status == "PENDING" }
    val pendingMonetization = monetizationProfiles.filter { it.status == "PENDING_REVIEW" }
    val pendingPayouts = allPayouts.filter { it.status == "PENDING" }

    // Dialog state for reject/suspend
    var actionTargetUser by remember { mutableStateOf<MonetizationProfileEntity?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "REJECT" or "SUSPEND"
    var actionReasonInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة تحكم المشرف (Admin)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("admin_dashboard_scaffold")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Analytics Overview Grid
            item {
                Text(
                    text = "📊 ملخص المنصة الحي",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        title = "المستخدمون",
                        value = users.size.toString(),
                        icon = Icons.Default.People,
                        color = VidoPurple,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "الفيديوهات وShorts",
                        value = totalVideos.toString(),
                        icon = Icons.Default.VideoLibrary,
                        color = VidoCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        title = "إجمالي المشاهدات",
                        value = formatViews(totalViews),
                        icon = Icons.Default.Visibility,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "طلبات الربح المعلقة",
                        value = pendingMonetization.size.toString(),
                        icon = Icons.Default.MonetizationOn,
                        color = Color(0xFF00C853),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Tabs Header
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 0.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("تحقيق الربح (${pendingMonetization.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("الإعلانات والأمان") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("السحوبات (${pendingPayouts.size})") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("البلاغات والمخالفات (${pendingReports.size})") }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("الاستئنافات والتدقيق") }
                    )
                    Tab(
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 },
                        text = { Text("البث المباشر والهدايا (${activeLiveStreams.size}) 🔴") }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                // TAB 0: Monetization Applications
                0 -> {
                    if (monetizationProfiles.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد ملفات تحقيق ربح مسجلة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(monetizationProfiles, key = { it.userId }) { prof ->
                            AdminMonetizationCard(
                                profile = prof,
                                onApprove = { viewModel.reviewMonetization(prof.userId, approve = true) },
                                onReject = {
                                    actionTargetUser = prof
                                    actionType = "REJECT"
                                    actionReasonInput = ""
                                },
                                onSuspend = {
                                    actionTargetUser = prof
                                    actionType = "SUSPEND"
                                    actionReasonInput = ""
                                }
                            )
                        }
                    }
                }

                // TAB 1: Ad Settings & Anti-Fraud
                1 -> {
                    // Ad Share Settings
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "⚙️ ضبط نسب مشاركة إيرادات AdMob",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("نسبة المنشئ (Creator Share): ${platformSettings.creatorSharePercent.toInt()}%", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("نسبة المنصة (Platform Share): ${platformSettings.platformSharePercent.toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Slider(
                                    value = platformSettings.creatorSharePercent.toFloat(),
                                    onValueChange = { newCreatorShare ->
                                        val creatorVal = newCreatorShare.toDouble()
                                        val platformVal = 100.0 - creatorVal
                                        viewModel.updatePlatformSettings(
                                            platformSettings.copy(
                                                creatorSharePercent = creatorVal,
                                                platformSharePercent = platformVal
                                            )
                                        )
                                    },
                                    valueRange = 10f..90f,
                                    steps = 7
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                // Shorts Ad Interval
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("تكرار إعلانات Shorts:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("إعلان بعد كل ${platformSettings.shortsAdInterval} مقاطع", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(2, 3, 4, 5).forEach { interval ->
                                            FilterChip(
                                                selected = platformSettings.shortsAdInterval == interval,
                                                onClick = {
                                                    viewModel.updatePlatformSettings(
                                                        platformSettings.copy(shortsAdInterval = interval)
                                                    )
                                                },
                                                label = { Text("$interval") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Anti-Fraud Engine Logs
                    item {
                        Text(
                            text = "🛡️ سجل نظام الحماية من الاحتيال (Anti-Fraud Engine)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (flaggedImpressions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Outlined.Security, contentDescription = null, tint = Color(0xFF00C853))
                                    Text("لا توجد مشاهدات أو نقرات مشبوهة مسجلة حالياً.", fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        items(flaggedImpressions, key = { it.id }) { imp ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "⚠️ مشاهدة مشبوهة: ${imp.status}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp
                                    )
                                    Text("معرف الإعلان: ${imp.adUnitId} • التنسيق: ${imp.adFormat}", fontSize = 11.sp)
                                    Text("المنشئ: ${imp.creatorId ?: "عام"} • المشاهد: ${imp.viewerId ?: "زائر"} • ${formatTimeAgo(imp.timestamp)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // TAB 2: Payouts
                2 -> {
                    if (allPayouts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد طلبات سحب أرباح", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(allPayouts, key = { it.id }) { payout ->
                            AdminPayoutCard(
                                payout = payout,
                                onApprove = { viewModel.updatePayoutStatus(payout, "PAID", "تم التحويل بنجاح عبر النظام المالي") },
                                onReject = { viewModel.updatePayoutStatus(payout, "REJECTED", "بيانات الحساب غير مطابقة") }
                            )
                        }
                    }
                }

                // TAB 3: Reports & Violations
                3 -> {
                    // Filter Chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = reportFilterStatus == "ALL",
                                onClick = { reportFilterStatus = "ALL" },
                                label = { Text("الكل (${reports.size})") }
                            )
                            FilterChip(
                                selected = reportFilterStatus == "PENDING",
                                onClick = { reportFilterStatus = "PENDING" },
                                label = { Text("قيد المراجعة (${reports.count { it.status == "PENDING" }})") }
                            )
                            FilterChip(
                                selected = reportFilterStatus == "CONFIRMED",
                                onClick = { reportFilterStatus = "CONFIRMED" },
                                label = { Text("مؤكدة (${reports.count { it.status == "CONFIRMED" }})") }
                            )
                            FilterChip(
                                selected = reportFilterStatus == "DISMISSED",
                                onClick = { reportFilterStatus = "DISMISSED" },
                                label = { Text("مرفوضة (${reports.count { it.status == "DISMISSED" }})") }
                            )
                        }
                    }

                    val filteredReports = when (reportFilterStatus) {
                        "PENDING" -> reports.filter { it.status == "PENDING" }
                        "CONFIRMED" -> reports.filter { it.status == "CONFIRMED" }
                        "DISMISSED" -> reports.filter { it.status == "DISMISSED" }
                        else -> reports
                    }

                    if (filteredReports.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد بلاغات تطابق التصفية الحالية 👍", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(filteredReports, key = { it.id }) { report ->
                            val creator = users.find { it.id == report.reportedUserId }
                            val reportedVideo = (longVideos + shortsList).find { it.id == report.targetId }

                            AdminReportModerationCard(
                                report = report,
                                creator = creator,
                                video = reportedVideo,
                                onConfirmViolation = {
                                    reportForViolationDialog = report
                                    violationCategoryInput = report.reason
                                    takeDownContentCheck = true
                                },
                                onDismissReport = {
                                    viewModel.dismissReport(report.id, "المحتوى لا يخالف سياسات النشر بعد الفحص اليدوي")
                                }
                            )
                        }
                    }
                }

                // TAB 4: Appeals & Audit Logs
                4 -> {
                    item {
                        Text(
                            text = "⚖️ طلبات الاستئناف المقدمة من المنشئين (${appeals.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (appeals.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد طلبات استئناف معلقة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        items(appeals, key = { it.id }) { appeal ->
                            val user = users.find { it.id == appeal.userId }
                            AdminAppealCard(
                                appeal = appeal,
                                user = user,
                                onApprove = {
                                    appealForReviewDialog = appeal
                                    isApproveAppealAction = true
                                    appealNotesInput = "تم قبول الاستئناف وخفض المخالفة بعد إعادة الفحص"
                                },
                                onReject = {
                                    appealForReviewDialog = appeal
                                    isApproveAppealAction = false
                                    appealNotesInput = "تم تثبيت القرار لمخالفته إرشادات المجتمع"
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "📜 سجل التدقيق الإداري (Audit Log)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (auditLogs.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("سجل التدقيق فارغ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        items(auditLogs, key = { it.id }) { log ->
                            AdminAuditLogCard(log = log)
                        }
                    }
                }

                // TAB 5: Live Streaming & Gifts Management
                5 -> {
                    // Live Overview Stats
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "🔴 ملخص البث المباشر والهدايا الحالية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AdminStatCard(
                                        title = "البثوث النشطة",
                                        value = activeLiveStreams.size.toString(),
                                        icon = Icons.Default.LiveTv,
                                        color = Color(0xFFE91E63),
                                        modifier = Modifier.weight(1f)
                                    )
                                    AdminStatCard(
                                        title = "المشاهدون الآن",
                                        value = activeLiveStreams.sumOf { it.viewersCount }.toString(),
                                        icon = Icons.Default.People,
                                        color = VidoPurple,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AdminStatCard(
                                        title = "إجمالي الهدايا",
                                        value = allGiftTransactions.size.toString(),
                                        icon = Icons.Default.CardGiftcard,
                                        color = VidoPink,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AdminStatCard(
                                        title = "العملات المتداولة",
                                        value = allGiftTransactions.sumOf { it.totalCoins }.toString(),
                                        icon = Icons.Default.Paid,
                                        color = VidoGold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider()

                                // Revenue Share Setting row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("نسبة المنشئ من الهدايا: ${platformSettings.liveCreatorSharePercent.toInt()}%", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("نسبة المنصة: ${platformSettings.livePlatformSharePercent.toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Button(
                                        onClick = { showLiveSettingsDialog = true },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("تعديل النسب")
                                    }
                                }
                            }
                        }
                    }

                    // Live Platform Statistics Overview
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🔴 إحصائيات البث المباشر والهدايا", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = VidoPurple.copy(alpha = 0.1f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("البثوث الجارية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${activeLiveStreams.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VidoPurple)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = VidoPink.copy(alpha = 0.1f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("الهدايا المرسلة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${allGiftTransactions.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VidoPink)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = VidoGold.copy(alpha = 0.1f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("العملات المكتسبة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${activeLiveStreams.sumOf { it.totalCoinsEarned }}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VidoGold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live Platform & Creator Share Settings Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("نسبة مشاركة أرباح البث المباشر", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "المنشئ: ${platformSettings.liveCreatorSharePercent.toInt()}% | المنصة: ${platformSettings.livePlatformSharePercent.toInt()}%",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        livePlatformShareInput = platformSettings.livePlatformSharePercent.toInt().toString()
                                        liveCreatorShareInput = platformSettings.liveCreatorSharePercent.toInt().toString()
                                        showLiveSettingsDialog = true
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VidoPurple)
                                ) {
                                    Text("تعديل النسب ⚙️", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Active Live Streams Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "البثوث المباشرة الجارية الآن (${activeLiveStreams.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (activeLiveStreams.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد بثوث مباشرة جارية في الوقت الحالي", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        items(activeLiveStreams, key = { it.id }) { stream ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            AsyncImage(
                                                model = stream.hostAvatarUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column {
                                                Text(stream.hostFullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("@${stream.hostUsername}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFE91E63)
                                        ) {
                                            Text(
                                                text = "🔴 مباشر (${stream.viewersCount} مشاهد)",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(stream.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("❤️ ${stream.likesCount} إعجاب", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("🎁 ${stream.totalGiftsCount} هدية", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("🪙 ${stream.totalCoinsEarned} عملة", fontSize = 12.sp, color = VidoGold, fontWeight = FontWeight.Bold)
                                    }

                                    HorizontalDivider()

                                    // Admin Action Buttons: End Stream & Ban Stream
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                scope.launch { viewModel.endLiveStream(stream.id) }
                                            }
                                        ) {
                                            Text("إنهاء البث")
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                liveStreamToBan = stream
                                                liveBanReasonInput = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text("حظر البث 🚫")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Gifts Management Section
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎁 إدارة الهدايا (${availableGifts.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Button(
                                onClick = { showAddGiftDialog = true },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("+ إضافة هدية")
                            }
                        }
                    }

                    items(availableGifts, key = { it.id }) { gift ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(gift.emojiIcon, fontSize = 28.sp)
                                    Column {
                                        Text(gift.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("🪙 ${gift.coinPrice} عملة", fontSize = 12.sp, color = VidoGold, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            editingGift = gift
                                            giftNameInput = gift.name
                                            giftEmojiInput = gift.emojiIcon
                                            giftPriceInput = gift.coinPrice.toString()
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = VidoPurple, modifier = Modifier.size(20.dp))
                                    }
                                    Switch(
                                        checked = gift.isEnabled,
                                        onCheckedChange = {
                                            scope.launch {
                                                viewModel.adminSaveGift(gift.copy(isEnabled = it))
                                            }
                                        }
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch { viewModel.adminDeleteGift(gift.id) }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Coin Packages Management Section
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🪙 إدارة باقات العملات (${availableCoinPackages.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Button(
                                onClick = { showAddPackageDialog = true },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("+ إضافة باقة")
                            }
                        }
                    }

                    items(availableCoinPackages, key = { it.id }) { pkg ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${pkg.coinsAmount} عملة (${pkg.title})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("السعر: $${pkg.priceUsd} (مكافأة: +${pkg.bonusCoins})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { scope.launch { viewModel.adminDeleteCoinPackage(pkg.id) } }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // Ended Live Streams Archive
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "سجل البثوث المنتهية والأرشيف (${endedLiveStreams.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    if (endedLiveStreams.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد بثوث منتهية بعد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        items(endedLiveStreams, key = { it.id }) { endedStream ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(endedStream.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("المضيف: ${endedStream.hostFullName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("ذروة المشاهدين: ${endedStream.peakViewers} | الهدايا: ${endedStream.totalGiftsCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("🪙 ${endedStream.totalCoinsEarned}", fontWeight = FontWeight.Bold, color = VidoGold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reject or Suspend Reason Dialog
    if (actionTargetUser != null && actionType != null) {
        val target = actionTargetUser!!
        val isReject = actionType == "REJECT"

        AlertDialog(
            onDismissRequest = {
                actionTargetUser = null
                actionType = null
            },
            title = {
                Text(
                    text = if (isReject) "رفض طلب تحقيق الربح" else "تعليق تحقيق الربح للحساب",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المستخدم: @${target.userId}", fontSize = 13.sp)
                    OutlinedTextField(
                        value = actionReasonInput,
                        onValueChange = { actionReasonInput = it },
                        label = { Text("سبب الإجراء الموجه للمنشئ") },
                        placeholder = { Text(if (isReject) "مثال: المحتوى يحتوي على مقاطع منسوخة" else "مثال: تم رصد مشاهدات غير طبيعية") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = actionReasonInput.ifBlank { if (isReject) "مخالفة سياسة المحتوى" else "نشاط مشبوه" }
                        if (isReject) {
                            viewModel.reviewMonetization(target.userId, approve = false, reason = reason)
                        } else {
                            viewModel.suspendMonetization(target.userId, reason = reason)
                        }
                        actionTargetUser = null
                        actionType = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الإجراء")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    actionTargetUser = null
                    actionType = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Confirm Violation Dialog (Issues official strike + takes down content + sends notice)
    if (reportForViolationDialog != null) {
        val report = reportForViolationDialog!!
        val creator = users.find { it.id == report.reportedUserId }
        val currentStrikes = creator?.warningCount ?: 0
        val nextStrike = (currentStrikes + 1).coerceAtMost(5)

        AlertDialog(
            onDismissRequest = { reportForViolationDialog = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("تأكيد تسجيل مخالفة رسمية", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⚠️ سيتم رفع عدد المخالفات للمنشئ من ($currentStrikes/5) إلى ($nextStrike/5)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                            if (nextStrike >= 5) {
                                Text(
                                    text = "🚨 تحذير: بلوغ 5 مخالفات سيؤدي إلى إنهاء وتعليق الحساب تلقائياً (TERMINATED).",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text("المنشئ المعني: @${creator?.username ?: report.reportedUserId}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = violationCategoryInput,
                        onValueChange = { violationCategoryInput = it },
                        label = { Text("نوع المخالفة المعتمد") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = takeDownContentCheck,
                            onCheckedChange = { takeDownContentCheck = it }
                        )
                        Text("إزالة/حجب المحتوى المخالف فوراً من المنصة", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmReportViolation(
                            reportId = report.id,
                            violationCategory = violationCategoryInput.ifBlank { report.reason },
                            takeDownContent = takeDownContentCheck
                        )
                        reportForViolationDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("✓ تأكيد المخالفة وفرض التحذير")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportForViolationDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Appeal Review Dialog
    if (appealForReviewDialog != null) {
        val appeal = appealForReviewDialog!!
        val isApprove = isApproveAppealAction

        AlertDialog(
            onDismissRequest = { appealForReviewDialog = null },
            title = {
                Text(
                    text = if (isApprove) "قبول طلب الاستئناف وخفض المخالفة" else "رفض الاستئناف وتثبيت المخالفة",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("المستخدم: @${appeal.userId} • المخالفة رقم: ${appeal.strikeNumber}", fontSize = 13.sp)
                    Text("حجة المنشئ: ${appeal.reason}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = appealNotesInput,
                        onValueChange = { appealNotesInput = it },
                        label = { Text("ملاحظات المشرف الموجهة للمنشئ") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reviewAppeal(
                            appealId = appeal.id,
                            isApproved = isApprove,
                            reviewNotes = appealNotesInput
                        )
                        appealForReviewDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApprove) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isApprove) "تأكيد قبول الاستئناف ✓" else "تأكيد رفض الاستئناف ✕")
                }
            },
            dismissButton = {
                TextButton(onClick = { appealForReviewDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: Add Gift
    if (showAddGiftDialog) {
        AlertDialog(
            onDismissRequest = { showAddGiftDialog = false },
            title = { Text("إضافة هدية بث جديدة 🎁", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = giftNameInput,
                        onValueChange = { giftNameInput = it },
                        label = { Text("اسم الهدية (مثال: تاج ذهبي)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = giftEmojiInput,
                        onValueChange = { giftEmojiInput = it },
                        label = { Text("رمز أو إيموجي الهدية (مثال: 👑)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = giftPriceInput,
                        onValueChange = { giftPriceInput = it },
                        label = { Text("سعر الهدية بالعملات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = giftPriceInput.toLongOrNull() ?: 10L
                        val newGift = LiveGiftEntity(
                            id = "gift_" + java.util.UUID.randomUUID().toString().take(8),
                            name = giftNameInput.ifBlank { "هدية مميزة" },
                            emojiIcon = giftEmojiInput.ifBlank { "🎁" },
                            animationType = "CROWN_ROYAL",
                            coinPrice = price,
                            isEnabled = true,
                            displayOrder = availableGifts.size + 1
                        )
                        scope.launch {
                            viewModel.adminSaveGift(newGift)
                            showAddGiftDialog = false
                            giftNameInput = ""
                            giftEmojiInput = "🎁"
                            giftPriceInput = "10"
                        }
                    }
                ) {
                    Text("إضافة الهدية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGiftDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: Edit Gift
    editingGift?.let { gift ->
        AlertDialog(
            onDismissRequest = { editingGift = null },
            title = { Text("تعديل الهدية (${gift.name})", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = giftNameInput,
                        onValueChange = { giftNameInput = it },
                        label = { Text("اسم الهدية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = giftEmojiInput,
                        onValueChange = { giftEmojiInput = it },
                        label = { Text("رمز أو إيموجي الهدية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = giftPriceInput,
                        onValueChange = { giftPriceInput = it },
                        label = { Text("السعر بالعملات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = giftPriceInput.toLongOrNull() ?: gift.coinPrice
                        val updated = gift.copy(
                            name = giftNameInput.ifBlank { gift.name },
                            emojiIcon = giftEmojiInput.ifBlank { gift.emojiIcon },
                            coinPrice = price
                        )
                        scope.launch {
                            viewModel.adminSaveGift(updated)
                            editingGift = null
                        }
                    }
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGift = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: Add Coin Package
    if (showAddPackageDialog) {
        AlertDialog(
            onDismissRequest = { showAddPackageDialog = false },
            title = { Text("إضافة باقة شحن عملات 🪙", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pkgTitleInput,
                        onValueChange = { pkgTitleInput = it },
                        label = { Text("اسم الباقة (مثال: باقة النجوم)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pkgCoinsInput,
                        onValueChange = { pkgCoinsInput = it },
                        label = { Text("عدد العملات الأساسي") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pkgBonusInput,
                        onValueChange = { pkgBonusInput = it },
                        label = { Text("العملات الإضافية (Bonus)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pkgPriceInput,
                        onValueChange = { pkgPriceInput = it },
                        label = { Text("السعر بالدولار ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val coins = pkgCoinsInput.toLongOrNull() ?: 100L
                        val bonus = pkgBonusInput.toLongOrNull() ?: 0L
                        val price = pkgPriceInput.toDoubleOrNull() ?: 0.99
                        val newPkg = CoinPackageEntity(
                            id = "pkg_" + java.util.UUID.randomUUID().toString().take(8),
                            coinsAmount = coins,
                            bonusCoins = bonus,
                            priceUsd = price,
                            title = pkgTitleInput.ifBlank { "باقة عملات" },
                            isPopular = false,
                            isEnabled = true
                        )
                        scope.launch {
                            viewModel.adminSaveCoinPackage(newPkg)
                            showAddPackageDialog = false
                        }
                    }
                ) {
                    Text("إضافة الباقة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPackageDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: Ban Live Stream
    liveStreamToBan?.let { stream ->
        AlertDialog(
            onDismissRequest = { liveStreamToBan = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("حظر وإنهاء البث المباشر", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("البث: ${stream.title}")
                    Text("المضيف: @${stream.hostUsername} (${stream.hostFullName})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = liveBanReasonInput,
                        onValueChange = { liveBanReasonInput = it },
                        label = { Text("سبب حظر البث") },
                        placeholder = { Text("مثال: انتهاك إرشادات المجتمع / محتوى غير لائق") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = liveBanReasonInput.ifBlank { "مخالفة إرشادات البث المباشر" }
                        scope.launch {
                            viewModel.adminBanLiveStream(stream.id, reason)
                            liveStreamToBan = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الحظر والإنهاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { liveStreamToBan = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: Live Revenue Share Settings
    if (showLiveSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showLiveSettingsDialog = false },
            title = { Text("نسب توزيع أرباح البث المباشر 💰", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("حدد نسبة المنشئ ونسبة المنصة من قيمة الهدايا:")
                    OutlinedTextField(
                        value = liveCreatorShareInput,
                        onValueChange = {
                            liveCreatorShareInput = it
                            val cr = it.toDoubleOrNull() ?: 70.0
                            livePlatformShareInput = (100.0 - cr).coerceIn(0.0, 100.0).toInt().toString()
                        },
                        label = { Text("نسبة صاحب البث (%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = livePlatformShareInput,
                        onValueChange = {
                            livePlatformShareInput = it
                            val pl = it.toDoubleOrNull() ?: 30.0
                            liveCreatorShareInput = (100.0 - pl).coerceIn(0.0, 100.0).toInt().toString()
                        },
                        label = { Text("نسبة المنصة (%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val creator = liveCreatorShareInput.toDoubleOrNull() ?: 70.0
                        val platform = livePlatformShareInput.toDoubleOrNull() ?: 30.0
                        scope.launch {
                            viewModel.adminUpdateLiveRevenueShare(platform, creator)
                            showLiveSettingsDialog = false
                        }
                    }
                ) {
                    Text("حفظ النسب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiveSettingsDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun AdminMonetizationCard(
    profile: MonetizationProfileEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onSuspend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (profile.status) {
                "PENDING_REVIEW" -> Color(0xFFFFF8E1)
                "APPROVED" -> MaterialTheme.colorScheme.surface
                "SUSPENDED" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الحساب: ${profile.userId}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Surface(
                    color = when (profile.status) {
                        "APPROVED" -> Color(0xFF00C853).copy(alpha = 0.15f)
                        "PENDING_REVIEW" -> Color(0xFFF57C00).copy(alpha = 0.15f)
                        "SUSPENDED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = profile.status,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = when (profile.status) {
                            "APPROVED" -> Color(0xFF00C853)
                            "PENDING_REVIEW" -> Color(0xFFF57C00)
                            "SUSPENDED" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("المتابعون: ${formatViews(profile.followersCount.toLong())}", fontSize = 12.sp)
                Text("ساعات المشاهدة: ${String.format("%.1f", profile.watchHours)}", fontSize = 12.sp)
                Text("مشاهدات Shorts: ${formatViews(profile.shortsViews)}", fontSize = 12.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("إجمالي الأرباح: $${String.format("%.2f", profile.lifetimeEarnings)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                Text("الرصيد المتاح: $${String.format("%.2f", profile.currentBalance)}", fontSize = 12.sp)
            }

            if (profile.rejectionReason.isNotBlank()) {
                Text("سبب الرفض/التعليق: ${profile.rejectionReason}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (profile.status == "PENDING_REVIEW") {
                    TextButton(onClick = onReject) {
                        Text("رفض الطلب", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text("اعتماد الشريك ✓", fontSize = 12.sp)
                    }
                } else if (profile.status == "APPROVED") {
                    OutlinedButton(onClick = onSuspend) {
                        Text("تعليق الأرباح ⚠️", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                } else if (profile.status == "SUSPENDED") {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text("إعادة التفعيل ✓", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPayoutCard(
    payout: PayoutEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "طلب سحب: $${String.format("%.2f", payout.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF00C853)
                )
                Text("الحالة: ${payout.status}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text("المستخدم: ${payout.userId} • الطريقة: ${payout.method}", fontSize = 12.sp)
            Text("تفاصيل الحساب: ${payout.payoutDetails}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Text("تاريخ الطلب: ${formatTimeAgo(payout.requestedAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (payout.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onReject) {
                        Text("رفض الطلب", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text("اعتماد الدفع ✓", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }

            Column {
                Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AdminReportModerationCard(
    report: ReportEntity,
    creator: UserEntity?,
    video: VideoEntity?,
    onConfirmViolation: () -> Unit,
    onDismissReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (report.status == "PENDING") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️ بلاغ عن: ${report.reportType}",
                    fontWeight = FontWeight.Bold,
                    color = VidoCoral,
                    fontSize = 13.sp
                )
                Surface(
                    color = when (report.status) {
                        "PENDING" -> Color(0xFFF57C00).copy(alpha = 0.15f)
                        "CONFIRMED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        "DISMISSED" -> Color(0xFF00C853).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = report.status,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = when (report.status) {
                            "PENDING" -> Color(0xFFF57C00)
                            "CONFIRMED" -> MaterialTheme.colorScheme.error
                            "DISMISSED" -> Color(0xFF00C853)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "السبب: ${report.reason}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            if (report.details.isNotBlank()) {
                Text(
                    text = "شرح البلاغ: ${report.details}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (creator != null) {
                Text(
                    text = "صاحب المحتوى: @${creator.username} • إنذارات سابقة: (${creator.warningCount} من 5)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (video != null) {
                Text(
                    text = "عنوان المحتوى: ${video.title}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = formatTimeAgo(report.timestamp),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (report.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissReport) {
                        Text("رفض البلاغ (تجاهل)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onConfirmViolation,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("تأكيد المخالفة وفرض عقوبة", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAppealCard(
    appeal: AppealEntity,
    user: UserEntity?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appeal.status == "PENDING") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚖️ استئناف المخالفة (${appeal.strikeNumber} من 5)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = VidoPurple
                )

                Surface(
                    color = when (appeal.status) {
                        "PENDING" -> Color(0xFFF57C00).copy(alpha = 0.15f)
                        "APPROVED" -> Color(0xFF00C853).copy(alpha = 0.15f)
                        "REJECTED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = appeal.status,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = when (appeal.status) {
                            "PENDING" -> Color(0xFFF57C00)
                            "APPROVED" -> Color(0xFF00C853)
                            "REJECTED" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "المستخدم: @${user?.username ?: appeal.userId}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "حجة الاستئناف: ${appeal.reason}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (appeal.additionalInfo.isNotBlank()) {
                Text(
                    text = "معلومات إضافية: ${appeal.additionalInfo}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!appeal.reviewNotes.isNullOrBlank()) {
                Text(
                    text = "قرار المشرف: ${appeal.reviewNotes}",
                    fontSize = 11.sp,
                    color = if (appeal.status == "APPROVED") Color(0xFF00C853) else MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = formatTimeAgo(appeal.timestamp),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (appeal.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onReject) {
                        Text("رفض الاستئناف", color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text("قبول وإلغاء الإنذار ✓", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAuditLogCard(
    log: AuditLogEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 ${log.action}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = when {
                        log.action.contains("CONFIRM") || log.action.contains("BAN") -> MaterialTheme.colorScheme.error
                        log.action.contains("APPROVE") || log.action.contains("DISMISS") -> Color(0xFF00C853)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = formatTimeAgo(log.timestamp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "المشرف: ${log.adminId} • الهدف: ${log.targetUserId ?: log.contentId ?: "N/A"}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (log.reason.isNotBlank()) {
                Text(
                    text = "السبب/الملاحظات: ${log.reason}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ReportCardItem(
    report: ReportEntity,
    onResolve: () -> Unit,
    onDismiss: () -> Unit,
    onDeleteTarget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (report.status == "PENDING") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "بلاغ عن ${report.reportType}",
                    fontWeight = FontWeight.Bold,
                    color = VidoCoral,
                    fontSize = 13.sp
                )
                Text(
                    text = "الحالة: ${report.status}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "السبب: ${report.reason}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            if (report.details.isNotBlank()) {
                Text(
                    text = "التفاصيل: ${report.details}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatTimeAgo(report.timestamp),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (report.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("تجاهل")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onDeleteTarget,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("حذف المحتوى المخالف", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
