package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val viewsCount: Long = 0,
    val likesCount: Long = 0,
    val commentsCount: Int = 0,
    val category: String = "عام", // ألعاب, رياضة, موسيقى, أفلام, تقنية, تعليم, ترفيه
    val tags: String = "",
    val isShort: Boolean = false,
    val isPublic: Boolean = true,
    val uploadTimestamp: Long = System.currentTimeMillis(),
    val qualityOptions: String = "1080p, 720p, 480p, Auto",
    val soundTrackTitle: String = "الصوت الأصلي - VidoMix"
)
