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

    // --- Publish Content with Auto-Moderation ---
    suspend fun publishVideo(
        channelId: String,
        title: String,
        description: String,
        videoUrl: String,
        thumbnailUrl: String,
        durationSeconds: Int,
        category: String,
        tags: String,
        isShort: Boolean,
        soundTrackTitle: String = "الصوت الأصلي - VidoMix"
    ): Result<VideoEntity> {
        val inspection = com.example.core.ContentModerationEngine.inspectContent(
            title = title,
            description = description,
            tags = tags,
            mediaUrl = videoUrl
        )

        if (inspection.status == "REJECTED") {
            return Result.failure(Exception(inspection.explanation))
        }

        val video = VideoEntity(
            id = (if (isShort) "short_" else "vid_") + UUID.randomUUID().toString().take(8),
            channelId = channelId,
            title = title.trim(),
            description = description.trim(),
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            durationSeconds = durationSeconds,
            category = category,
            tags = tags,
            isShort = isShort,
            isPublic = true,
            uploadTimestamp = System.currentTimeMillis(),
            soundTrackTitle = soundTrackTitle,
            moderationStatus = inspection.status,
            moderationReason = inspection.violationCategory
        )

        dao.insertVideo(video)

        // If flagged as UNDER_REVIEW, create an internal review report
        if (inspection.status == "UNDER_REVIEW") {
            val chan = dao.getChannelByIdDirect(channelId)
            val autoReport = ReportEntity(
                id = "rep_auto_" + UUID.randomUUID().toString().take(8),
                reporterUserId = "SYSTEM_MODERATION",
                reportedUserId = chan?.userId ?: "",
                targetId = video.id,
                reportType = if (isShort) "SHORT" else "VIDEO",
                reason = inspection.violationCategory ?: "مراجعة تلقائية للمحتوى المشبوه",
                details = inspection.explanation,
                status = "PENDING"
            )
            dao.insertReport(autoReport)
        }

        return Result.success(video)
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

    // --- Content Moderation & Reporting System ---
    fun getAllReports(): Flow<List<ReportEntity>> = dao.getAllReports()
    fun getReportsByStatus(status: String): Flow<List<ReportEntity>> = dao.getReportsByStatus(status)
    fun getAllAppeals(): Flow<List<AppealEntity>> = dao.getAllAppeals()
    fun getAppealsForUser(userId: String): Flow<List<AppealEntity>> = dao.getAppealsForUser(userId)
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()

    suspend fun submitReport(
        reporterUserId: String,
        targetId: String,
        reportType: String,
        reason: String,
        details: String
    ): Result<ReportEntity> {
        if (dao.hasUserReportedContent(reporterUserId, targetId)) {
            return Result.failure(Exception("لقد قمت بالإبلاغ عن هذا المحتوى مسبقاً، وهو قيد المراجعة لدى فريق الإدارة."))
        }

        // Determine reported creator/user
        var reportedUserId = ""
        when (reportType) {
            "VIDEO", "SHORT" -> {
                val vid = dao.getVideoByIdDirect(targetId)
                if (vid != null) {
                    val chan = dao.getChannelByIdDirect(vid.channelId)
                    reportedUserId = chan?.userId ?: ""
                }
            }
            "COMMENT" -> {
                val comm = dao.getCommentByIdDirect(targetId)
                reportedUserId = comm?.userId ?: ""
            }
            "USER" -> {
                reportedUserId = targetId
            }
        }

        val report = ReportEntity(
            id = "rep_" + UUID.randomUUID().toString().take(10),
            reporterUserId = reporterUserId,
            reportedUserId = reportedUserId,
            targetId = targetId,
            reportType = reportType,
            reason = reason,
            details = details.trim(),
            status = "PENDING"
        )
        dao.insertReport(report)
        return Result.success(report)
    }

    suspend fun confirmViolation(
        reportId: String,
        adminId: String,
        violationCategory: String,
        takeDownContent: Boolean = true
    ): Result<Unit> {
        val report = dao.getReportByIdDirect(reportId)
            ?: return Result.failure(Exception("البلاغ غير موجود"))

        // 1. Mark report as CONFIRMED
        val updatedReport = report.copy(
            status = "CONFIRMED",
            reviewedBy = adminId,
            reviewedAt = System.currentTimeMillis(),
            decision = "CONFIRMED: $violationCategory"
        )
        dao.updateReport(updatedReport)

        // 2. Increment strike count for the creator
        val creator = if (report.reportedUserId.isNotBlank()) dao.getUserByIdDirect(report.reportedUserId) else null
        var newStrike = 1
        var newStatus = "ACTIVE"

        if (creator != null) {
            newStrike = (creator.warningCount + 1).coerceAtMost(5)
            newStatus = if (newStrike >= 5) "TERMINATED" else if (newStrike >= 3) "RESTRICTED" else "ACTIVE"
            dao.updateUserWarningAndStatus(creator.id, newStrike, newStatus)

            // Send official violation notification
            val notif = NotificationEntity(
                id = "notif_violation_" + UUID.randomUUID().toString().take(8),
                userId = creator.id,
                type = "SYSTEM",
                title = "⚠️ تحذير مخالفة رسمي ($newStrike من 5)",
                message = "تم تأكيد مخالفة المحتوى لإرشادات المجتمع بسبب: $violationCategory. سيتم إنهاء الحساب تلقائياً عند بلوغ 5 مخالفات مؤكدة. يمكنك تقديم استئناف إذا رأيت أن القرار غير دقيق.",
                targetId = report.targetId
            )
            dao.insertNotification(notif)
        }

        // 3. Take down content if selected
        if (takeDownContent) {
            when (report.reportType) {
                "VIDEO", "SHORT" -> {
                    dao.updateVideoModeration(report.targetId, "REMOVED", violationCategory)
                }
                "COMMENT" -> {
                    dao.deleteComment(report.targetId)
                }
            }
        }

        // 4. Record Audit Log
        val auditLog = AuditLogEntity(
            id = "audit_" + UUID.randomUUID().toString().take(10),
            adminId = adminId,
            action = "CONFIRM_VIOLATION",
            targetUserId = report.reportedUserId,
            contentId = report.targetId,
            contentType = report.reportType,
            reason = violationCategory,
            previousStatus = "PENDING",
            newStatus = "STRIKE_$newStrike"
        )
        dao.insertAuditLog(auditLog)

        return Result.success(Unit)
    }

    suspend fun dismissReport(
        reportId: String,
        adminId: String,
        reason: String
    ): Result<Unit> {
        val report = dao.getReportByIdDirect(reportId)
            ?: return Result.failure(Exception("البلاغ غير موجود"))

        val updatedReport = report.copy(
            status = "DISMISSED",
            reviewedBy = adminId,
            reviewedAt = System.currentTimeMillis(),
            decision = "DISMISSED: $reason"
        )
        dao.updateReport(updatedReport)

        val auditLog = AuditLogEntity(
            id = "audit_" + UUID.randomUUID().toString().take(10),
            adminId = adminId,
            action = "DISMISS_REPORT",
            targetUserId = report.reportedUserId,
            contentId = report.targetId,
            contentType = report.reportType,
            reason = reason,
            previousStatus = "PENDING",
            newStatus = "DISMISSED"
        )
        dao.insertAuditLog(auditLog)

        return Result.success(Unit)
    }

    suspend fun submitAppeal(
        userId: String,
        contentId: String?,
        contentType: String,
        strikeNumber: Int,
        reason: String,
        additionalInfo: String
    ): Result<AppealEntity> {
        val appeal = AppealEntity(
            id = "appeal_" + UUID.randomUUID().toString().take(10),
            userId = userId,
            contentId = contentId,
            contentType = contentType,
            strikeNumber = strikeNumber,
            reason = reason,
            additionalInfo = additionalInfo.trim(),
            status = "PENDING"
        )
        dao.insertAppeal(appeal)
        return Result.success(appeal)
    }

    suspend fun reviewAppeal(
        appealId: String,
        adminId: String,
        isApproved: Boolean,
        reviewNotes: String
    ): Result<Unit> {
        val appeal = dao.getAppealByIdDirect(appealId)
            ?: return Result.failure(Exception("طلب الاستئناف غير موجود"))

        val newStatus = if (isApproved) "APPROVED" else "REJECTED"
        val updatedAppeal = appeal.copy(
            status = newStatus,
            reviewedBy = adminId,
            reviewedAt = System.currentTimeMillis(),
            reviewNotes = reviewNotes
        )
        dao.updateAppeal(updatedAppeal)

        val user = dao.getUserByIdDirect(appeal.userId)
        if (user != null && isApproved) {
            // Deduct 1 strike
            val newWarningCount = (user.warningCount - 1).coerceAtLeast(0)
            val newAccountStatus = if (newWarningCount < 5) "ACTIVE" else "RESTRICTED"
            dao.updateUserWarningAndStatus(user.id, newWarningCount, newAccountStatus)

            // Restore content if applicable
            if (!appeal.contentId.isNullOrBlank() && (appeal.contentType == "VIDEO" || appeal.contentType == "SHORT")) {
                dao.updateVideoModeration(appeal.contentId, "APPROVED", null)
            }

            // Notification
            val notif = NotificationEntity(
                id = "notif_appeal_" + UUID.randomUUID().toString().take(8),
                userId = user.id,
                type = "SYSTEM",
                title = "✓ تم قبول طلب الاستئناف",
                message = "تمت مراجعة محتواك وقبول الاستئناف بنجاح. تم خفض عدد المخالفات المسجلة بحسابك إلى ($newWarningCount من 5).",
                targetId = appeal.contentId
            )
            dao.insertNotification(notif)
        } else if (user != null && !isApproved) {
            val notif = NotificationEntity(
                id = "notif_appeal_" + UUID.randomUUID().toString().take(8),
                userId = user.id,
                type = "SYSTEM",
                title = "✕ تم رفض طلب الاستئناف",
                message = "بعد التدقيق والمراجعة، تم تثبيت قرار المخالفة لعدم تطابق المحتوى مع سياسات المجتمع: $reviewNotes",
                targetId = appeal.contentId
            )
            dao.insertNotification(notif)
        }

        // Audit Log
        val auditLog = AuditLogEntity(
            id = "audit_" + UUID.randomUUID().toString().take(10),
            adminId = adminId,
            action = if (isApproved) "APPROVE_APPEAL" else "REJECT_APPEAL",
            targetUserId = appeal.userId,
            contentId = appeal.contentId,
            contentType = appeal.contentType,
            reason = reviewNotes,
            previousStatus = "PENDING",
            newStatus = newStatus
        )
        dao.insertAuditLog(auditLog)

        return Result.success(Unit)
    }

    // --- User Block & Preference System ---
    fun getBlockedUsers(userId: String): Flow<List<BlockedUserEntity>> = dao.getBlockedUsers(userId)
    fun isUserBlocked(userId: String, blockedUserId: String): Flow<Boolean> = dao.isUserBlocked(userId, blockedUserId)

    suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit> {
        if (userId == blockedUserId) return Result.failure(Exception("لا يمكنك حظر حسابك الشخصي"))
        val block = BlockedUserEntity(
            id = "block_${userId}_${blockedUserId}",
            userId = userId,
            blockedUserId = blockedUserId
        )
        dao.insertBlockedUser(block)
        // Auto unfollow if currently following
        val channel = dao.getChannelByUserIdDirect(blockedUserId)
        if (channel != null) {
            dao.deleteFollow(userId, channel.id)
            dao.decrementChannelSubscribers(channel.id)
        }
        return Result.success(Unit)
    }

    suspend fun unblockUser(userId: String, blockedUserId: String): Result<Unit> {
        dao.deleteBlockedUser(userId, blockedUserId)
        return Result.success(Unit)
    }

    suspend fun markNotInterested(userId: String, video: VideoEntity): Result<Unit> {
        val entry = NotInterestedEntity(
            id = "not_int_${userId}_${video.id}",
            userId = userId,
            videoId = video.id,
            category = video.category,
            channelId = video.channelId
        )
        dao.insertNotInterested(entry)
        return Result.success(Unit)
    }

    suspend fun dislikeCategory(userId: String, category: String): Result<Unit> {
        val entry = DislikedCategoryEntity(
            id = "dislike_${userId}_${category}",
            userId = userId,
            category = category
        )
        dao.insertDislikedCategory(entry)
        return Result.success(Unit)
    }

    // --- Video Editing by Owner ---
    suspend fun updateVideoDetails(
        videoId: String,
        title: String,
        description: String,
        category: String,
        tags: String
    ): Result<Unit> {
        dao.updateVideoDetails(videoId, title.trim(), description.trim(), category, tags.trim())
        return Result.success(Unit)
    }

    suspend fun toggleVideoVisibility(videoId: String, isPublic: Boolean): Result<Unit> {
        dao.updateVideoVisibility(videoId, isPublic)
        return Result.success(Unit)
    }

    // =========================================================================
    // --- Live Streaming, Gifts, and Wallet Operations ---
    // =========================================================================

    fun getActiveLiveStreams(): Flow<List<LiveStreamEntity>> = dao.getActiveLiveStreams()
    fun getAllLiveStreams(): Flow<List<LiveStreamEntity>> = dao.getAllLiveStreams()
    fun getEndedLiveStreams(): Flow<List<LiveStreamEntity>> = dao.getEndedLiveStreams()
    fun getLiveStreamById(id: String): Flow<LiveStreamEntity?> = dao.getLiveStreamById(id)
    suspend fun getLiveStreamByIdDirect(id: String): LiveStreamEntity? = dao.getLiveStreamByIdDirect(id)

    suspend fun checkLiveEligibility(userId: String): Result<Boolean> {
        return authManager.checkLiveEligibilityFromFirestore(userId)
    }

    suspend fun startLiveStream(
        hostUserId: String,
        title: String,
        coverUrl: String,
        category: String,
        isFrontCamera: Boolean,
        isMicEnabled: Boolean,
        commentsAllowed: Boolean,
        giftsAllowed: Boolean
    ): Result<LiveStreamEntity> {
        val eligibility = checkLiveEligibility(hostUserId)
        if (eligibility.isFailure) {
            return Result.failure(eligibility.exceptionOrNull() ?: Exception("غير مؤهل لبدء بث مباشر"))
        }

        // End any previous active stream from this host
        val previousActive = dao.getActiveLiveStreamByHostDirect(hostUserId)
        if (previousActive != null) {
            dao.updateLiveStreamStatus(previousActive.id, "ENDED", System.currentTimeMillis())
            dao.clearViewerSessions(previousActive.id)
        }

        val hostUser = dao.getUserByIdDirect(hostUserId)!!
        val streamId = "live_" + UUID.randomUUID().toString().take(10)

        val sampleStreamUrls = listOf(
            "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4",
            "https://filesamples.com/samples/video/mp4/sample_960x400_ocean_with_audio.mp4",
            "https://test-videos.co.uk/vids/jellyfish/mp4/h264/720/Jellyfish_720_10s_1MB.mp4",
            "https://filesamples.com/samples/video/mp4/sample_1280x720.mp4"
        )
        val selectedUrl = sampleStreamUrls.random()

        val stream = LiveStreamEntity(
            id = streamId,
            hostUserId = hostUserId,
            hostUsername = hostUser.username,
            hostFullName = hostUser.fullName,
            hostAvatarUrl = hostUser.avatarUrl,
            title = title.ifBlank { "بث مباشر لـ ${hostUser.fullName}" },
            coverUrl = coverUrl.ifBlank { hostUser.avatarUrl },
            category = category,
            streamUrl = selectedUrl,
            status = "LIVE",
            viewersCount = 1,
            peakViewers = 1,
            likesCount = 0,
            totalGiftsCount = 0,
            totalCoinsEarned = 0L,
            commentsAllowed = commentsAllowed,
            giftsAllowed = giftsAllowed,
            isMicEnabled = isMicEnabled,
            isFrontCamera = isFrontCamera,
            startedAt = System.currentTimeMillis()
        )
        dao.insertLiveStream(stream)

        // Sync live stream session metadata to Firestore
        try {
            val streamMap = hashMapOf<String, Any>(
                "id" to streamId,
                "hostUserId" to hostUserId,
                "hostUsername" to hostUser.username,
                "hostFullName" to hostUser.fullName,
                "title" to stream.title,
                "coverUrl" to stream.coverUrl,
                "category" to category,
                "status" to "LIVE",
                "viewersCount" to 1,
                "commentsAllowed" to commentsAllowed,
                "giftsAllowed" to giftsAllowed,
                "startedAt" to stream.startedAt
            )
            authManager.getFirestoreInstance().collection("live_streams").document(streamId).set(streamMap)
        } catch (e: Exception) {
            android.util.Log.w("VidoMixRepository", "Firestore stream broadcast sync note: ${e.message}")
        }

        // Notify followers who have liveNotificationsEnabled
        val userChannel = dao.getChannelByUserIdDirect(hostUserId)
        if (userChannel != null) {
            val channelFollows = dao.getFollowersForChannelDirect(userChannel.id)
            for (follow in channelFollows) {
                val followerUser = dao.getUserByIdDirect(follow.followerUserId)
                if (followerUser != null && followerUser.liveNotificationsEnabled) {
                    dao.insertNotification(
                        NotificationEntity(
                            id = "notif_live_${UUID.randomUUID().toString().take(8)}",
                            userId = follow.followerUserId,
                            type = "LIVE_START",
                            title = "بث مباشر جديد 🔴",
                            message = "بدأ ${hostUser.fullName} بثًا مباشرًا الآن: ${stream.title}",
                            targetId = streamId
                        )
                    )
                }
            }
        }

        return Result.success(stream)
    }

    suspend fun endLiveStream(streamId: String, hostUserId: String): Result<Unit> {
        val stream = dao.getLiveStreamByIdDirect(streamId) ?: return Result.failure(Exception("البث غير موجود"))
        val currentUser = authManager.currentUser.value
        val isHost = stream.hostUserId == hostUserId
        val isAdmin = currentUser?.isAdmin == true
        if (!isHost && !isAdmin) {
            return Result.failure(Exception("ليس لديك صلاحية لإنهاء هذا البث"))
        }
        dao.updateLiveStreamStatus(streamId, "ENDED", System.currentTimeMillis())
        dao.clearViewerSessions(streamId)
        return Result.success(Unit)
    }

    suspend fun joinLiveStream(streamId: String, user: UserEntity): Result<Boolean> {
        if (dao.isUserBannedFromStreamDirect(streamId, user.id)) {
            return Result.failure(Exception("أنت محظور من مشاهدة هذا البث المباشر من قِبل المضيف."))
        }
        val stream = dao.getLiveStreamByIdDirect(streamId) ?: return Result.failure(Exception("البث غير موجود أو انتهى"))
        if (stream.status != "LIVE") {
            return Result.failure(Exception("انتهى هذا البث المباشر."))
        }

        val session = LiveViewerSessionEntity(
            id = "sess_${streamId}_${user.id}",
            streamId = streamId,
            userId = user.id,
            username = user.username,
            avatarUrl = user.avatarUrl,
            role = if (stream.hostUserId == user.id) "HOST" else if (user.isAdmin) "MODERATOR" else "VIEWER"
        )
        dao.insertViewerSession(session)

        val newCount = stream.viewersCount + 1
        val newPeak = maxOf(newCount, stream.peakViewers)
        dao.updateLiveStreamMetrics(
            streamId,
            viewers = newCount,
            peak = newPeak,
            likes = stream.likesCount,
            gifts = stream.totalGiftsCount,
            coins = stream.totalCoinsEarned
        )

        dao.insertLiveComment(
            LiveCommentEntity(
                id = "c_join_${UUID.randomUUID().toString().take(8)}",
                streamId = streamId,
                userId = user.id,
                username = user.username,
                userAvatarUrl = user.avatarUrl,
                text = "انضم إلى البث المباشر 👋",
                isSystemNotification = true
            )
        )

        return Result.success(true)
    }

    suspend fun leaveLiveStream(streamId: String, userId: String) {
        dao.removeViewerSession(streamId, userId)
        val stream = dao.getLiveStreamByIdDirect(streamId) ?: return
        val newCount = maxOf(1, stream.viewersCount - 1)
        dao.updateLiveStreamMetrics(
            streamId,
            viewers = newCount,
            peak = stream.peakViewers,
            likes = stream.likesCount,
            gifts = stream.totalGiftsCount,
            coins = stream.totalCoinsEarned
        )
    }

    fun getLiveComments(streamId: String): Flow<List<LiveCommentEntity>> = dao.getLiveCommentsForStream(streamId)

    suspend fun sendLiveComment(streamId: String, user: UserEntity, text: String): Result<LiveCommentEntity> {
        val stream = dao.getLiveStreamByIdDirect(streamId) ?: return Result.failure(Exception("البث غير موجود"))
        if (stream.status != "LIVE") return Result.failure(Exception("البث منتهي"))
        if (!stream.commentsAllowed && stream.hostUserId != user.id) {
            return Result.failure(Exception("التعليقات متوقفة في هذا البث حالياً."))
        }
        if (dao.isUserMutedInStreamDirect(streamId, user.id)) {
            return Result.failure(Exception("تم كتمك في هذا البث من قبل المضيف."))
        }
        if (dao.isUserBannedFromStreamDirect(streamId, user.id)) {
            return Result.failure(Exception("أنت محظور من هذا البث."))
        }

        val comment = LiveCommentEntity(
            id = "lc_" + UUID.randomUUID().toString().take(10),
            streamId = streamId,
            userId = user.id,
            username = user.username,
            userAvatarUrl = user.avatarUrl,
            text = text.trim(),
            isHost = (stream.hostUserId == user.id),
            isModerator = user.isAdmin || (user.liveRole == "MODERATOR")
        )
        dao.insertLiveComment(comment)
        return Result.success(comment)
    }

    suspend fun pinLiveComment(streamId: String, comment: LiveCommentEntity): Result<Unit> {
        dao.updateLiveCommentPinned(comment.id, true)
        dao.updateLiveStreamPinnedComment(streamId, comment.id, comment.text, comment.username)
        return Result.success(Unit)
    }

    suspend fun unpinLiveComment(streamId: String): Result<Unit> {
        val stream = dao.getLiveStreamByIdDirect(streamId)
        if (stream?.pinnedCommentId != null) {
            dao.updateLiveCommentPinned(stream.pinnedCommentId, false)
        }
        dao.updateLiveStreamPinnedComment(streamId, null, null, null)
        return Result.success(Unit)
    }

    suspend fun deleteLiveComment(commentId: String): Result<Unit> {
        dao.deleteLiveComment(commentId)
        return Result.success(Unit)
    }

    suspend fun muteUserInLive(streamId: String, targetUserId: String, targetUsername: String): Result<Unit> {
        val entry = LiveMutedUserEntity(
            id = "mute_${streamId}_${targetUserId}",
            streamId = streamId,
            userId = targetUserId,
            username = targetUsername
        )
        dao.insertMutedUser(entry)
        return Result.success(Unit)
    }

    suspend fun unmuteUserInLive(streamId: String, targetUserId: String): Result<Unit> {
        dao.deleteMutedUser(streamId, targetUserId)
        return Result.success(Unit)
    }

    suspend fun banUserFromLive(streamId: String, targetUserId: String, targetUsername: String): Result<Unit> {
        val entry = LiveBannedViewerEntity(
            id = "ban_${streamId}_${targetUserId}",
            streamId = streamId,
            userId = targetUserId,
            username = targetUsername
        )
        dao.insertBannedViewer(entry)
        dao.removeViewerSession(streamId, targetUserId)
        return Result.success(Unit)
    }

    suspend fun addLiveLike(streamId: String, count: Int = 1) {
        val stream = dao.getLiveStreamByIdDirect(streamId) ?: return
        dao.updateLiveStreamMetrics(
            streamId,
            viewers = stream.viewersCount,
            peak = stream.peakViewers,
            likes = stream.likesCount + count,
            gifts = stream.totalGiftsCount,
            coins = stream.totalCoinsEarned
        )
    }

    fun getAllGifts(): Flow<List<LiveGiftEntity>> = dao.getAllGifts()
    fun getActiveGifts(): Flow<List<LiveGiftEntity>> = dao.getActiveGifts()

    suspend fun sendLiveGift(
        streamId: String,
        senderUserId: String,
        giftId: String,
        quantity: Int = 1
    ): Result<LiveGiftTransactionEntity> {
        val stream = dao.getLiveStreamByIdDirect(streamId) ?: return Result.failure(Exception("البث غير موجود"))
        if (stream.status != "LIVE") return Result.failure(Exception("البث منتهي"))
        if (!stream.giftsAllowed) return Result.failure(Exception("إرسال الهدايا متوقف في هذا البث حالياً."))

        val sender = dao.getUserByIdDirect(senderUserId) ?: return Result.failure(Exception("المرسل غير موجود"))
        val gift = dao.getGiftByIdDirect(giftId) ?: return Result.failure(Exception("الهدية غير متوفرة"))
        if (!gift.isEnabled) return Result.failure(Exception("هذه الهدية معطلة حالياً."))

        val totalCoinsCost = gift.coinPrice * quantity
        if (sender.coinsBalance < totalCoinsCost) {
            return Result.failure(Exception("رصيد العملات غير كافٍ! يتطلب $totalCoinsCost عملة، ورصيدك الحالي هو ${sender.coinsBalance} عملة."))
        }

        val newSenderBalance = sender.coinsBalance - totalCoinsCost
        dao.updateUserCoinsBalance(senderUserId, newSenderBalance)

        val settings = dao.getPlatformSettingsDirect() ?: PlatformSettingsEntity()
        val totalUsdValue = totalCoinsCost * settings.coinToUsdRate
        val hostShareUsd = totalUsdValue * (settings.liveCreatorSharePercent / 100.0)
        val platformShareUsd = totalUsdValue * (settings.livePlatformSharePercent / 100.0)

        val hostProfile = dao.getMonetizationProfileDirect(stream.hostUserId)
        if (hostProfile != null) {
            dao.updateMonetizationProfile(
                hostProfile.copy(
                    currentBalance = hostProfile.currentBalance + hostShareUsd,
                    pendingBalance = hostProfile.pendingBalance,
                    lifetimeEarnings = hostProfile.lifetimeEarnings + hostShareUsd
                )
            )
        }

        val txId = "gtx_" + UUID.randomUUID().toString().take(12)
        val transaction = LiveGiftTransactionEntity(
            id = txId,
            streamId = streamId,
            senderUserId = senderUserId,
            senderUsername = sender.username,
            senderAvatarUrl = sender.avatarUrl,
            hostUserId = stream.hostUserId,
            giftId = gift.id,
            giftName = gift.name,
            giftIcon = gift.emojiIcon,
            giftCoinPrice = gift.coinPrice,
            quantity = quantity,
            totalCoins = totalCoinsCost,
            hostEarningsAmount = hostShareUsd,
            platformFeeAmount = platformShareUsd
        )
        dao.insertGiftTransaction(transaction)

        dao.updateLiveStreamMetrics(
            id = streamId,
            viewers = stream.viewersCount,
            peak = stream.peakViewers,
            likes = stream.likesCount,
            gifts = stream.totalGiftsCount + quantity,
            coins = stream.totalCoinsEarned + totalCoinsCost
        )

        dao.insertLiveComment(
            LiveCommentEntity(
                id = "c_gift_${UUID.randomUUID().toString().take(8)}",
                streamId = streamId,
                userId = sender.id,
                username = sender.username,
                userAvatarUrl = sender.avatarUrl,
                text = "أرسل ${gift.name} ${gift.emojiIcon} x$quantity!",
                isSystemNotification = true
            )
        )

        return Result.success(transaction)
    }

    fun getAllCoinPackages(): Flow<List<CoinPackageEntity>> = dao.getAllCoinPackages()
    fun getActiveCoinPackages(): Flow<List<CoinPackageEntity>> = dao.getActiveCoinPackages()

    suspend fun purchaseCoins(
        userId: String,
        packageId: String,
        paymentMethod: String = "Google Play Billing"
    ): Result<CoinPurchaseOrderEntity> {
        val user = dao.getUserByIdDirect(userId) ?: return Result.failure(Exception("المستخدم غير موجود"))
        val packages = dao.getAllCoinPackagesDirect()
        val pkg = packages.find { it.id == packageId } ?: return Result.failure(Exception("الباقة غير موجودة"))

        val orderId = "GPA." + UUID.randomUUID().toString().take(14).replace("-", "").uppercase()
        if (dao.hasPurchaseOrder(orderId)) {
            return Result.failure(Exception("تمت معالجة هذه العملية مسبقاً لمنع التكرار."))
        }

        val totalCoinsAdded = pkg.coinsAmount + pkg.bonusCoins
        val newBalance = user.coinsBalance + totalCoinsAdded
        dao.updateUserCoinsBalance(userId, newBalance)

        val order = CoinPurchaseOrderEntity(
            id = orderId,
            userId = userId,
            packageId = packageId,
            coinsAmount = totalCoinsAdded,
            pricePaidUsd = pkg.priceUsd,
            paymentMethod = paymentMethod,
            status = "COMPLETED",
            serverVerificationToken = "sig_valid_" + UUID.randomUUID().toString().take(16)
        )
        dao.insertCoinPurchaseOrder(order)

        return Result.success(order)
    }

    fun getUserPurchaseOrders(userId: String): Flow<List<CoinPurchaseOrderEntity>> = dao.getUserPurchaseOrders(userId)
    fun getAllPurchaseOrders(): Flow<List<CoinPurchaseOrderEntity>> = dao.getAllPurchaseOrders()
    fun getGiftTransactionsSent(userId: String): Flow<List<LiveGiftTransactionEntity>> = dao.getGiftTransactionsSentByUser(userId)
    fun getGiftTransactionsReceived(hostUserId: String): Flow<List<LiveGiftTransactionEntity>> = dao.getGiftTransactionsReceivedByUser(hostUserId)
    fun getAllGiftTransactions(): Flow<List<LiveGiftTransactionEntity>> = dao.getAllGiftTransactions()
    fun getViewerSessions(streamId: String): Flow<List<LiveViewerSessionEntity>> = dao.getViewerSessions(streamId)

    suspend fun adminBanLiveStream(streamId: String, adminId: String, reason: String): Result<Unit> {
        val stream = dao.getLiveStreamByIdDirect(streamId) ?: return Result.failure(Exception("البث غير موجود"))
        dao.updateLiveStreamStatus(streamId, "BANNED", System.currentTimeMillis())
        dao.clearViewerSessions(streamId)
        dao.insertAuditLog(
            AuditLogEntity(
                id = "audit_live_${UUID.randomUUID().toString().take(8)}",
                adminId = adminId,
                action = "BAN_LIVE_STREAM",
                targetUserId = stream.hostUserId,
                contentId = streamId,
                contentType = "LIVE_STREAM",
                reason = reason,
                previousStatus = stream.status,
                newStatus = "BANNED"
            )
        )
        return Result.success(Unit)
    }

    suspend fun adminSaveGift(gift: LiveGiftEntity): Result<Unit> {
        dao.insertGift(gift)
        return Result.success(Unit)
    }

    suspend fun adminDeleteGift(giftId: String): Result<Unit> {
        dao.deleteGift(giftId)
        return Result.success(Unit)
    }

    suspend fun adminSaveCoinPackage(pkg: CoinPackageEntity): Result<Unit> {
        dao.insertCoinPackage(pkg)
        return Result.success(Unit)
    }

    suspend fun adminDeleteCoinPackage(pkgId: String): Result<Unit> {
        dao.deleteCoinPackage(pkgId)
        return Result.success(Unit)
    }

    suspend fun adminUpdateLiveRevenueShare(platformShare: Double, creatorShare: Double): Result<Unit> {
        val settings = dao.getPlatformSettingsDirect() ?: PlatformSettingsEntity()
        val updated = settings.copy(
            livePlatformSharePercent = platformShare,
            liveCreatorSharePercent = creatorShare
        )
        dao.insertPlatformSettings(updated)
        return Result.success(Unit)
    }

    suspend fun toggleLiveNotifications(userId: String, enabled: Boolean): Result<Unit> {
        val user = dao.getUserByIdDirect(userId) ?: return Result.failure(Exception("المستخدم غير موجود"))
        dao.insertUser(user.copy(liveNotificationsEnabled = enabled))
        return Result.success(Unit)
    }
}
