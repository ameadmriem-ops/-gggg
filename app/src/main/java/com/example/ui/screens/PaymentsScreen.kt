package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MonetizationProfileEntity
import com.example.data.model.PayoutEntity
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.VidoCoral
import com.example.ui.theme.VidoCyan
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    viewModel: VidoMixViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val monetizationProfile by viewModel.currentUserMonetization.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()
    val payouts by viewModel.currentUserPayouts.collectAsStateWithLifecycle()

    var showPayoutDialog by remember { mutableStateOf(false) }
    var payoutAmountInput by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("BANK_WIRE") }
    var payoutDetailsInput by remember { mutableStateOf("") }
    var payoutErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val user = currentUser
    val profile = monetizationProfile ?: MonetizationProfileEntity(
        userId = user?.id ?: "",
        channelId = "chan_${user?.id ?: ""}",
        currentBalance = 0.0,
        pendingBalance = 0.0
    )

    val minThreshold = platformSettings.minPayoutThreshold

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الأرباح والمدفوعات (Payments)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("payments_screen_scaffold")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "💳 أرصدة الحساب الحالية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("الرصيد المتاح للسحب", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$${String.format("%.2f", profile.currentBalance)}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00C853)
                                )
                            }

                            Column {
                                Text("الرصيد المعلق (Pending)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$${String.format("%.2f", profile.pendingBalance)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF57C00)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الحد الأدنى للسحب: $${minThreshold.toInt()}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    payoutAmountInput = String.format("%.2f", profile.currentBalance)
                                    payoutErrorMessage = null
                                    showPayoutDialog = true
                                },
                                enabled = profile.status == "APPROVED" && profile.currentBalance >= minThreshold,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("request_payout_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طلب سحب الأرباح")
                            }
                        }
                    }
                }
            }

            // Security Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "تتم جميع عمليات التحويل المالي عبر خوادم وبوابات الدفع الآمنة المعتمدة بعد مراجعة الفريق المالي.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Payment History Header
            item {
                Text(
                    text = "سجل طلبات السحب (Payout History)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (payouts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد طلبات سحب سابقة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(payouts, key = { it.id }) { payout ->
                    PayoutHistoryItem(payout = payout)
                }
            }
        }
    }

    // Payout Request Dialog
    if (showPayoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showPayoutDialog = false },
            title = { Text("طلب سحب جديد 💸", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    payoutErrorMessage?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 12.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = payoutAmountInput,
                        onValueChange = { payoutAmountInput = it },
                        label = { Text("المبلغ المراد سحبه ($ USD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("طريقة الاستلام:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMethod == "BANK_WIRE",
                            onClick = { selectedMethod = "BANK_WIRE" },
                            label = { Text("تحويل بنكي") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMethod == "PAYPAL",
                            onClick = { selectedMethod = "PAYPAL" },
                            label = { Text("PayPal") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMethod == "WISE",
                            onClick = { selectedMethod = "WISE" },
                            label = { Text("Wise") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = payoutDetailsInput,
                        onValueChange = { payoutDetailsInput = it },
                        label = { Text(if (selectedMethod == "PAYPAL") "بريد حساب PayPal" else if (selectedMethod == "WISE") "بريد Wise" else "رقم الآيبان (IBAN) واسم البنك") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payoutAmountInput.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            payoutErrorMessage = "يرجى إدخال مبلغ سحب صحيح"
                            return@Button
                        }
                        if (payoutDetailsInput.isBlank()) {
                            payoutErrorMessage = "يرجى كتابة تفاصيل الحساب لاستلام الدفعة"
                            return@Button
                        }

                        isSubmitting = true
                        coroutineScope.launch {
                            val result = viewModel.requestPayout(
                                amount = amount,
                                method = selectedMethod,
                                details = payoutDetailsInput
                            )
                            isSubmitting = false
                            if (result.isSuccess) {
                                showPayoutDialog = false
                            } else {
                                payoutErrorMessage = result.exceptionOrNull()?.message ?: "فشل تقديم طلب السحب"
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("تأكيد الطلب")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayoutDialog = false }, enabled = !isSubmitting) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun PayoutHistoryItem(payout: PayoutEntity, modifier: Modifier = Modifier) {
    val (statusText, statusColor) = when (payout.status) {
        "PAID" -> "مكتمل ومدفوع ✓" to Color(0xFF00C853)
        "PROCESSING" -> "جارٍ المعالجة ⚙" to Color(0xFF0288D1)
        "PENDING" -> "قيد المراجعة ⏳" to Color(0xFFF57C00)
        "FAILED" -> "فشل التحويل ✖" to MaterialTheme.colorScheme.error
        "REJECTED" -> "مرفوض" to MaterialTheme.colorScheme.error
        else -> payout.status to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$${String.format("%.2f", payout.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = when (payout.method) {
                            "BANK_WIRE" -> "تحويل بنكي"
                            "PAYPAL" -> "PayPal"
                            "WISE" -> "Wise"
                            else -> payout.method
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${payout.payoutDetails} • ${formatTimeAgo(payout.requestedAt)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (payout.adminNotes.isNotBlank()) {
                    Text(
                        text = "ملاحظة الإدارة: ${payout.adminNotes}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
