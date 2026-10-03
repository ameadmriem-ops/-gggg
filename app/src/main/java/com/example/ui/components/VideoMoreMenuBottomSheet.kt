package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChannelEntity
import com.example.data.model.UserEntity
import com.example.data.model.VideoEntity
import com.example.ui.theme.VidoCoral
import com.example.ui.theme.VidoCyan
import com.example.ui.theme.VidoPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoMoreMenuBottomSheet(
    video: VideoEntity,
    channel: ChannelEntity?,
    currentUser: UserEntity?,
    isSaved: Boolean,
    onDismiss: () -> Unit,
    onNotInterested: () -> Unit,
    onBlockCreator: (String) -> Unit,
    onReport: () -> Unit,
    onSaveToggle: () -> Unit,
    onDislikeCategory: (String) -> Unit,
    onDownload: () -> Unit,
    onEditVideo: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onToggleVisibility: (Boolean) -> Unit = {},
    onDeleteVideo: () -> Unit = {},
    onViewAnalytics: () -> Unit = {},
    onManageComments: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local state for confirmation dialogs
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val isOwner = currentUser != null && (
        (channel != null && channel.userId == currentUser.id) ||
        (currentUser.id == video.channelId)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        },
        modifier = modifier.testTag("video_more_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Video Header Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 72.dp, height = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = channel?.name ?: "قناة VidoMix",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isOwner) {
                    // --- OWNER ACTIONS ---
                    item {
                        VideoMenuOptionItem(
                            icon = Icons.Outlined.Edit,
                            title = "تعديل الفيديو والعنوان والوصف",
                            subtitle = "تعديل بيانات المقطع والتصنيف والكلمات المفتاحية",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = { showEditDialog = true }
                        )
                    }

                    item {
                        VideoMenuOptionItem(
                            icon = if (video.isPublic) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            title = if (video.isPublic) "إخفاء الفيديو (جعله خاصاً)" else "إتاحة الفيديو (جعله عاماً)",
                            subtitle = if (video.isPublic) "لن يظهر الفيديو في نتائج البحث والتوصيات" else "سيتمكن الجميع من مشاهدة هذا المقطع",
                            tint = VidoCyan,
                            onClick = {
                                onToggleVisibility(!video.isPublic)
                                onDismiss()
                            }
                        )
                    }

                    item {
                        VideoMenuOptionItem(
                            icon = Icons.Outlined.Analytics,
                            title = "إحصائيات واستوديو الفيديو",
                            subtitle = "عرض المشاهدات ومعدل التفاعل والأرباح",
                            tint = Color(0xFF00C853),
                            onClick = {
                                onViewAnalytics()
                                onDismiss()
                            }
                        )
                    }

                    item {
                        VideoMenuOptionItem(
                            icon = Icons.Outlined.Comment,
                            title = "إدارة التعليقات",
                            subtitle = "مراجعة وتصفية تعليقات المتابعين على المقطع",
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                onManageComments()
                                onDismiss()
                            }
                        )
                    }

                    item {
                        VideoMenuOptionItem(
                            icon = Icons.Outlined.Share,
                            title = "مشاركة المقطع",
                            subtitle = "مشاركة رابط المباشر للمقطع",
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                shareVideoLink(context, video)
                                onDismiss()
                            }
                        )
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    item {
                        VideoMenuOptionItem(
                            icon = Icons.Outlined.Delete,
                            title = "حذف الفيديو نهائياً",
                            subtitle = "سيتم حذف الفيديو وجميع التعليقات والإحصائيات الخاصة به",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteConfirmDialog = true }
                        )
                    }

                } else {
                    // --- VIEWER ACTIONS ---
                    item {
                        VideoMenuOptionItem(
                            icon = Icons.Outlined.ThumbDown,
                            title = "غير مهتم",
                            subtitle = "تقليل اقتراح محتوى مشابه لهذا المقطع",
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                onNotInterested()
                                onDismiss()
                            }
                        )
                    }

                    item {
                        VideoMenuOptionItem(
                            icon = Icons.Outlined.Block,
                            title = "حظر الحساب (@${channel?.name ?: "صاحب القناة"})",
                            subtitle = "عدم إظهار فيديوهات وShorts هذا المنشئ لك إطلاقاً",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { showBlockConfirmDialog = true }
                        )
                    }

                    item {
                        VideoMenuOptionItem(
                            icon = Icons.Outlined.ReportProblem,
                            title = "إبلاغ عن محتوى مخالف",
                            subtitle = "عري، عنف، خطاب كراهية، احتيال، حقوق ملكية",
                            tint = VidoCoral,
                            onClick = {
                                onReport()
                                onDismiss()
                            }
                        )
                    }

                    item {
                        VideoMenuOptionItem(
                            icon = Icons.Outlined.Share,
                            title = "مشاركة",
                            subtitle = "نسخ ومشاركة رابط الفيديو عبر التطبيقات",
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                shareVideoLink(context, video)
                                onDismiss()
                            }
                        )
                    }

                    item {
                        VideoMenuOptionItem(
                            icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            title = if (isSaved) "إزالة من المحفوظات" else "حفظ في المحفوظات",
                            subtitle = "إمكانية المشاهدة لاحقاً من صفحة حسابك",
                            tint = if (isSaved) VidoCyan else MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                onSaveToggle()
                                onDismiss()
                            }
                        )
                    }

                    item {
                        VideoMenuOptionItem(
                            icon = Icons.Outlined.NotInterested,
                            title = "لا أريد رؤية هذا النوع من المحتوى",
                            subtitle = "تخصيص التفضيلات وتجنب تصنيف '${video.category}'",
                            tint = VidoPurple,
                            onClick = {
                                onDislikeCategory(video.category)
                                onDismiss()
                            }
                        )
                    }

                    item {
                        VideoMenuOptionItem(
                            icon = Icons.Outlined.Download,
                            title = "تنزيل لمشاهدته بلا إنترنت",
                            subtitle = if (video.isPublic) "تنزيل نسخة مؤقتة داخل التطبيق" else "التنزيل غير متاح لهذا المقطع",
                            tint = if (video.isPublic) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            onClick = {
                                onDownload()
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Block Confirmation Dialog
    if (showBlockConfirmDialog && channel != null) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "هل تريد حظر هذا الحساب؟",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "عند حظر '${channel.name}'، لن تظهر لك فيديوهاته أو مقاطع Shorts الخاصة به في الـFeed أو الاقتراحات مجدداً.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockConfirmDialog = false
                        onBlockCreator(channel.userId)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حظر")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Video Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "حذف الفيديو نهائياً؟",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف '${video.title}'؟ هذا الإجراء نهائي ولا يمكن التراجع عنه.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteVideo()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Video Dialog
    if (showEditDialog) {
        var titleInput by remember { mutableStateOf(video.title) }
        var descriptionInput by remember { mutableStateOf(video.description) }
        var categoryInput by remember { mutableStateOf(video.category) }
        var tagsInput by remember { mutableStateOf(video.tags) }

        val categories = listOf("عام", "ألعاب", "تقنية", "تعليم", "موسيقى", "ترفيه", "رياضة", "أفلام")

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "تعديل بيانات الفيديو",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("عنوان الفيديو", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("الوصف", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("التصنيف:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(4).forEach { cat ->
                            FilterChip(
                                selected = categoryInput == cat,
                                onClick = { categoryInput = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text("الكلمات المفتاحية (Tags)", fontSize = 12.sp) },
                        placeholder = { Text("مثال: تقنية, برمجة, شرح", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (titleInput.isNotBlank()) {
                            onEditVideo(titleInput, descriptionInput, categoryInput, tagsInput)
                            showEditDialog = false
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun VideoMenuOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

private fun shareVideoLink(context: Context, video: VideoEntity) {
    val shareType = if (video.isShort) "short" else "video"
    val shareUrl = "https://vidomix.app/$shareType/${video.id}"
    val shareText = "شاهد هذا المقطع المميز على VidoMix:\n${video.title}\n$shareUrl"

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, video.title)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "مشاركة عبر"))
}
