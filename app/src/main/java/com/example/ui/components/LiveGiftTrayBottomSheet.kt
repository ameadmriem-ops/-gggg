package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
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
import com.example.data.model.LiveGiftEntity
import com.example.ui.theme.VidoGold
import com.example.ui.theme.VidoPink
import com.example.ui.theme.VidoPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveGiftTrayBottomSheet(
    gifts: List<LiveGiftEntity>,
    userCoinsBalance: Long,
    onDismiss: () -> Unit,
    onSendGift: (giftId: String, quantity: Int) -> Unit,
    onRechargeClick: () -> Unit
) {
    var selectedGiftId by remember { mutableStateOf(gifts.firstOrNull()?.id) }
    var selectedQuantity by remember { mutableIntStateOf(1) }
    val quantities = listOf(1, 5, 10, 99)

    val selectedGift = gifts.find { it.id == selectedGiftId }
    val totalCost = (selectedGift?.coinPrice ?: 0L) * selectedQuantity
    val hasEnoughCoins = userCoinsBalance >= totalCost

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1E1B2E),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header: Coin Balance & Recharge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎁 هدايا البث", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                // Balance Badge with Recharge Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.clickable { onRechargeClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🪙 $userCoinsBalance عملة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VidoGold)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(VidoPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "شحن", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Gifts Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gifts, key = { it.id }) { gift ->
                    val isSelected = gift.id == selectedGiftId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF3B2F63) else Color.White.copy(alpha = 0.05f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, VidoGold) else null,
                        modifier = Modifier
                            .aspectRatio(0.9f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedGiftId = gift.id }
                            .testTag("gift_item_${gift.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(gift.emojiIcon, fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(gift.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("🪙 ${gift.coinPrice}", fontSize = 11.sp, color = VidoGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quantity Selector & Send Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    quantities.forEach { q ->
                        val isQSelected = q == selectedQuantity
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isQSelected) VidoPurple else Color.White.copy(alpha = 0.08f))
                                .clickable { selectedQuantity = q }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("x$q", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isQSelected) Color.White else Color.White.copy(alpha = 0.7f))
                        }
                    }
                }

                // Send Button
                Button(
                    onClick = {
                        if (selectedGiftId != null && hasEnoughCoins) {
                            onSendGift(selectedGiftId!!, selectedQuantity)
                        } else if (!hasEnoughCoins) {
                            onRechargeClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasEnoughCoins) VidoPink else Color(0xFF673AB7)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("send_gift_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasEnoughCoins) "إرسال (🪙 $totalCost)" else "شحن العملات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
