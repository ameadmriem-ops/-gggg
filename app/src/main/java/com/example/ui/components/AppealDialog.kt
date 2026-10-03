package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VidoPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppealDialog(
    strikeNumber: Int,
    contentTitle: String,
    onDismissRequest: () -> Unit,
    onSubmitAppeal: (reason: String, additionalInfo: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var reasonInput by remember { mutableStateOf("") }
    var additionalInfoInput by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = VidoPurple,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "استئناف قرار المخالفة ($strikeNumber من 5)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            if (isSubmitted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "تم تقديم الاستئناف بنجاح",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "سيقوم فريق الإشراف بمراجعة طلبك وإشعارك بالقرار النهائي فور الانتهاء.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "المحتوى المعني: $contentTitle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("سبب الاستئناف") },
                        placeholder = { Text("وضح لماذا تعتقد أن هذا القرار تم عن طريق الخطأ...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("appeal_reason_input"),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = additionalInfoInput,
                        onValueChange = { additionalInfoInput = it },
                        label = { Text("معلومات إضافية أو مراجع (اختياري)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("appeal_extra_info_input"),
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("إغلاق")
                }
            } else {
                Button(
                    onClick = {
                        isSubmitted = true
                        onSubmitAppeal(reasonInput, additionalInfoInput)
                    },
                    enabled = reasonInput.isNotBlank(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VidoPurple),
                    modifier = Modifier.testTag("submit_appeal_button")
                ) {
                    Text("إرسال طلب الاستئناف", color = Color.White)
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) {
                TextButton(onClick = onDismissRequest) {
                    Text("إلغاء")
                }
            }
        },
        modifier = modifier.testTag("appeal_dialog")
    )
}
