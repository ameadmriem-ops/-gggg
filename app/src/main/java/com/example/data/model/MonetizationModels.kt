package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monetization_profiles")
data class MonetizationProfileEntity(
    @PrimaryKey val userId: String,
    val channelId: String,
    val status: String = "NOT_ELIGIBLE", // NOT_ELIGIBLE, ELIGIBLE, PENDING_REVIEW, APPROVED, REJECTED, SUSPENDED, DISABLED
    val tier: Int = 0, // 0 = None, 1 = Tier 1 (Basic), 2 = Tier 2 (Revenue Share Partner)
    val followersCount: Int = 0,
    val publicVideosCount: Int = 0,
    val watchHours: Double = 0.0,
    val shortsViews: Long = 0L,
    val appliedAt: Long? = null,
    val reviewedAt: Long? = null,
    val rejectionReason: String = "",
    val currentBalance: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val lifetimeEarnings: Double = 0.0,
    val adImpressionsCount: Long = 0L,
    val monetizedPlaybacksCount: Long = 0L,
    val payoutMethod: String = "BANK_WIRE", // BANK_WIRE, PAYPAL, WISE
    val payoutDetails: String = ""
)

@Entity(tableName = "ad_impressions")
data class AdImpressionEntity(
    @PrimaryKey val id: String,
    val impressionId: String,
    val adUnitId: String,
    val videoId: String?,
    val creatorId: String?,
    val viewerId: String?,
    val adFormat: String, // PRE_ROLL, MID_ROLL, POST_ROLL, SHORTS_FEED
    val adSource: String = "Google AdMob",
    val timestamp: Long = System.currentTimeMillis(),
    val revenueValue: Double = 0.012, // Actual or eCPM based in USD
    val currency: String = "USD",
    val precision: String = "ESTIMATED",
    val country: String = "SA",
    val platformShareAmount: Double = 0.0,
    val creatorShareAmount: Double = 0.0,
    val isCreatorEligible: Boolean = false,
    val status: String = "VERIFIED" // VERIFIED, SUSPICIOUS, HELD_FOR_REVIEW, PAID, SELF_VIEW
)

@Entity(tableName = "payouts")
data class PayoutEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val currency: String = "USD",
    val method: String, // BANK_WIRE, PAYPAL, WISE
    val payoutDetails: String,
    val status: String = "PENDING", // PENDING, PROCESSING, PAID, FAILED, REJECTED
    val requestedAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val adminNotes: String = ""
)

@Entity(tableName = "platform_settings")
data class PlatformSettingsEntity(
    @PrimaryKey val key: String = "global",
    val platformSharePercent: Double = 60.0,
    val creatorSharePercent: Double = 40.0,
    val shortsAdInterval: Int = 3, // Show ad every N shorts
    val midrollEnabled: Boolean = true,
    val minPayoutThreshold: Double = 50.0
)
