package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.player.VideoPlayerView
import com.example.ui.components.videoCategories
import com.example.ui.theme.VidoCoral
import com.example.ui.theme.VidoPurple
import com.example.ui.viewmodel.VidoMixViewModel

data class VideoPreset(
    val name: String,
    val url: String,
    val thumb: String,
    val duration: Int,
    val category: String
)

val samplePresets = listOf(
    VideoPreset(
        name = "تقنية وذكاء اصطناعي",
        url = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4",
        thumb = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
        duration = 340,
        category = "تقنية"
    ),
    VideoPreset(
        name = "جيمينج وتحديات",
        url = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_1MB.mp4",
        thumb = "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=600&auto=format&fit=crop&q=80",
        duration = 210,
        category = "ألعاب"
    ),
    VideoPreset(
        name = "طبيعة وسفر",
        url = "https://filesamples.com/samples/video/mp4/sample_960x400_ocean_with_audio.mp4",
        thumb = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80",
        duration = 590,
        category = "ترفيه"
    ),
    VideoPreset(
        name = "رياضة وأكشن",
        url = "https://filesamples.com/samples/video/mp4/sample_1280x720.mp4",
        thumb = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600&auto=format&fit=crop&q=80",
        duration = 180,
        category = "رياضة"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: VidoMixViewModel,
    onUploadSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var isShort by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("تقنية") }
    var isPublic by remember { mutableStateOf(true) }

    var selectedVideoUrl by remember { mutableStateOf(samplePresets.first().url) }
    var selectedThumbnailUrl by remember { mutableStateOf(samplePresets.first().thumb) }
    var videoDuration by remember { mutableIntStateOf(samplePresets.first().duration) }
    var isPublishing by remember { mutableStateOf(false) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Media Picker for device gallery
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUrl = uri.toString()
            uploadStatusMessage = "تم اختيار الفيديو من الجهاز بنجاح!"
        }
    }

    LaunchedEffect(selectedVideoUrl) {
        viewModel.playerController.prepareAndPlay(selectedVideoUrl, autoPlay = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "نشر محتوى جديد",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
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
            // Type Selector: فيديو طويل vs مقطع قصير
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
                            text = "مقطع قصير (Short) ⚡",
                            fontWeight = FontWeight.Bold,
                            color = if (isShort) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Video Preview Player Box
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "معاينة الفيديو قبل النشر (Preview):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isShort) Modifier.height(280.dp)
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

            // Video Source Selector (Gallery or Sample Presets)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اختر مصدر الفيديو:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        OutlinedButton(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("pick_from_device_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("من الهاتف", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset buttons
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(samplePresets) { preset ->
                            val isSelected = selectedVideoUrl == preset.url
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    selectedVideoUrl = preset.url
                                    selectedThumbnailUrl = preset.thumb
                                    videoDuration = preset.duration
                                    selectedCategory = preset.category
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = preset.thumb,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(preset.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }

            // Title Field
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان ${if (isShort) "المقطع القصير" else "الفيديو"} *") },
                    placeholder = { Text("مثال: تجربة مذهلة للذكاء الاصطناعي...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Description Field
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف والتفاصيل") },
                    placeholder = { Text("اكتب نبذة شيقة للمشاهدين...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_desc_input"),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Category Selection
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

            // Tags Field
            item {
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("الهاشتاغات والكلمات المفتاحية") },
                    placeholder = { Text("مثال: تقنية, برمجة, 2026") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Privacy Switch (عام / خاص)
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
                            text = if (isPublic) "يمكن لجميع مستخدمي VidoMix مشاهدة الفيديو" else "أنت فقط من يرى هذا الفيديو",
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

            // Status feedback message
            uploadStatusMessage?.let { msg ->
                item {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Publish Button
            item {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            uploadStatusMessage = "يرجى كتابة عنوان للفيديو أولاً!"
                            return@Button
                        }
                        isPublishing = true
                        uploadStatusMessage = null
                        coroutineScope.launch {
                            try {
                                viewModel.uploadNewVideo(
                                    title = title,
                                    description = description,
                                    videoUrl = selectedVideoUrl,
                                    thumbnailUrl = selectedThumbnailUrl,
                                    durationSeconds = if (isShort) 30 else videoDuration,
                                    category = selectedCategory,
                                    tags = tags,
                                    isShort = isShort,
                                    isPublic = isPublic
                                ).join()
                                isPublishing = false
                                onUploadSuccess()
                            } catch (e: Exception) {
                                isPublishing = false
                                uploadStatusMessage = "تعذر نشر الفيديو: ${e.message ?: "يرجى التحقق من الاتصال والمحاولة لاحقاً"}"
                            }
                        }
                    },
                    enabled = !isPublishing && title.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("publish_video_button"),
                    shape = RoundedCornerShape(26.dp),
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جارٍ النشر والمعالجة...")
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isShort) "نشر المقطع القصير الآن ⚡" else "نشر الفيديو الآن 🚀",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
