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
import com.example.data.model.MonetizationProfileEntity
import com.example.data.model.PayoutEntity
import com.example.data.model.ReportEntity
import com.example.ui.components.formatTimeAgo
import com.example.ui.components.formatViews
import com.example.ui.theme.VidoCoral
import com.example.ui.theme.VidoCyan
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel

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
    val monetizationProfiles by viewModel.allMonetizationProfiles.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()
    val allPayouts by viewModel.allPayouts.collectAsStateWithLifecycle()
    val flaggedImpressions by viewModel.flaggedImpressions.collectAsStateWithLifecycle()
    val recentImpressions by viewModel.recentImpressions.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

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
                        text = { Text("البلاغات (${pendingReports.size})") }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("المحتوى") }
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

                // TAB 3: Reports
                3 -> {
                    if (reports.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد بلاغات معلقة 👍", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(reports, key = { it.id }) { report ->
                            ReportCardItem(
                                report = report,
                                onResolve = { viewModel.updateReportStatus(report.id, "RESOLVED") },
                                onDismiss = { viewModel.updateReportStatus(report.id, "DISMISSED") },
                                onDeleteTarget = {
                                    if (report.reportType == "VIDEO" || report.reportType == "SHORT") {
                                        viewModel.deleteVideo(report.targetId)
                                    }
                                    viewModel.updateReportStatus(report.id, "RESOLVED")
                                }
                            )
                        }
                    }
                }

                // TAB 4: Content Moderation
                4 -> {
                    val allList = longVideos + shortsList
                    items(allList, key = { it.id }) { video ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = video.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(video.title, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 13.sp)
                                    Text("${video.category} • ${if (video.isShort) "Short" else "Video"} • ${formatViews(video.viewsCount)} مشاهدة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteVideo(video.id) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الفيديو")
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
