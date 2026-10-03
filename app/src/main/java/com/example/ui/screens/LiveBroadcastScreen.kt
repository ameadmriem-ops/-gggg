package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.LiveCommentEntity
import com.example.data.model.LiveGiftTransactionEntity
import com.example.ui.components.LiveGiftTrayBottomSheet
import com.example.ui.components.LiveViewersBottomSheet
import com.example.ui.theme.VidoGold
import com.example.ui.theme.VidoPink
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(UnstableApi::class)
@Composable
fun LiveBroadcastScreen(
    streamId: String,
    viewModel: VidoMixViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentStream by viewModel.currentLiveStream.collectAsStateWithLifecycle()
    val comments by viewModel.currentLiveComments.collectAsStateWithLifecycle()
    val viewers by viewModel.currentStreamViewers.collectAsStateWithLifecycle()
    val gifts by viewModel.activeGifts.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val isHost = remember(currentStream, currentUser) {
        currentStream?.hostUserId == currentUser?.id
    }

    val isHostFollowed by remember(currentStream) {
        if (currentStream != null) viewModel.isHostFollowed(currentStream!!.hostUserId)
        else kotlinx.coroutines.flow.flowOf(false)
    }.collectAsStateWithLifecycle(initialValue = false)

    var commentText by remember { mutableStateOf("") }
    var showGiftTray by remember { mutableStateOf(false) }
    var showViewersList by remember { mutableStateOf(false) }
    var showEndStreamDialog by remember { mutableStateOf(false) }
    var showStreamSummarySheet by remember { mutableStateOf(false) }
    var showStreamEndedDialog by remember { mutableStateOf(false) }
    var streamEndedMessage by remember { mutableStateOf("") }
    var selectedCommentForModeration by remember { mutableStateOf<LiveCommentEntity?>(null) }
    var areControlsVisible by remember { mutableStateOf(true) }

    // Auto-hide controls after 7 seconds of inactivity
    LaunchedEffect(areControlsVisible) {
        if (areControlsVisible) {
            delay(7000)
            areControlsVisible = false
        }
    }

    // Monitor stream status for viewers
    LaunchedEffect(currentStream?.status) {
        val status = currentStream?.status
        if (status == "ENDED" && !isHost) {
            streamEndedMessage = "انتهى هذا البث المباشر. شكراً لمتابعتك!"
            showStreamEndedDialog = true
        } else if (status == "BANNED") {
            streamEndedMessage = "تم إيقاف هذا البث المباشر لمخالفته إرشادات المجتمع."
            showStreamEndedDialog = true
        }
    }

    // Floating Like Hearts animation state
    data class FloatingHeart(val id: String, val color: Color, val xOffset: Float)
    var floatingHearts by remember { mutableStateOf<List<FloatingHeart>>(emptyList()) }

    // Active Gift Animation Banner
    var activeGiftBanner by remember { mutableStateOf<LiveGiftTransactionEntity?>(null) }

    // Initialize Stream
    LaunchedEffect(streamId) {
        viewModel.openLiveStream(streamId)
    }

    // Auto-scroll comments
    val commentsListState = rememberLazyListState()
    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            commentsListState.animateScrollToItem(comments.size - 1)
        }
    }

    // Background ExoPlayer for stream playback
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    LaunchedEffect(currentStream?.streamUrl) {
        val url = currentStream?.streamUrl
        if (url != null) {
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            viewModel.closeLiveStream(streamId)
        }
    }

    BackHandler {
        if (isHost) {
            showEndStreamDialog = true
        } else {
            onNavigateBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
            }
    ) {
        // 1. Live Background Video Stream
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Subtle gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // 2. Floating Likes Animation Layer (Vertical floating hearts)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 120.dp)
                .width(80.dp)
                .height(260.dp)
        ) {
            floatingHearts.forEach { heart ->
                key(heart.id) {
                    val transition = rememberInfiniteTransition(label = "heart")
                    val animY by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = -240f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "y"
                    )
                    val animAlpha by transition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1800, easing = FastOutLinearInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "alpha"
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = heart.xOffset.dp, y = animY.dp)
                            .alpha(animAlpha)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = heart.color,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // 3. Gift Animation Banner Overlay
        AnimatedVisibility(
            visible = activeGiftBanner != null,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        ) {
            activeGiftBanner?.let { giftTx ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, VidoGold),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = giftTx.senderAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(giftTx.senderUsername, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("أرسل ${giftTx.giftName}!", color = VidoGold, fontSize = 12.sp)
                        }
                        Text(giftTx.giftIcon, fontSize = 32.sp)
                        Text("x${giftTx.quantity}", color = VidoPink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        }

        // Center Spectacular Gift Animation (TikTok Live inspired)
        AnimatedVisibility(
            visible = activeGiftBanner != null,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            activeGiftBanner?.let { giftTx ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        VidoGold.copy(alpha = 0.6f),
                                        VidoPink.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = giftTx.giftIcon,
                            fontSize = 72.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, VidoGold)
                    ) {
                        Text(
                            text = "${giftTx.senderUsername} أرسل ${giftTx.giftName} x${giftTx.quantity}!",
                            color = VidoGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 4. Main HUD Layer (Controls, Header, Comments)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP BAR: Host info, Viewers Count, End/Exit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Host Profile Chip
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier.clickable {
                            currentStream?.let { onNavigateToChannel(it.hostUserId) }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box {
                                AsyncImage(
                                    model = currentStream?.hostAvatarUrl,
                                    contentDescription = currentStream?.hostFullName,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFFE91E63), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                            Column {
                                Text(
                                    text = currentStream?.hostFullName ?: "مباشر",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "🔴 ${currentStream?.viewersCount ?: 1} مشاهد",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }

                            // Follow button if viewer is not the host
                            if (!isHost && currentUser != null) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isHostFollowed) Color.White.copy(alpha = 0.2f) else Color(0xFFE91E63),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            currentStream?.let { stream ->
                                                viewModel.toggleFollowHostUser(stream.hostUserId)
                                            }
                                        }
                                ) {
                                    Text(
                                        text = if (isHostFollowed) "متابَع ✓" else "+ متابعة",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Viewers avatars & Exit button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Viewers Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { showViewersList = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.People, contentDescription = "المشاهدون", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("${viewers.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Exit or End Stream Button
                        IconButton(
                            onClick = {
                                if (isHost) {
                                    showEndStreamDialog = true
                                } else {
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // PINNED COMMENT BANNER (if active)
                currentStream?.pinnedCommentText?.let { pinText ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2C2448).copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VidoPurple),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PushPin, contentDescription = "مثبت", tint = VidoGold, modifier = Modifier.size(16.dp))
                                Column {
                                    Text(
                                        text = currentStream?.pinnedCommentUser ?: "تعليق مثبت",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = VidoGold
                                    )
                                    Text(
                                        text = pinText,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                            if (isHost) {
                                IconButton(
                                    onClick = { scope.launch { viewModel.unpinLiveComment(streamId) } },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إلغاء التثبيت", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // BOTTOM SECTION: Comments & Interactive Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Floating Comments List (Max height 200dp)
                    LazyColumn(
                        state = commentsListState,
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            val isClickable = isHost || (currentUser?.isAdmin == true)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (comment.isSystemNotification) Color(0xFF673AB7).copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.clickable(enabled = isClickable) {
                                    if (isClickable) {
                                        selectedCommentForModeration = comment
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (comment.userAvatarUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = comment.userAvatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Text(
                                        text = comment.username,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (comment.isHost) VidoGold else Color.White.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = comment.text,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Controls Row: Input + Like + Share + Gift
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Comment Input Field
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("شارك برأيك في البث...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f)) },
                            singleLine = true,
                            trailingIcon = {
                                if (commentText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val text = commentText
                                            commentText = ""
                                            scope.launch {
                                                val res = viewModel.sendLiveComment(streamId, text)
                                                if (res.isFailure) {
                                                    snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "تعذر إرسال التعليق")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "إرسال", tint = VidoPurple)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = VidoPurple,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("live_comment_input")
                        )

                        // Gift Button 🎁
                        IconButton(
                            onClick = { showGiftTray = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("live_gift_tray_button")
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = "هدية", tint = VidoPink, modifier = Modifier.size(24.dp))
                        }

                        // Like Heart Button ❤️
                        IconButton(
                            onClick = {
                                viewModel.addLiveLike(streamId)
                                // Trigger floating animated heart
                                val colors = listOf(Color(0xFFE91E63), Color(0xFFFF5722), Color(0xFF9C27B0), Color(0xFF00E676), Color(0xFFFFD700))
                                val newHeart = FloatingHeart(
                                    id = UUID.randomUUID().toString(),
                                    color = colors.random(),
                                    xOffset = (-20..20).random().toFloat()
                                )
                                floatingHearts = floatingHearts + newHeart
                                scope.launch {
                                    delay(2000)
                                    floatingHearts = floatingHearts.filter { it.id != newHeart.id }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("live_like_button")
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "إعجاب", tint = Color.Red, modifier = Modifier.size(24.dp))
                        }

                        // Share Button 🔗
                        IconButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "شاهد بث مباشر الآن لـ ${currentStream?.hostFullName}: https://vidomix.app/live/$streamId")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "مشاركة البث المباشر")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

        // 5. Offline Reconnecting Indicator
        if (!isOnline) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text("انقطع الاتصال، جاري إعادة المحاولة...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // 6. Gift Tray Bottom Sheet
        if (showGiftTray) {
            LiveGiftTrayBottomSheet(
                gifts = gifts,
                userCoinsBalance = currentUser?.coinsBalance ?: 0L,
                onDismiss = { showGiftTray = false },
                onSendGift = { giftId, quantity ->
                    showGiftTray = false
                    scope.launch {
                        val res = viewModel.sendLiveGift(streamId, giftId, quantity)
                        if (res.isSuccess) {
                            activeGiftBanner = res.getOrNull()
                            delay(3500)
                            activeGiftBanner = null
                        } else {
                            snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "فشل إرسال الهدية")
                        }
                    }
                },
                onRechargeClick = {
                    showGiftTray = false
                    onNavigateToWallet()
                }
            )
        }

        // 7. Viewers List Bottom Sheet
        if (showViewersList) {
            LiveViewersBottomSheet(
                viewers = viewers,
                isHost = isHost,
                onDismiss = { showViewersList = false },
                onMuteUser = { targetId, username ->
                    scope.launch {
                        viewModel.muteUserInLive(streamId, targetId, username)
                        snackbarHostState.showSnackbar("تم كتم $username في هذا البث.")
                    }
                },
                onBanUser = { targetId, username ->
                    scope.launch {
                        viewModel.banUserFromLive(streamId, targetId, username)
                        snackbarHostState.showSnackbar("تم حظر $username وطردِه من البث.")
                    }
                }
            )
        }

        // 8. Comment Moderation Dialog (Host / Mod)
        selectedCommentForModeration?.let { modComment ->
            AlertDialog(
                onDismissRequest = { selectedCommentForModeration = null },
                title = { Text("إدارة التعليق (${modComment.username})") },
                text = { Text("\"${modComment.text}\"") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                viewModel.pinLiveComment(streamId, modComment)
                                selectedCommentForModeration = null
                            }
                        }
                    ) {
                        Text("📌 تثبيت التعليق")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    viewModel.muteUserInLive(streamId, modComment.userId, modComment.username)
                                    selectedCommentForModeration = null
                                    snackbarHostState.showSnackbar("تم كتم ${modComment.username}")
                                }
                            }
                        ) {
                            Text("كتم المستخدم")
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    viewModel.deleteLiveComment(modComment.id)
                                    selectedCommentForModeration = null
                                }
                            }
                        ) {
                            Text("حذف", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }

        // 9. End Stream Dialog (For Host)
        if (showEndStreamDialog) {
            AlertDialog(
                onDismissRequest = { showEndStreamDialog = false },
                title = { Text("إنهاء البث المباشر") },
                text = { Text("هل أنت متأكد من إنهاء جلسة البث الحالية؟ سيتم حفظ إحصائيات البث وأرباح الهدايا في حسابك.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showEndStreamDialog = false
                            scope.launch {
                                viewModel.endLiveStream(streamId)
                                showStreamSummarySheet = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("إنهاء البث")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndStreamDialog = false }) {
                        Text("متابعة البث")
                    }
                }
            )
        }

        // 10. Stream Summary Sheet after host ends stream
        if (showStreamSummarySheet) {
            AlertDialog(
                onDismissRequest = {
                    showStreamSummarySheet = false
                    onNavigateBack()
                },
                title = { Text("ملخص البث المباشر 📊", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("أحسنت! اكتمل بثك المباشر بنجاح.")
                        HorizontalDivider()
                        Text("👥 ذروة المشاهدين: ${currentStream?.peakViewers ?: 1}")
                        Text("❤️ إجمالي الإعجابات: ${currentStream?.likesCount ?: 0}")
                        Text("🎁 إجمالي الهدايا: ${currentStream?.totalGiftsCount ?: 0}")
                        Text("🪙 إجمالي العملات المكتسبة: ${currentStream?.totalCoinsEarned ?: 0}")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showStreamSummarySheet = false
                            onNavigateBack()
                        }
                    ) {
                        Text("تم والعودة للرئيسية")
                    }
                }
            )
        }

        // 11. Stream Ended Dialog for Viewers
        if (showStreamEndedDialog) {
            AlertDialog(
                onDismissRequest = {
                    showStreamEndedDialog = false
                    onNavigateBack()
                },
                title = { Text("تنبيه البث المباشر 🔴", fontWeight = FontWeight.Bold) },
                text = { Text(streamEndedMessage) },
                confirmButton = {
                    Button(
                        onClick = {
                            showStreamEndedDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("العودة للرئيسية")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
