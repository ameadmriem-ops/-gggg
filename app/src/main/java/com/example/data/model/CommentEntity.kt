package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val userId: String,
    val parentCommentId: String? = null,
    val content: String,
    val likesCount: Int = 0,
    val moderationStatus: String = "APPROVED", // "APPROVED", "PENDING", "REMOVED"
    val moderationReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
