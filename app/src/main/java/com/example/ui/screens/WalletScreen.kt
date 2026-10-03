package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.VidoGold
import com.example.ui.theme.VidoPink
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: VidoMixViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val coinPackages by viewModel.activeCoinPackages.collectAsStateWithLifecycle()
    val purchaseOrders by viewModel.currentUserPurchaseOrders.collectAsStateWithLifecycle()
    val sentGifts by viewModel.currentUserSentGifts.collectAsStateWithLifecycle()
    val receivedGifts by viewModel.currentUserReceivedGifts.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var purchasingPackageId by remember { mutableStateOf<String?>(null) }
    var purchaseSuccessMessage by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("المحفظة والعملات 🪙", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (currentUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(48.dp), tint = VidoPurple)
                        Text("تسجيل الدخول مطلوب", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("يرجى تسجيل الدخول للوصول إلى محفظتك وإدارة العملات والهدايا.")
                        Button(onClick = onNavigateToAuth, shape = RoundedCornerShape(24.dp)) {
                            Text("تسجيل الدخول")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // 1. Balance Golden Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF2E1A47), Color(0xFF4A148C), Color(0xFF311B92))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("رصيد العملات المتاح", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "حساب آمن وموثوق ✓",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🪙", fontSize = 32.sp)
                                    Text(
                                        text = "${currentUser?.coinsBalance ?: 0}",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = VidoGold
                                    )
                                    Text("عملة", fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(top = 10.dp))
                                }

                                Text(
                                    text = "استخدم العملات لدعم المبدعين وإرسال هدايا ممتعة أثناء البث المباشر!",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // 2. Buy Coins Section (Google Play Billing packages)
                item {
                    Text(
                        text = "باقات شحن العملات (Google Play Billing)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                items(coinPackages, key = { it.id }) { pkg ->
                    val isPurchasingThis = purchasingPackageId == pkg.id
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (pkg.isPopular) Color(0xFF2C224D) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (pkg.isPopular) androidx.compose.foundation.BorderStroke(1.5.dp, VidoGold) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(VidoGold.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🪙", fontSize = 24.sp)
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${pkg.coinsAmount} عملة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        if (pkg.bonusCoins > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = VidoPink
                                            ) {
                                                Text(
                                                    text = "+${pkg.bonusCoins} هدية",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = pkg.title,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Price & Purchase Button
                            Button(
                                onClick = {
                                    purchasingPackageId = pkg.id
                                    scope.launch {
                                        val res = viewModel.purchaseCoins(pkg.id)
                                        purchasingPackageId = null
                                        if (res.isSuccess) {
                                            purchaseSuccessMessage = "تمت عملية الشراء بنجاح! أضيفت ${pkg.coinsAmount + pkg.bonusCoins} عملة إلى رصيدك."
                                        } else {
                                            snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "فشلت عملية الشراء")
                                        }
                                    }
                                },
                                enabled = purchasingPackageId == null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pkg.isPopular) VidoGold else VidoPurple,
                                    contentColor = if (pkg.isPopular) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("buy_package_${pkg.id}")
                            ) {
                                if (isPurchasingThis) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = "$${pkg.priceUsd}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Transactions History Tabs
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("الشحن (${purchaseOrders.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("المرسلة (${sentGifts.size})") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("المستلمة (${receivedGifts.size})") }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // Purchase Orders
                        if (purchaseOrders.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("لا توجد عمليات شحن سابقة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(purchaseOrders, key = { it.id }) { order ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("شحن عملات: +${order.coinsAmount} عملة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("المعرف: ${order.id}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(dateFormat.format(Date(order.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("$${order.pricePaidUsd}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VidoPurple)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF00C853).copy(alpha = 0.15f)
                                            ) {
                                                Text("ناجحة ✓", color = Color(0xFF00C853), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Sent Gifts
                        if (sentGifts.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("لم ترسل أي هدايا حتى الآن", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(sentGifts, key = { it.id }) { gtx ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(gtx.giftIcon, fontSize = 24.sp)
                                            Column {
                                                Text("${gtx.giftName} x${gtx.quantity}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(dateFormat.format(Date(gtx.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Text("-${gtx.totalCoins} عملة", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Received Gifts & Streamer Earnings
                        if (receivedGifts.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد هدايا مستلمة حتى الآن. ابدأ بثاً مباشراً لجذب المعجبين!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(receivedGifts, key = { it.id }) { gtx ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(gtx.giftIcon, fontSize = 24.sp)
                                            Column {
                                                Text("من: ${gtx.senderUsername}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("${gtx.giftName} x${gtx.quantity}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("+${gtx.totalCoins} عملة", color = VidoGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("+$${"%.2f".format(gtx.hostEarningsAmount)}", color = Color(0xFF00C853), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }

        // Purchase Success Dialog
        purchaseSuccessMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { purchaseSuccessMessage = null },
                title = { Text("تمت العملية بنجاح 🎉", fontWeight = FontWeight.Bold) },
                text = { Text(msg) },
                confirmButton = {
                    Button(onClick = { purchaseSuccessMessage = null }) {
                        Text("موافق")
                    }
                }
            )
        }
    }
}
