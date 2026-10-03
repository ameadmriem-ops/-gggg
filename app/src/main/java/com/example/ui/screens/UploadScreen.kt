package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.LocalVideoFileInfo
import com.example.core.VideoMetadataHelper
import com.example.data.model.VideoEntity
import com.example.player.VideoPlayerView
import com.example.ui.components.videoCategories
import com.example.ui.theme.VidoCoral
import com.example.ui.theme.VidoCyan
import com.example.ui.theme.VidoGold
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class VideoPreset(
    val name: String,
    val url: String,
    val thumb: String,
    val duration: Int,
    val category: String,
    val description: String
)

val samplePresets = listOf(
    VideoPreset(
        name = "تقنية وذكاء اصطناعي",
        url = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4",
        thumb = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
        duration = 340,
        category = "تقنية",
        description = "نظرة مستقبلية واستعراض أحدث تقنيات ونماذج الذكاء الاصطناعي التوليدي لعام 2026."
    ),
    VideoPreset(
        name = "جيمينج وتحديات",
        url = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_1MB.mp4",
        thumb = "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=600&auto=format&fit=crop&q=80",
        duration = 210,
        category = "ألعاب",
        description = "مغامرة وتحدي في عالم الألعاب الرقمية بدقة عالية ومعدل إطارات سلس."
    ),
    VideoPreset(
        name = "طبيعة وسفر",
        url = "https://filesamples.com/samples/video/mp4/sample_960x400_ocean_with_audio.mp4",
        thumb = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80",
        duration = 590,
        category = "ترفيه",
        description = "مشاهد ساحرة للأمواج والطبيعة الخلابة مع أصوات البحر النقية."
    ),
    VideoPreset(
        name = "رياضة وأكشن",
        url = "https://filesamples.com/samples/video/mp4/sample_1280x720.mp4",
        thumb = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600&auto=format&fit=crop&q=80",
        duration = 180,
        category = "رياضة",
        description = "لقطات حماسية وتدريبات رياضية مكثفة لأبطال اللياقة البدنية."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: VidoMixViewModel,
    onUploadSuccess: () -> Unit,
    onStartLiveClick: () -> Unit = {},
    onNavigateToVideo: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userUploadedVideos by viewModel.userUploadedVideos.collectAsStateWithLifecycle()

    // Screen Mode: 0 = رفع فيديو جديد, 1 = فيديوهاتي في Room
    var selectedScreenTab by remember { mutableIntStateOf(0) }

    // Upload Form State
    var isShort by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var soundTrackTitle by remember { mutableStateOf("الصوت الأصلي - VidoMix") }
    var selectedCategory by remember { mutableStateOf("تقنية") }
    var isPublic by remember { mutableStateOf(true) }

    // Selected Video State
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoUrl by remember { mutableStateOf(samplePresets.first().url) }
    var selectedThumbnailUrl by remember { mutableStateOf(samplePresets.first().thumb) }
    var videoDurationSeconds by remember { mutableIntStateOf(samplePresets.first().duration) }
    var extractedFileInfo by remember { mutableStateOf<LocalVideoFileInfo?>(null) }
    var isExtractingMetadata by remember { mutableStateOf(false) }

    // Upload Progress & Dialog
    var isPublishing by remember { mutableStateOf(false) }
    var publishingStep by remember { mutableStateOf("جارٍ تجهيز ملف الفيديو...") }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastUploadedVideoTitle by remember { mutableStateOf("") }
    var videoToDelete by remember { mutableStateOf<VideoEntity?>(null) }

    // Quick Hashtags
    val popularHashtags = listOf("#تقنية", "#محتوى_عربي", "#برمجة", "#VidoMix", "#شورتس", "#ذكاء_اصطناعي", "#ألعاب", "#سفر")

    // Helper to process picked video URI
    fun onVideoPicked(uri: Uri) {
        selectedVideoUri = uri
        selectedVideoUrl = uri.toString()
        isExtractingMetadata = true
        uploadStatusMessage = null

        coroutineScope.launch {
            try {
                val info = VideoMetadataHelper.extractMetadata(context, uri)
                extractedFileInfo = info
                videoDurationSeconds = info.durationSeconds

                // Auto-suggest title if current title is empty
                if (title.isBlank()) {
                    val suggested = info.fileName
                        .substringBeforeLast(".")
                        .replace("_", " ")
                        .replace("-", " ")
                        .trim()
                    if (suggested.isNotBlank()) {
                        title = suggested
                    }
                }

                // If thumbnail was extracted from video frame
                if (info.thumbnailUri.isNotBlank()) {
                    selectedThumbnailUrl = info.thumbnailUri
                }

                // Auto-detect Shorts mode if portrait orientation
                if (info.isPortrait) {
                    isShort = true
                }

                // Update sound title
                val username = currentUser?.fullName ?: currentUser?.username ?: "المستخدم"
                soundTrackTitle = "الصوت الأصلي - $username"

                uploadStatusMessage = "تم استخراج بيانات الفيديو من الجهاز بنجاح!"
            } catch (e: Exception) {
                uploadStatusMessage = "تم اختيار الفيديو بنجاح (تعذر استخراج بعض البيانات الوصفية: ${e.localizedMessage})"
            } finally {
                isExtractingMetadata = false
            }
        }
    }

    // Media Picker for device gallery (Photo Picker - modern Android)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onVideoPicked(uri)
        }
    }

    // Storage Picker for any device files (File Manager / Open Document)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onVideoPicked(uri)
        }
    }

    // Prepare preview in player when selectedVideoUrl changes
    LaunchedEffect(selectedVideoUrl) {
        if (selectedVideoUrl.isNotBlank()) {
            viewModel.playerController.prepareAndPlay(selectedVideoUrl, autoPlay = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "رفع وإدارة المحتوى",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "تخزين محلي آمن ومباشر عبر Room Database",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onStartLiveClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFE91E63).copy(alpha = 0.15f),
                            contentColor = Color(0xFFE91E63)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("بث مباشر 🔴", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("upload_screen_scaffold")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("upload_scroll_view"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Mode Tabs: رفع جديد vs فيديوهاتي في Room
            item {
                TabRow(
                    selectedTabIndex = selectedScreenTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedScreenTab == 0,
                        onClick = { selectedScreenTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("رفع فيديو جديد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedScreenTab == 1,
                        onClick = { selectedScreenTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("محفوظ في Room (${userUploadedVideos.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                }
            }

            // TAB 1: فيديوهاتي المحفوظة محلياً في Room Database
            if (selectedScreenTab == 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الفيديوهات المرفوعة محلياً (Room)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "بيانات وصفية مخزنة محلياً بالكامل ومتاحة بدون إنترنت",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                text = "${userUploadedVideos.size} فيديو",
                                modifier = Modifier.padding(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (userUploadedVideos.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = "لا توجد فيديوهات مرفوعة حتى الآن",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "اختر ملف فيديو من هاتفك وقم بتعبئة العنوان والوصف لحفظه في قاعدة البيانات المحلية (Room).",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { selectedScreenTab = 0 },
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("رفع فيديو من الجهاز الآن")
                                }
                            }
                        }
                    }
                } else {
                    items(userUploadedVideos, key = { it.id }) { video ->
                        LocalUploadedVideoItemCard(
                            video = video,
                            onPlayClick = {
                                onNavigateToVideo(video.id)
                            },
                            onDeleteClick = {
                                videoToDelete = video
                            }
                        )
                    }
                }
            }

            // TAB 0: رفع فيديو جديد من الجهاز مع البيانات الوصفية (Room Persistence)
            if (selectedScreenTab == 0) {
                // Section 1: Device Video Picker Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                color = if (selectedVideoUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedVideoUri != null)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PermMedia,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "اختيار ملف فيديو من ذاكرة الجهاز",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "اختر من معرض الوسائط أو متصفح الملفات (MP4, MKV, WebM)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Primary Button: Android Photo Picker
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(46.dp)
                                        .testTag("pick_from_device_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("معرض الفيديوهات", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                // Secondary Button: All Files / Storage
                                OutlinedButton(
                                    onClick = {
                                        documentPickerLauncher.launch("video/*")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("pick_from_files_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("كل الملفات", fontSize = 13.sp)
                                }
                            }

                            // Quick sample presets for testing on emulator
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "أو اختر عينة فيديو سريعة للتجربة الفورية:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(samplePresets) { preset ->
                                        val isSelected = selectedVideoUrl == preset.url && selectedVideoUri == null
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier.clickable {
                                                selectedVideoUri = null
                                                selectedVideoUrl = preset.url
                                                selectedThumbnailUrl = preset.thumb
                                                videoDurationSeconds = preset.duration
                                                selectedCategory = preset.category
                                                if (title.isBlank()) title = preset.name
                                                if (description.isBlank()) description = preset.description
                                                extractedFileInfo = LocalVideoFileInfo(
                                                    uri = Uri.parse(preset.url),
                                                    fileName = "${preset.name}.mp4",
                                                    fileSizeFormatted = "18.2 MB",
                                                    fileSizeBytes = 19084000L,
                                                    durationSeconds = preset.duration,
                                                    durationFormatted = "%02d:%02d".format(preset.duration / 60, preset.duration % 60),
                                                    width = 1920,
                                                    height = 1080,
                                                    resolutionLabel = "1080p FHD",
                                                    isPortrait = false,
                                                    thumbnailUri = preset.thumb,
                                                    mimeType = "video/mp4"
                                                )
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    model = preset.thumb,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = preset.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Extracted Video Technical Metadata Card
                extractedFileInfo?.let { info ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("extracted_metadata_card"),
                            shape = RoundedCornerShape(14.dp),
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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF00C853),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "البيانات التقنية المستخرجة من الملف:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                text = if (info.isPortrait) "مقطع عمودي (Shorts)" else "فيديو أفقي (Standard)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TechnicalInfoChip(
                                        icon = Icons.Outlined.Description,
                                        label = "الملف",
                                        value = info.fileName.take(18),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TechnicalInfoChip(
                                        icon = Icons.Outlined.DataUsage,
                                        label = "الحجم",
                                        value = info.fileSizeFormatted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TechnicalInfoChip(
                                        icon = Icons.Outlined.Timer,
                                        label = "المدة",
                                        value = info.durationFormatted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TechnicalInfoChip(
                                        icon = Icons.Outlined.HighQuality,
                                        label = "الدقة",
                                        value = info.resolutionLabel,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Format Selector (فيديو طويل vs مقطع قصير Shorts)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isShort) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { isShort = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "فيديو طويل 📺",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (!isShort) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isShort) VidoCoral else Color.Transparent)
                                .clickable { isShort = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "شورتس ⚡",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isShort) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Section 4: Interactive Video Preview Player Box
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "معاينة الفيديو قبل النشر:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isExtractingMetadata) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Text("جارٍ قراءة الملف...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isShort) Modifier.height(290.dp)
                                    else Modifier.aspectRatio(16f / 9f)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .testTag("upload_preview_player")
                        ) {
                            VideoPlayerView(
                                controller = viewModel.playerController,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Section 5: Metadata Form (Title, Description, Category, Tags)
                // 5a. Title Field
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                if (it.length <= 100) title = it
                            },
                            label = { Text("عنوان ${if (isShort) "المقطع القصير" else "الفيديو"} (مطلوب) *") },
                            placeholder = { Text("مثال: تجربة مذهلة للذكاء الاصطناعي...") },
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (title.isBlank()) "حقل العنوان مطلوب للحفظ في Room" else "العنوان ممتاز وواضح",
                                        color = if (title.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Text("${title.length}/100")
                                }
                            },
                            isError = title.isBlank() && uploadStatusMessage != null,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_title_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // 5b. Description Field
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                if (it.length <= 500) description = it
                            },
                            label = { Text("الوصف والتفاصيل الوصفية (Metadata)") },
                            placeholder = { Text("اكتب نبذة شيقة، روابط، أو تفاصيل حول هذا الفيديو...") },
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("يدعم الروابط والهاشتاغات")
                                    Text("${description.length}/500")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_desc_input"),
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Quick Hashtag Chips
                        Text(
                            text = "إضافة هاشتاغ سريع للوصف:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(popularHashtags) { tag ->
                                SuggestionChip(
                                    onClick = {
                                        description = if (description.contains(tag)) description else "$description $tag".trim()
                                    },
                                    label = { Text(tag, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }
                    }
                }

                // 5c. Category Selection
                item {
                    Column {
                        Text(
                            text = "التصنيف:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(videoCategories.filter { it != "الكل" }) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }
                    }
                }

                // 5d. Tags & Sound
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            label = { Text("الكلمات المفتاحية") },
                            placeholder = { Text("تقنية, هاتف, أندرويد") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = soundTrackTitle,
                            onValueChange = { soundTrackTitle = it },
                            label = { Text("المقطع الصوتي") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                // 5e. Privacy Switch (عام / خاص)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isPublic) "محتوى عام (Public)" else "محتوى خاص (Private)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isPublic) "يظهر في الصفحة الرئيسية وقوائم البحث للجميع" else "يُحفظ في جهازك وقاعدة بيانات Room فقط بدون نشر عام",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it },
                            modifier = Modifier.testTag("upload_privacy_switch")
                        )
                    }
                }

                // Status message banner
                uploadStatusMessage?.let { msg ->
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (msg.contains("يرجى") || msg.contains("تعذر"))
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (msg.contains("يرجى") || msg.contains("تعذر"))
                                        Icons.Default.ErrorOutline
                                    else
                                        Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (msg.contains("يرجى") || msg.contains("تعذر"))
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = msg,
                                    color = if (msg.contains("يرجى") || msg.contains("تعذر"))
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else
                                        MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Section 6: Save & Publish Action Button (Room Persistence)
                item {
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                uploadStatusMessage = "يرجى كتابة عنوان للفيديو أولاً للمتابعة!"
                                return@Button
                            }

                            isPublishing = true
                            uploadStatusMessage = null

                            coroutineScope.launch {
                                try {
                                    publishingStep = "جارٍ استخراج وتأكيد البيانات الوصفية..."
                                    delay(400)
                                    publishingStep = "حفظ السجل في قاعدة البيانات المحلية (Room)..."
                                    delay(400)

                                    val finalDuration = if (isShort) {
                                        if (videoDurationSeconds > 60) 60 else videoDurationSeconds
                                    } else {
                                        videoDurationSeconds
                                    }

                                    viewModel.uploadNewVideo(
                                        title = title.trim(),
                                        description = description.trim(),
                                        videoUrl = selectedVideoUrl,
                                        thumbnailUrl = selectedThumbnailUrl,
                                        durationSeconds = finalDuration,
                                        category = selectedCategory,
                                        tags = tags.trim(),
                                        isShort = isShort,
                                        isPublic = isPublic,
                                        soundTrackTitle = soundTrackTitle.trim()
                                    ).join()

                                    lastUploadedVideoTitle = title.trim()
                                    isPublishing = false
                                    showSuccessDialog = true
                                } catch (e: Exception) {
                                    isPublishing = false
                                    uploadStatusMessage = "تعذر حفظ الفيديو في Room: ${e.message ?: "يرجى المحاولة مجدداً"}"
                                }
                            }
                        },
                        enabled = !isPublishing && title.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("publish_video_button"),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isShort) VidoCoral else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(publishingStep, fontSize = 14.sp)
                        } else {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isShort) "حفظ المقطع القصير في Room ⚡" else "حفظ ونشر الفيديو في Room 🚀",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Success Dialog on Room Local Persistence
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00C853).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "تم الحفظ بنجاح في قاعدة البيانات!",
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "تم حفظ الفيديو \"$lastUploadedVideoTitle\" وبياناته الوصفية في قاعدة البيانات المحلية (Room) بنجاح.",
                        fontSize = 13.sp
                    )
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• التصنيف: $selectedCategory", fontSize = 12.sp)
                            Text("• النوع: ${if (isShort) "مقطع قصير (Shorts)" else "فيديو طويل"}", fontSize = 12.sp)
                            Text("• الخصوصية: ${if (isPublic) "عام" else "خاص"}", fontSize = 12.sp)
                            Text("• حالة التخزين: تم إدراج الكيان في جدول videos بنجاح", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onUploadSuccess()
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("مشاهدة في الصفحة الرئيسية")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSuccessDialog = false
                        selectedScreenTab = 1 // Switch to Room videos list
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("عرض في قائمة Room")
                }
            }
        )
    }

    // Delete confirmation dialog
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            icon = {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("حذف الفيديو من Room؟") },
            text = {
                Text("هل أنت متأكد من رغبتك في إزالة \"${video.title}\" من قاعدة البيانات المحلية؟ لن تتمكن من استرجاعه لاحقاً.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUploadedVideo(video.id)
                        videoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("نعم، احذف")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { videoToDelete = null },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun TechnicalInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun LocalUploadedVideoItemCard(
    video: VideoEntity,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(video.uploadTimestamp) { dateFormat.format(Date(video.uploadTimestamp)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("local_video_item_${video.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with duration overlay
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 75.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .clickable { onPlayClick() }
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "تشغيل",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Duration badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    val m = video.durationSeconds / 60
                    val s = video.durationSeconds % 60
                    Text(
                        text = "%02d:%02d".format(m, s),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (video.description.isNotBlank()) {
                    Text(
                        text = video.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(video.category, fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                    Text(
                        text = if (video.isShort) "شورتس ⚡" else "فيديو 📺",
                        fontSize = 10.sp,
                        color = if (video.isShort) VidoCoral else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions (Delete)
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف من Room",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
