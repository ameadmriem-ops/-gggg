package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.theme.VidoGold
import com.example.ui.theme.VidoPink
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiveSetupScreen(
    viewModel: VidoMixViewModel,
    onNavigateBack: () -> Unit,
    onStreamStarted: (streamId: String) -> Unit,
    onNavigateToAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isEligible by remember { mutableStateOf(false) }
    var eligibilityMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingEligibility by remember { mutableStateOf(true) }

    // Form inputs
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("دردشة") }
    var selectedCoverUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&auto=format&fit=crop&q=80") }
    var isFrontCamera by remember { mutableStateOf(true) }
    var isMicEnabled by remember { mutableStateOf(true) }
    var commentsAllowed by remember { mutableStateOf(true) }
    var giftsAllowed by remember { mutableStateOf(true) }
    var isStartingStream by remember { mutableStateOf(false) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedCoverUrl = uri.toString()
        }
    }

    val categories = listOf("دردشة", "ألعاب", "تقنية", "ترفيه", "رياضة", "تعليم", "موسيقى")
    val defaultCovers = listOf(
        "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&auto=format&fit=crop&q=80"
    )

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            isCheckingEligibility = false
            isEligible = false
        } else {
            isCheckingEligibility = true
            val res = viewModel.checkLiveEligibility()
            isCheckingEligibility = false
            if (res.isSuccess) {
                isEligible = true
                eligibilityMessage = null
                if (title.isBlank()) {
                    title = "بث مباشر لـ ${currentUser?.fullName} 🔴"
                }
            } else {
                isEligible = false
                eligibilityMessage = res.exceptionOrNull()?.message ?: "غير مؤهل للبث المباشر"
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "إعداد البث المباشر (Stream Setup)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "التحقق المباشر من الأهلية (50 متابعًا في Firestore)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
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
            // Not logged in
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = VidoPurple)
                        Text(
                            text = "تسجيل الدخول مطلوب",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يرجى تسجيل الدخول أو إنشاء حساب للتمكن من بدء بث مباشر في VidoMix.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                        Button(
                            onClick = onNavigateToAuth,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تسجيل الدخول / إنشاء حساب")
                        }
                    }
                }
            }
        } else if (isCheckingEligibility) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VidoPurple)
            }
        } else if (!isEligible) {
            // Under 50 followers or restricted
            val currentFollowers = currentUser?.followersCount ?: 0
            val targetFollowers = 50
            val progress = (currentFollowers.toFloat() / targetFollowers.toFloat()).coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(VidoPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = VidoPurple,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "شرط فتح البث المباشر",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = eligibilityMessage ?: "تحتاج إلى 50 متابعًا لبدء بث مباشر.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )

                        // Follower Progress Bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المتابعون الحاليون", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currentFollowers / $targetFollowers", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VidoPurple)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = VidoPurple,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Text(
                            text = "نصيحة: انشر مقاطع Shorts وفيديوهات تفاعلية وشارك ملفك الشخصي لتصل إلى 50 متابعاً بسهولة ويتم فتح زر البث المباشر تلقائياً.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        Button(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("العودة إلى التطبيق")
                        }
                    }
                }
            }
        } else {
            // Eligible! Full Setup Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Firestore Verification Confirmation Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00C853).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00C853).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firestore_verified_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "حسابك مؤهل للبث المباشر (Firestore Verified) ✓",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF00C853)
                            )
                            Text(
                                text = "تم التحقق من استيفاء شرط الـ 50 متابعًا بنجاح (${currentUser?.followersCount ?: 50} متابع)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان البث المباشر *") },
                    placeholder = { Text("أدخل عنواناً جذاباً لمشاهديك...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_title_input")
                )

                // Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختر فئة البث:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = cat == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VidoPurple,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Cover Photo Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("صورة غلاف البث:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        TextButton(
                            onClick = {
                                coverPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("صورة من المعرض", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        defaultCovers.forEach { url ->
                            val isSelected = url == selectedCoverUrl
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, VidoPurple) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedCoverUrl = url }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Stream Controls Toggles
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("إعدادات الكاميرا والصوت:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        // Camera Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Cameraswitch, contentDescription = null, tint = VidoPurple)
                                Column {
                                    Text(if (isFrontCamera) "الكاميرا الأمامية" else "الكاميرا الخلفية", fontWeight = FontWeight.SemiBold)
                                    Text("التبديل بين العدسة الأمامية والخلفية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isFrontCamera,
                                onCheckedChange = { isFrontCamera = it },
                                modifier = Modifier.testTag("camera_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Mic Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(if (isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff, contentDescription = null, tint = VidoPurple)
                                Column {
                                    Text("الميكروفون", fontWeight = FontWeight.SemiBold)
                                    Text(if (isMicEnabled) "الصوت قيد التشغيل" else "كتم صوت الميكروفون", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isMicEnabled,
                                onCheckedChange = { isMicEnabled = it },
                                modifier = Modifier.testTag("mic_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Comments Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ChatBubble, contentDescription = null, tint = VidoPurple)
                                Column {
                                    Text("السماح بالتعليقات", fontWeight = FontWeight.SemiBold)
                                    Text("إظهار دردشة المشاهدين التفاعلية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = commentsAllowed,
                                onCheckedChange = { commentsAllowed = it },
                                modifier = Modifier.testTag("comments_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Gifts Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = VidoPink)
                                Column {
                                    Text("السماح باستلام الهدايا 🎁", fontWeight = FontWeight.SemiBold)
                                    Text("تمكين المشاهدين من إرسال هدايا العملات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = giftsAllowed,
                                onCheckedChange = { giftsAllowed = it },
                                modifier = Modifier.testTag("gifts_switch")
                            )
                        }
                    }
                }

                // Start Live Button
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("يرجى إدخال عنوان للبث المباشر") }
                            return@Button
                        }
                        isStartingStream = true
                        scope.launch {
                            val result = viewModel.startLiveStream(
                                title = title,
                                coverUrl = selectedCoverUrl,
                                category = selectedCategory,
                                isFrontCamera = isFrontCamera,
                                isMicEnabled = isMicEnabled,
                                commentsAllowed = commentsAllowed,
                                giftsAllowed = giftsAllowed
                            )
                            isStartingStream = false
                            if (result.isSuccess) {
                                onStreamStarted(result.getOrNull()!!.id)
                            } else {
                                snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "فشل بدء البث")
                            }
                        }
                    },
                    enabled = !isStartingStream,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_live_button")
                ) {
                    if (isStartingStream) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = "بدء البث المباشر الآن",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
