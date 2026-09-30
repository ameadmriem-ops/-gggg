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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Logout
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
import com.example.ui.components.VideoCard
import com.example.ui.components.formatViews
import com.example.ui.theme.VidoCoral
import com.example.ui.theme.VidoCyan
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: VidoMixViewModel,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToChannel: (String) -> Unit,
    onNavigateToMonetization: () -> Unit = {},
    onNavigateToStudio: () -> Unit = {},
    onNavigateToPayments: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val savedVideos by viewModel.savedVideos.collectAsStateWithLifecycle()
    val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
    val allVideos by viewModel.rawLongVideos.collectAsStateWithLifecycle()
    val allShorts by viewModel.shortsList.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val channelMap = remember(channels) { channels.associateBy { it.id } }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    if (currentUser == null) {
        // Guest state
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "قم بتسجيل الدخول للاستمتاع بكافة المزايا",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Button(
                    onClick = onNavigateToAuth,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("login_button_prompt")
                ) {
                    Text("تسجيل الدخول / إنشاء حساب")
                }
            }
        }
        return
    }

    val user = currentUser!!
    val userVideos = remember(allVideos, allShorts, user) {
        (allVideos + allShorts).filter { vid ->
            val chan = channelMap[vid.channelId]
            chan?.userId == user.id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الملف الشخصي", fontWeight = FontWeight.Bold) },
                actions = {
                    if (user.isAdmin) {
                        IconButton(
                            onClick = onNavigateToAdmin,
                            modifier = Modifier.testTag("admin_panel_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AdminPanelSettings,
                                contentDescription = "لوحة الإدارة",
                                tint = VidoCoral
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("profile_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("profile_screen_scaffold")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Avatar
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = user.fullName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            if (user.isVerified) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "موثق",
                                    tint = VidoCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (user.isAdmin) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(VidoCoral)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("مشرف", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "@${user.username} • ${user.email}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (user.bio.isNotBlank()) {
                            Text(
                                text = user.bio,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats: Followers, Following, Videos, Shorts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatViews(user.followersCount.toLong()),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text("المتابعون", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatViews(user.followingCount.toLong()),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text("يتابع", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = userVideos.count { !it.isShort }.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text("الفيديوهات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = userVideos.count { it.isShort }.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text("Shorts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Edit Profile & Creator Studio Shortcuts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showEditProfileDialog = true },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تعديل", fontSize = 12.sp)
                            }

                            Button(
                                onClick = onNavigateToStudio,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("creator_studio_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Studio 📊", fontSize = 12.sp)
                            }

                            Button(
                                onClick = onNavigateToMonetization,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_monetization_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                            ) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("الربح 💰", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Tabs: فيديوهاتي, المحفوظات, سجل المشاهدة
            item {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("فيديوهاتي (${userVideos.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("المحفوظات (${savedVideos.size})") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("سجل المشاهدة (${watchHistory.size})") }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    if (userVideos.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("لم تقم بنشر أي فيديو حتى الآن. اضغط على زر (+) للبدء!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(userVideos, key = { it.id }) { video ->
                            val channel = channelMap[video.channelId]
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                VideoCard(
                                    video = video,
                                    channel = channel,
                                    onVideoClick = onNavigateToVideo,
                                    onChannelClick = onNavigateToChannel
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (savedVideos.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد فيديوهات محفوظة. احفظ الفيديوهات لمشاهدتها لاحقاً!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(savedVideos, key = { it.id }) { video ->
                            val channel = channelMap[video.channelId]
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                VideoCard(
                                    video = video,
                                    channel = channel,
                                    onVideoClick = onNavigateToVideo,
                                    onChannelClick = onNavigateToChannel,
                                    isSaved = true,
                                    onSaveToggle = { viewModel.toggleSaveVideoById(it) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    if (watchHistory.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("سجل المشاهدة فارغ.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { viewModel.clearWatchHistory() }) {
                                    Text("مسح سجل المشاهدة", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        items(watchHistory, key = { it.id }) { video ->
                            val channel = channelMap[video.channelId]
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                VideoCard(
                                    video = video,
                                    channel = channel,
                                    onVideoClick = onNavigateToVideo,
                                    onChannelClick = onNavigateToChannel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(user.fullName) }
        var editBio by remember { mutableStateOf(user.bio) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("تعديل الملف الشخصي", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("الاسم الكامل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("نبذة عنك") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.authManager.updateProfile(editName, editBio)
                            showEditProfileDialog = false
                        }
                    }
                ) {
                    Text("حفظ التغييرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "الإعدادات والخيارات",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                // Dark mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.DarkMode, contentDescription = null)
                        Text("الوضع الليلي (Dark Theme)")
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }

                ListItem(
                    headlineContent = { Text("تحقيق الربح (Monetization)") },
                    supportingContent = { Text("برنامج مشاركة أرباح إعلانات Google AdMob") },
                    leadingContent = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF00C853)) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        onNavigateToMonetization()
                    }
                )

                ListItem(
                    headlineContent = { Text("استوديو المنشئين (Creator Studio)") },
                    supportingContent = { Text("إحصائيات المشاهدات والتفاعل والإيرادات") },
                    leadingContent = { Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        onNavigateToStudio()
                    }
                )

                ListItem(
                    headlineContent = { Text("الأرباح والمدفوعات (Payments)") },
                    supportingContent = { Text("رصيد الأرباح المتاح وطلبات السحب") },
                    leadingContent = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = VidoCyan) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        onNavigateToPayments()
                    }
                )

                if (user.isAdmin) {
                    ListItem(
                        headlineContent = { Text("لوحة الإدارة والمشرفين") },
                        supportingContent = { Text("إدارة البلاغات والمحتوى والمستخدمين") },
                        leadingContent = { Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = VidoCoral) },
                        modifier = Modifier.clickable {
                            showSettingsSheet = false
                            onNavigateToAdmin()
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text("تبديل الحساب") },
                    leadingContent = { Icon(Icons.Default.SwitchAccount, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        onNavigateToAuth()
                    }
                )

                ListItem(
                    headlineContent = { Text("تسجيل الخروج", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Outlined.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        viewModel.authManager.logout()
                    }
                )
            }
        }
    }
}
