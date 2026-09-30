package com.example.data.repository

import com.example.data.local.VidoMixDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class VidoMixRepository(
    val dao: VidoMixDao,
    val authManager: AuthManager,
    val recommendationEngine: RecommendationEngine
) {

    // --- Content Feeds ---
    fun getLongVideos(): Flow<List<VideoEntity>> = dao.getVideos(isShort = false)
    fun getTrendingLongVideos(): Flow<List<VideoEntity>> = dao.getTrendingVideos(isShort = false)
    fun getVideosByCategory(cat: String): Flow<List<VideoEntity>> = dao.getVideosByCategory(cat, isShort = false)

    fun getShorts(): Flow<List<VideoEntity>> = dao.getVideos(isShort = true)
    fun getTrendingShorts(): Flow<List<VideoEntity>> = dao.getTrendingVideos(isShort = true)

    fun getVideoById(id: String): Flow<VideoEntity?> = dao.getVideoById(id)
    suspend fun getVideoByIdDirect(id: String): VideoEntity? = dao.getVideoByIdDirect(id)

    fun getVideosByChannel(channelId: String, isShort: Boolean): Flow<List<VideoEntity>> =
        dao.getVideosByChannel(channelId, isShort)

    // --- Channel & Creator ---
    fun getChannelById(id: String): Flow<ChannelEntity?> = dao.getChannelById(id)
    suspend fun getChannelByIdDirect(id: String): ChannelEntity? = dao.getChannelByIdDirect(id)
    fun getChannelByUserId(userId: String): Flow<ChannelEntity?> = dao.getChannelByUserId(userId)
    fun getAllChannels(): Flow<List<ChannelEntity>> = dao.getAllChannels()

    // --- Interactions ---
    fun isLiked(userId: String, targetType: String, targetId: String): Flow<Boolean> =
        dao.isLikedFlow(userId, targetType, targetId)

    suspend fun toggleLike(userId: String, targetType: String, targetId: String): Boolean {
        val existing = dao.getLike(userId, targetType, targetId)
        return if (existing != null) {
            dao.deleteLike(userId, targetType, targetId)
            if (targetType == "VIDEO" || targetType == "SHORT") {
                dao.decrementLikes(targetId)
            }
            false
        } else {
            val newLike = LikeEntity(
                id = "like_" + UUID.randomUUID().toString().take(8),
                userId = userId,
                targetType = targetType,
                targetId = targetId
            )
            dao.insertLike(newLike)
            if (targetType == "VIDEO" || targetType == "SHORT") {
                dao.incrementLikes(targetId)
                // Add notification to video creator
                val video = dao.getVideoByIdDirect(targetId)
                if (video != null) {
                    val channel = dao.getChannelByIdDirect(video.channelId)
                    if (channel != null && channel.userId != userId) {
                        dao.insertNotification(
                            NotificationEntity(
                                id = "notif_" + UUID.randomUUID().toString().take(8),
                                userId = channel.userId,
                                type = "LIKE",
                                title = "إعجاب جديد",
                                message = "أعجب شخص ما بـ ${if (video.isShort) "المقطع القصير" else "الفيديو"}: ${video.title.take(20)}...",
                                targetId = targetId
                            )
                        )
                    }
                }
            }
            true
        }
    }

    fun isFollowing(followerUserId: String, channelId: String): Flow<Boolean> =
        dao.isFollowing(followerUserId, channelId)

    suspend fun toggleFollow(followerUserId: String, channelId: String): Boolean {
        val existing = dao.getFollow(followerUserId, channelId)
        return if (existing != null) {
            dao.deleteFollow(followerUserId, channelId)
            dao.decrementChannelSubscribers(channelId)
            false
        } else {
            val follow = FollowEntity(
                id = "flw_" + UUID.randomUUID().toString().take(8),
                followerUserId = followerUserId,
                followedChannelId = channelId
            )
            dao.insertFollow(follow)
            dao.incrementChannelSubscribers(channelId)

            val channel = dao.getChannelByIdDirect(channelId)
            if (channel != null && channel.userId != followerUserId) {
                dao.insertNotification(
                    NotificationEntity(
                        id = "notif_" + UUID.randomUUID().toString().take(8),
                        userId = channel.userId,
                        type = "NEW_FOLLOWER",
                        title = "متابع جديد!",
                        message = "لديك مشترك جديد في قناتك ${channel.name}",
                        targetId = followerUserId
                    )
                )
            }
            true
        }
    }

    fun isSaved(userId: String, videoId: String): Flow<Boolean> =
        dao.isVideoSaved(userId, videoId)

    suspend fun toggleSave(userId: String, videoId: String): Boolean {
        val count = dao.isVideoSaved(userId, videoId)
        val isAlreadySaved = dao.getVideoByIdDirect(videoId) != null // will query directly
        val existing = dao.getSavedVideosForUser(userId) // check
        // We can safely try insert or delete
        val savedId = "saved_${userId}_${videoId}"
        return try {
            val isSavedNow = dao.isVideoSaved(userId, videoId)
            // Perform delete or insert
            val entity = SavedVideoEntity(id = savedId, userId = userId, videoId = videoId)
            dao.insertSavedVideo(entity)
            true
        } catch (e: Exception) {
            dao.deleteSavedVideo(userId, videoId)
            false
        }
    }

    suspend fun removeSavedVideo(userId: String, videoId: String) {
        dao.deleteSavedVideo(userId, videoId)
    }

    fun getSavedVideos(userId: String): Flow<List<VideoEntity>> =
        dao.getSavedVideosForUser(userId)

    // --- Watch History ---
    suspend fun recordWatch(userId: String, videoId: String, watchedSeconds: Int, completed: Boolean) {
        dao.incrementViews(videoId)
        dao.recordWatchHistory(
            WatchHistoryEntity(
                id = "watch_${userId}_${videoId}",
                userId = userId,
                videoId = videoId,
                watchedSeconds = watchedSeconds,
                completed = completed
            )
        )
    }

    fun getWatchHistory(userId: String): Flow<List<VideoEntity>> =
        dao.getWatchHistoryVideos(userId)

    suspend fun clearWatchHistory(userId: String) = dao.clearWatchHistory(userId)

    // --- Comments ---
    fun getComments(videoId: String): Flow<List<CommentEntity>> =
        dao.getCommentsForVideo(videoId)

    fun getCommentReplies(parentId: String): Flow<List<CommentEntity>> =
        dao.getRepliesForComment(parentId)

    suspend fun addComment(videoId: String, userId: String, content: String, parentId: String? = null): CommentEntity {
        val comment = CommentEntity(
            id = "comm_" + UUID.randomUUID().toString().take(8),
            videoId = videoId,
            userId = userId,
            parentCommentId = parentId,
            content = content.trim(),
            likesCount = 0
        )
        dao.insertComment(comment)
        dao.incrementCommentsCount(videoId)

        val video = dao.getVideoByIdDirect(videoId)
        if (video != null) {
            val channel = dao.getChannelByIdDirect(video.channelId)
            if (channel != null && channel.userId != userId) {
                dao.insertNotification(
                    NotificationEntity(
                        id = "notif_" + UUID.randomUUID().toString().take(8),
                        userId = channel.userId,
                        type = if (parentId != null) "REPLY" else "COMMENT",
                        title = if (parentId != null) "رد جديد على تعليق" else "تعليق جديد على الفيديو",
                        message = content.take(30),
                        targetId = videoId
                    )
                )
            }
        }
        return comment
    }

    // --- Upload ---
    suspend fun uploadVideo(
        channelId: String,
        title: String,
        description: String,
        videoUrl: String,
        thumbnailUrl: String,
        durationSeconds: Int,
        category: String,
        tags: String,
        isShort: Boolean,
        isPublic: Boolean,
        soundTrackTitle: String = "الصوت الأصلي - VidoMix"
    ): VideoEntity {
        val newVideo = VideoEntity(
            id = (if (isShort) "short_" else "vid_") + UUID.randomUUID().toString().take(8),
            channelId = channelId,
            title = title.trim(),
            description = description.trim(),
            videoUrl = videoUrl.trim(),
            thumbnailUrl = thumbnailUrl.trim().ifEmpty {
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80"
            },
            durationSeconds = durationSeconds,
            viewsCount = 1,
            likesCount = 0,
            commentsCount = 0,
            category = category,
            tags = tags.trim(),
            isShort = isShort,
            isPublic = isPublic,
            soundTrackTitle = soundTrackTitle
        )
        dao.insertVideo(newVideo)

        // Increment channel video count
        val channel = dao.getChannelByIdDirect(channelId)
        if (channel != null) {
            dao.updateChannel(channel.copy(videoCount = channel.videoCount + 1))
        }

        return newVideo
    }

    // --- Search ---
    fun searchVideos(query: String): Flow<List<VideoEntity>> = dao.searchVideos(query)
    fun searchChannels(query: String): Flow<List<ChannelEntity>> = dao.searchChannels(query)
    fun getRecentSearches(userId: String): Flow<List<SearchHistoryEntity>> = dao.getRecentSearches(userId)
    suspend fun saveSearchQuery(userId: String, query: String) {
        if (query.isNotBlank()) {
            dao.insertSearch(SearchHistoryEntity(userId = userId, query = query.trim()))
        }
    }
    suspend fun deleteSearch(id: Long) = dao.deleteSearch(id)
    suspend fun clearSearchHistory(userId: String) = dao.clearSearchHistory(userId)

    // --- Notifications ---
    fun getNotifications(userId: String): Flow<List<NotificationEntity>> =
        dao.getNotificationsForUser(userId)

    suspend fun markNotificationAsRead(id: String) = dao.markNotificationAsRead(id)
    suspend fun clearNotifications(userId: String) = dao.clearNotifications(userId)

    // --- Reports ---
    fun getAllReports(): Flow<List<ReportEntity>> = dao.getAllReports()
    suspend fun submitReport(
        reporterUserId: String,
        reportType: String,
        targetId: String,
        reason: String,
        details: String
    ) {
        val report = ReportEntity(
            id = "rep_" + UUID.randomUUID().toString().take(8),
            reporterUserId = reporterUserId,
            reportType = reportType,
            targetId = targetId,
            reason = reason,
            details = details.trim()
        )
        dao.insertReport(report)
    }

    suspend fun updateReportStatus(reportId: String, status: String) {
        dao.updateReportStatus(reportId, status)
    }

    // --- Admin / Moderation ---
    fun getAllUsers(): Flow<List<UserEntity>> = dao.getAllUsers()
    suspend fun deleteVideo(videoId: String) = dao.deleteVideoById(videoId)

    // --- Monetization & Ad Revenue ---
    fun getMonetizationProfile(userId: String): Flow<MonetizationProfileEntity?> =
        dao.getMonetizationProfile(userId)

    suspend fun getMonetizationProfileDirect(userId: String): MonetizationProfileEntity? =
        dao.getMonetizationProfileDirect(userId)

    fun getAllMonetizationProfiles(): Flow<List<MonetizationProfileEntity>> =
        dao.getAllMonetizationProfiles()

    fun getPlatformSettings(): Flow<PlatformSettingsEntity?> =
        dao.getPlatformSettings()

    suspend fun getPlatformSettingsDirect(): PlatformSettingsEntity =
        dao.getPlatformSettingsDirect() ?: PlatformSettingsEntity()

    suspend fun updatePlatformSettings(settings: PlatformSettingsEntity) =
        dao.insertPlatformSettings(settings)

    suspend fun submitMonetizationApplication(userId: String) {
        val user = dao.getUserByIdDirect(userId) ?: return
        val channel = dao.getChannelByUserIdDirect(userId)
        val profile = dao.getMonetizationProfileDirect(userId) ?: MonetizationProfileEntity(
            userId = userId,
            channelId = channel?.id ?: "chan_$userId",
            followersCount = user.followersCount
        )
        dao.insertMonetizationProfile(
            profile.copy(
                status = "PENDING_REVIEW",
                appliedAt = System.currentTimeMillis(),
                rejectionReason = ""
            )
        )
    }

    suspend fun reviewMonetization(userId: String, approve: Boolean, reason: String = "") {
        val profile = dao.getMonetizationProfileDirect(userId) ?: return
        dao.insertMonetizationProfile(
            profile.copy(
                status = if (approve) "APPROVED" else "REJECTED",
                tier = if (approve) 2 else 0,
                reviewedAt = System.currentTimeMillis(),
                rejectionReason = reason
            )
        )
    }

    suspend fun suspendMonetization(userId: String, reason: String) {
        val profile = dao.getMonetizationProfileDirect(userId) ?: return
        dao.insertMonetizationProfile(
            profile.copy(
                status = "SUSPENDED",
                rejectionReason = reason
            )
        )
    }

    suspend fun recordAdImpression(
        impressionId: String,
        adUnitId: String,
        videoId: String?,
        viewerId: String?,
        adFormat: String,
        revenueValue: Double,
        currency: String = "USD",
        precision: String = "ESTIMATED",
        country: String = "SA"
    ): AdImpressionEntity {
        // 1. Determine creator ID from video
        var creatorId: String? = null
        if (videoId != null) {
            val video = dao.getVideoByIdDirect(videoId)
            if (video != null) {
                val channel = dao.getChannelByIdDirect(video.channelId)
                creatorId = channel?.userId
            }
        }

        // 2. Anti-Fraud check
        val fraudResult = com.example.ads.AntiFraudEngine.evaluateImpression(
            impressionId = impressionId,
            viewerId = viewerId,
            creatorId = creatorId,
            dao = dao
        )

        val impressionStatus = when (fraudResult) {
            com.example.ads.AntiFraudEngine.FraudCheckResult.VALID -> "VERIFIED"
            com.example.ads.AntiFraudEngine.FraudCheckResult.CREATOR_SELF_VIEW -> "SELF_VIEW"
            com.example.ads.AntiFraudEngine.FraudCheckResult.BURST_RATE_DETECTED -> "SUSPICIOUS"
            com.example.ads.AntiFraudEngine.FraudCheckResult.DUPLICATE_EVENT -> "DUPLICATE"
            else -> "HELD_FOR_REVIEW"
        }

        // 3. Check creator monetization eligibility (Must be APPROVED)
        val settings = getPlatformSettingsDirect()
        var isEligible = false
        var creatorShare = 0.0
        var platformShare = revenueValue

        if (creatorId != null && fraudResult == com.example.ads.AntiFraudEngine.FraudCheckResult.VALID) {
            val profile = dao.getMonetizationProfileDirect(creatorId)
            if (profile != null && profile.status == "APPROVED") {
                isEligible = true
                creatorShare = revenueValue * (settings.creatorSharePercent / 100.0)
                platformShare = revenueValue * (settings.platformSharePercent / 100.0)

                // Credit creator balance
                val updatedProfile = profile.copy(
                    currentBalance = profile.currentBalance + creatorShare,
                    lifetimeEarnings = profile.lifetimeEarnings + creatorShare,
                    adImpressionsCount = profile.adImpressionsCount + 1,
                    monetizedPlaybacksCount = profile.monetizedPlaybacksCount + 1
                )
                dao.updateMonetizationProfile(updatedProfile)
            }
        }

        val impressionEntity = AdImpressionEntity(
            id = "imp_rec_" + UUID.randomUUID().toString().take(10),
            impressionId = impressionId,
            adUnitId = adUnitId,
            videoId = videoId,
            creatorId = creatorId,
            viewerId = viewerId,
            adFormat = adFormat,
            timestamp = System.currentTimeMillis(),
            revenueValue = revenueValue,
            currency = currency,
            precision = precision,
            country = country,
            platformShareAmount = platformShare,
            creatorShareAmount = creatorShare,
            isCreatorEligible = isEligible,
            status = impressionStatus
        )
        dao.recordAdImpression(impressionEntity)
        return impressionEntity
    }

    // --- Payouts ---
    fun getPayoutsForUser(userId: String): Flow<List<PayoutEntity>> = dao.getPayoutsForUser(userId)
    fun getAllPayouts(): Flow<List<PayoutEntity>> = dao.getAllPayouts()

    suspend fun requestPayout(
        userId: String,
        amount: Double,
        method: String,
        details: String
    ): Result<PayoutEntity> {
        val profile = dao.getMonetizationProfileDirect(userId)
            ?: return Result.failure(Exception("ملف تحقيق الربح غير متوفر"))

        if (profile.status != "APPROVED") {
            return Result.failure(Exception("الحساب غير معتمد لسحب الأرباح أو معلق"))
        }

        val settings = getPlatformSettingsDirect()
        if (amount < settings.minPayoutThreshold) {
            return Result.failure(Exception("الحد الأدنى للسحب هو $${settings.minPayoutThreshold}"))
        }

        if (profile.currentBalance < amount) {
            return Result.failure(Exception("الرصيد المتاح غير كافٍ ($${String.format("%.2f", profile.currentBalance)})"))
        }

        // Deduct from current balance, increase pending balance
        dao.updateMonetizationProfile(
            profile.copy(
                currentBalance = profile.currentBalance - amount,
                pendingBalance = profile.pendingBalance + amount
            )
        )

        val payout = PayoutEntity(
            id = "pay_" + UUID.randomUUID().toString().take(8),
            userId = userId,
            amount = amount,
            method = method,
            payoutDetails = details.trim(),
            status = "PENDING"
        )
        dao.insertPayout(payout)
        return Result.success(payout)
    }

    suspend fun updatePayoutStatus(payout: PayoutEntity, newStatus: String, notes: String = "") {
        val updated = payout.copy(
            status = newStatus,
            processedAt = System.currentTimeMillis(),
            adminNotes = notes
        )
        dao.updatePayout(updated)

        val profile = dao.getMonetizationProfileDirect(payout.userId)
        if (profile != null) {
            if (newStatus == "PAID") {
                // Clear from pending balance
                val newPending = (profile.pendingBalance - payout.amount).coerceAtLeast(0.0)
                dao.updateMonetizationProfile(profile.copy(pendingBalance = newPending))
            } else if (newStatus == "REJECTED" || newStatus == "FAILED") {
                // Refund back to current balance
                val newPending = (profile.pendingBalance - payout.amount).coerceAtLeast(0.0)
                dao.updateMonetizationProfile(
                    profile.copy(
                        currentBalance = profile.currentBalance + payout.amount,
                        pendingBalance = newPending
                    )
                )
            }
        }
    }

    fun getFlaggedImpressions(): Flow<List<AdImpressionEntity>> = dao.getFlaggedImpressions()
    fun getRecentImpressions(limit: Int = 100): Flow<List<AdImpressionEntity>> = dao.getRecentImpressions(limit)
    fun getImpressionsForCreator(creatorId: String): Flow<List<AdImpressionEntity>> = dao.getImpressionsForCreator(creatorId)
}
