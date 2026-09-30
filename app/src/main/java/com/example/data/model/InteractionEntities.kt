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
    val reportType: String, // "VIDEO", "USER", "COMMENT"
    val targetId: String,
    val reason: String, // "INAPPROPRIATE", "COPYRIGHT", "FRAUD", "ABUSE", "OTHER"
    val details: String,
    val status: String = "PENDING", // "PENDING", "RESOLVED", "DISMISSED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
