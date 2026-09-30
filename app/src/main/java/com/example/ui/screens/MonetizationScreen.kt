package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonetizationScreen(
    viewModel: VidoMixViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onNavigateToPayments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val monetizationProfile by viewModel.currentUserMonetization.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()

    var showApplySuccessDialog by remember { mutableStateOf(false) }

    val user = currentUser
    val profile = monetizationProfile ?: MonetizationProfileEntity(
        userId = user?.id ?: "",
        channelId = "chan_${user?.id ?: ""}",
        followersCount = user?.followersCount ?: 0,
        publicVideosCount = 1,
        watchHours = 120.0,
        shortsViews = 24000L
    )

    // Calculate actual eligibility thresholds
    val followersTarget = 1000
    val watchHoursTarget = 4000.0
    val shortsViewsTarget = 10_000_000L

    val followersProgress = (profile.followersCount.toFloat() / followersTarget).coerceIn(0f, 1f)
    val watchHoursProgress = (profile.watchHours.toFloat() / watchHoursTarget.toFloat()).coerceIn(0f, 1f)
    val shortsProgress = (profile.shortsViews.toFloat() / shortsViewsTarget.toFloat()).coerceIn(0f, 1f)

    val meetsRequirements = profile.followersCount >= followersTarget &&
            (profile.watchHours >= watchHoursTarget || profile.shortsViews >= shortsViewsTarget)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("برنامج تحقيق الربح (Monetization)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("monetization_screen_scaffold")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (profile.status) {
                            "APPROVED" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            "PENDING_REVIEW" -> Color(0xFFFFF3CD).copy(alpha = 0.5f)
                            "SUSPENDED", "REJECTED" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (profile.status) {
                                    "APPROVED" -> Icons.Default.CheckCircle
                                    "PENDING_REVIEW" -> Icons.Default.HourglassTop
                                    "SUSPENDED", "REJECTED" -> Icons.Default.Cancel
                                    else -> Icons.Default.MonetizationOn
                                },
                                contentDescription = null,
                                tint = when (profile.status) {
                                    "APPROVED" -> Color(0xFF00C853)
                                    "PENDING_REVIEW" -> Color(0xFFF57C00)
                                    "SUSPENDED", "REJECTED" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(28.dp)
                            )

                            Text(
                                text = when (profile.status) {
                                    "APPROVED" -> "شريك معتمد (Partner Approved) ✓"
                                    "PENDING_REVIEW" -> "طلبك قيد المراجعة والتدقيق ⏳"
                                    "REJECTED" -> "تم رفض الطلب"
                                    "SUSPENDED" -> "تحقيق الربح معلّق مؤقتاً"
                                    "ELIGIBLE" -> "مؤهل لتقديم طلب الانضمام 🎉"
                                    else -> "غير مؤهل حالياً (قيد التقدم)"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text = when (profile.status) {
                                "APPROVED" -> "تهانينا! فيديوهاتك المؤهلة تشارك الآن في أرباح إعلانات Google AdMob بنسبة ${platformSettings.creatorSharePercent.toInt()}% لك و${platformSettings.platformSharePercent.toInt()}% للمنصة."
                                "PENDING_REVIEW" -> "فريق الإشراف يراجع قناتك للتأكد من أصالة المحتوى والمشاهدات وعدم وجود مخالفات أو تلاعب بالإعلانات. سنبلغك فور اتخاذ القرار."
                                "REJECTED" -> "سبب الرفض: ${profile.rejectionReason.ifBlank { "عدم استيفاء شروط جودة المحتوى وسياسات المنصة." }}"
                                "SUSPENDED" -> "تم تعليق أرباحك وإعلاناتك بسبب: ${profile.rejectionReason.ifBlank { "نشاط غير اعتيادي أو مخالفة لسياسات الملكية." }}"
                                "ELIGIBLE" -> "لقد استوفيت الشروط المطلوبة! اضغط على زر التقديم أدناه لبدء مراجعة القناة من قبل الإدارة."
                                else -> "واصل نشر الفيديوهات وتفاعل مع جمهورك للوصول إلى معايير الأهلية الموضحة أدناه."
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        )

                        // Quick Navigation for Approved creators
                        if (profile.status == "APPROVED") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onNavigateToStudio,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Creator Studio 📊", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = onNavigateToPayments,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("الأرباح والسحب 💳", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Criteria & Requirements Section
            item {
                Text(
                    text = "شروط الانضمام لبرنامج مشاركة أرباح الإعلانات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Metric 1: Followers
            item {
                RequirementCard(
                    title = "عدد المتابعين",
                    currentText = "${formatViews(profile.followersCount.toLong())} من ${formatViews(followersTarget.toLong())}",
                    progress = followersProgress,
                    isMet = profile.followersCount >= followersTarget,
                    icon = Icons.Default.Group
                )
            }

            // Metric 2: Watch Hours
            item {
                RequirementCard(
                    title = "ساعات المشاهدة العلنية (آخر 12 شهراً)",
                    currentText = "${String.format("%.1f", profile.watchHours)} ساعة من ${formatViews(watchHoursTarget.toLong())} ساعة",
                    progress = watchHoursProgress,
                    isMet = profile.watchHours >= watchHoursTarget,
                    icon = Icons.Default.Timer
                )
            }

            // Metric 3: Shorts Views (Alternative to watch hours)
            item {
                RequirementCard(
                    title = "أو: مشاهدات Shorts المؤهلة (آخر 90 يوماً)",
                    currentText = "${formatViews(profile.shortsViews)} من ${formatViews(shortsViewsTarget)} مشاهدة",
                    progress = shortsProgress,
                    isMet = profile.shortsViews >= shortsViewsTarget,
                    icon = Icons.Default.ElectricBolt
                )
            }

            // Important Terms & Policy Guidelines
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "شروط وقواعد استحقاق الربح الصارمة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        PolicyBullet(text = "يجب أن تكون الفيديوهات أصلية وغير منسوخة أو مسروقة.")
                        PolicyBullet(text = "ممنوع استخدام برامج التلاعب بالمشاهدات (Bots) أو شراء المتابعين.")
                        PolicyBullet(text = "نظام الحماية من الاحتيال (Anti-Fraud) يكتشف النقرات والمشاهدات الذاتية ويعلق الأرباح المشبوهة.")
                        PolicyBullet(text = "الإعلانات تظهر على جميع الفيديوهات لدعم المنصة، وتبدأ مشاركة الأرباح بعد اعتماد الحساب فقط (لا يتم احتساب أرباح بأثر رجعي).")
                    }
                }
            }

            // Action Button: Apply / Re-apply
            item {
                if (profile.status == "NOT_ELIGIBLE" || profile.status == "ELIGIBLE" || profile.status == "REJECTED") {
                    Button(
                        onClick = {
                            viewModel.submitMonetizationApplication()
                            showApplySuccessDialog = true
                        },
                        enabled = meetsRequirements && profile.status != "PENDING_REVIEW",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("apply_monetization_button"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (meetsRequirements) "تقديم طلب المراجعة والاعتماد" else "استوفِ الشروط لفتح زر التقديم",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showApplySuccessDialog) {
        AlertDialog(
            onDismissRequest = { showApplySuccessDialog = false },
            title = { Text("تم استلام طلبك بنجاح! 🚀", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "طلبك الآن في حالة (Pending Review). سيقوم فريق الإدارة بمراجعة نشاط القناة ونزاهة المشاهدات قبل تفعيل مشاركة الأرباح."
                )
            },
            confirmButton = {
                Button(onClick = { showApplySuccessDialog = false }) {
                    Text("حسناً")
                }
            }
        )
    }
}

@Composable
fun RequirementCard(
    title: String,
    currentText: String,
    progress: Float,
    isMet: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isMet) Color(0xFF00C853) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (isMet) {
                    Text(
                        text = "مستوفى ✓",
                        color = Color(0xFF00C853),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isMet) Color(0xFF00C853) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = currentText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PolicyBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}
