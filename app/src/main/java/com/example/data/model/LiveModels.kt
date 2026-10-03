package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "live_streams")
data class LiveStreamEntity(
    @PrimaryKey val id: String,
    val hostUserId: String,
    val hostUsername: String,
    val hostFullName: String,
    val hostAvatarUrl: String,
    val title: String,
    val coverUrl: String,
    val category: String, // "ألعاب", "تقنية", "دردشة", "ترفيه", "رياضة", "تعليم", "موسيقى"
    val streamUrl: String,
    val status: String = "LIVE", // "LIVE", "ENDED", "BANNED"
    val viewersCount: Int = 1,
    val peakViewers: Int = 1,
    val likesCount: Int = 0,
    val totalGiftsCount: Int = 0,
    val totalCoinsEarned: Long = 0L,
    val commentsAllowed: Boolean = true,
    val giftsAllowed: Boolean = true,
    val isMicEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val pinnedCommentId: String? = null,
    val pinnedCommentText: String? = null,
    val pinnedCommentUser: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null
)

@Entity(tableName = "live_comments")
data class LiveCommentEntity(
    @PrimaryKey val id: String,
    val streamId: String,
    val userId: String,
    val username: String,
    val userAvatarUrl: String,
    val text: String,
    val isPinned: Boolean = false,
    val isHost: Boolean = false,
    val isModerator: Boolean = false,
    val isSystemNotification: Boolean = false, // e.g. "انضم إلى البث"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "live_gifts")
data class LiveGiftEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emojiIcon: String,
    val animationType: String, // "HEART_BURST", "ROSE_SHOWER", "STAR_FALL", "DIAMOND_GLOW", "CROWN_ROYAL", "ROCKET_LAUNCH", "CASTLE_EPIC"
    val coinPrice: Long,
    val isEnabled: Boolean = true,
    val displayOrder: Int = 0
)

@Entity(tableName = "live_gift_transactions")
data class LiveGiftTransactionEntity(
    @PrimaryKey val id: String,
    val streamId: String,
    val senderUserId: String,
    val senderUsername: String,
    val senderAvatarUrl: String,
    val hostUserId: String,
    val giftId: String,
    val giftName: String,
    val giftIcon: String,
    val giftCoinPrice: Long,
    val quantity: Int = 1,
    val totalCoins: Long,
    val hostEarningsAmount: Double, // in USD
    val platformFeeAmount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "coin_packages")
data class CoinPackageEntity(
    @PrimaryKey val id: String,
    val coinsAmount: Long,
    val bonusCoins: Long = 0L,
    val priceUsd: Double,
    val title: String,
    val isPopular: Boolean = false,
    val isEnabled: Boolean = true
)

@Entity(tableName = "coin_purchase_orders")
data class CoinPurchaseOrderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val packageId: String,
    val coinsAmount: Long,
    val pricePaidUsd: Double,
    val paymentMethod: String = "Google Play Billing",
    val status: String = "COMPLETED", // "COMPLETED", "FAILED", "PENDING"
    val serverVerificationToken: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "live_muted_users")
data class LiveMutedUserEntity(
    @PrimaryKey val id: String,
    val streamId: String,
    val userId: String,
    val username: String,
    val mutedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "live_banned_viewers")
data class LiveBannedViewerEntity(
    @PrimaryKey val id: String,
    val streamId: String,
    val userId: String,
    val username: String,
    val bannedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "live_viewer_sessions")
data class LiveViewerSessionEntity(
    @PrimaryKey val id: String,
    val streamId: String,
    val userId: String,
    val username: String,
    val avatarUrl: String,
    val role: String = "VIEWER", // "VIEWER", "VIP", "MODERATOR", "HOST"
    val joinedAt: Long = System.currentTimeMillis()
)
