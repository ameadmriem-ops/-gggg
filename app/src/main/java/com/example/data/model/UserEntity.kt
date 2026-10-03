package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val avatarUrl: String,
    val bio: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val warningCount: Int = 0, // 0 to 5 strikes
    val accountStatus: String = "ACTIVE", // "ACTIVE", "RESTRICTED", "TERMINATED", "BANNED"
    val coinsBalance: Long = 100L,
    val liveRole: String = "USER", // "USER", "STREAMER", "MODERATOR", "ADMIN"
    val liveNotificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
