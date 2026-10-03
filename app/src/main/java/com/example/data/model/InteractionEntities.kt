package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "likes")
data class LikeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val targetType: String, // "VIDEO", "SHORT", "COMMENT"
    val targetId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "follows")
data class FollowEntity(
    @PrimaryKey val id: String,
    val followerUserId: String,
    val followedChannelId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_videos")
data class SavedVideoEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val videoId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val videoId: String,
    val watchedSeconds: Int,
    val completed: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // "NEW_FOLLOWER", "LIKE", "COMMENT", "REPLY", "NEW_VIDEO", "SYSTEM"
    val title: String,
    val message: String,
    val targetId: String? = null,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterUserId: String,
    val reportedUserId: String = "",
    val targetId: String,
    val reportType: String, // "VIDEO", "SHORT", "COMMENT", "USER"
    val reason: String, // "عري أو محتوى جنسي", "محتوى عنيف", "خطاب كراهية", "تحرش أو تنمر", "محتوى خطير", "Spam", "احتيال", "انتهاك حقوق الملكية", "محتوى غير قانوني", "سبب آخر"
    val details: String = "",
    val status: String = "PENDING", // "PENDING", "REVIEWING", "CONFIRMED", "REJECTED", "DISMISSED"
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    val decision: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "appeals")
data class AppealEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val reportId: String? = null,
    val contentId: String? = null,
    val contentType: String = "VIDEO", // "VIDEO", "SHORT", "COMMENT", "ACCOUNT"
    val strikeNumber: Int = 1,
    val reason: String,
    val additionalInfo: String = "",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    val reviewNotes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val adminId: String,
    val action: String, // "CONFIRM_VIOLATION", "DISMISS_REPORT", "REMOVE_CONTENT", "HIDE_CONTENT", "ISSUE_WARNING", "BAN_USER", "UNBAN_USER", "APPROVE_APPEAL", "REJECT_APPEAL"
    val targetUserId: String? = null,
    val contentId: String? = null,
    val contentType: String? = null,
    val reason: String,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_users")
data class BlockedUserEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val blockedUserId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "not_interested_videos")
data class NotInterestedEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val videoId: String,
    val category: String,
    val channelId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "disliked_categories")
data class DislikedCategoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)
