package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VidoMixDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserByIdDirect(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    // --- Channels ---
    @Query("SELECT * FROM channels WHERE id = :id")
    fun getChannelById(id: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelByIdDirect(id: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE userId = :userId LIMIT 1")
    fun getChannelByUserId(userId: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE userId = :userId LIMIT 1")
    suspend fun getChannelByUserIdDirect(userId: String): ChannelEntity?

    @Query("SELECT * FROM channels ORDER BY subscriberCount DESC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' OR handle LIKE '%' || :query || '%'")
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    // --- Videos & Shorts ---
    @Query("SELECT * FROM videos WHERE isShort = :isShort AND isPublic = 1 ORDER BY uploadTimestamp DESC")
    fun getVideos(isShort: Boolean): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isShort = :isShort AND isPublic = 1 ORDER BY viewsCount DESC")
    fun getTrendingVideos(isShort: Boolean): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isShort = :isShort AND category = :category AND isPublic = 1 ORDER BY uploadTimestamp DESC")
    fun getVideosByCategory(category: String, isShort: Boolean): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    fun getVideoById(id: String): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoByIdDirect(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE channelId = :channelId AND isShort = :isShort AND isPublic = 1 ORDER BY uploadTimestamp DESC")
    fun getVideosByChannel(channelId: String, isShort: Boolean): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE channelId = :channelId ORDER BY uploadTimestamp DESC")
    fun getAllVideosByChannel(channelId: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY viewsCount DESC")
    fun searchVideos(query: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: String)

    @Query("UPDATE videos SET viewsCount = viewsCount + 1 WHERE id = :id")
    suspend fun incrementViews(id: String)

    @Query("UPDATE videos SET likesCount = likesCount + 1 WHERE id = :id")
    suspend fun incrementLikes(id: String)

    @Query("UPDATE videos SET likesCount = CASE WHEN likesCount > 0 THEN likesCount - 1 ELSE 0 END WHERE id = :id")
    suspend fun decrementLikes(id: String)

    @Query("UPDATE videos SET commentsCount = commentsCount + 1 WHERE id = :id")
    suspend fun incrementCommentsCount(id: String)

    // --- Comments ---
    @Query("SELECT * FROM comments WHERE videoId = :videoId AND parentCommentId IS NULL ORDER BY timestamp DESC")
    fun getCommentsForVideo(videoId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE parentCommentId = :parentId ORDER BY timestamp ASC")
    fun getRepliesForComment(parentId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteComment(id: String)

    @Query("SELECT * FROM comments WHERE id = :id LIMIT 1")
    suspend fun getCommentByIdDirect(id: String): CommentEntity?

    @Query("UPDATE comments SET moderationStatus = :status, moderationReason = :reason WHERE id = :commentId")
    suspend fun updateCommentModeration(commentId: String, status: String, reason: String?)

    // --- Likes ---
    @Query("SELECT * FROM likes WHERE userId = :userId AND targetType = :targetType AND targetId = :targetId LIMIT 1")
    suspend fun getLike(userId: String, targetType: String, targetId: String): LikeEntity?

    @Query("SELECT COUNT(*) > 0 FROM likes WHERE userId = :userId AND targetType = :targetType AND targetId = :targetId")
    fun isLikedFlow(userId: String, targetType: String, targetId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Query("DELETE FROM likes WHERE userId = :userId AND targetType = :targetType AND targetId = :targetId")
    suspend fun deleteLike(userId: String, targetType: String, targetId: String)

    // --- Follows ---
    @Query("SELECT COUNT(*) > 0 FROM follows WHERE followerUserId = :followerUserId AND followedChannelId = :channelId")
    fun isFollowing(followerUserId: String, channelId: String): Flow<Boolean>

    @Query("SELECT * FROM follows WHERE followerUserId = :followerUserId AND followedChannelId = :channelId LIMIT 1")
    suspend fun getFollow(followerUserId: String, channelId: String): FollowEntity?

    @Query("SELECT * FROM follows WHERE followedChannelId = :channelId")
    suspend fun getFollowersForChannelDirect(channelId: String): List<FollowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE followerUserId = :followerUserId AND followedChannelId = :channelId")
    suspend fun deleteFollow(followerUserId: String, channelId: String)

    @Query("UPDATE channels SET subscriberCount = subscriberCount + 1 WHERE id = :channelId")
    suspend fun incrementChannelSubscribers(channelId: String)

    @Query("UPDATE channels SET subscriberCount = CASE WHEN subscriberCount > 0 THEN subscriberCount - 1 ELSE 0 END WHERE id = :channelId")
    suspend fun decrementChannelSubscribers(channelId: String)

    // --- Saved Videos ---
    @Query("SELECT COUNT(*) > 0 FROM saved_videos WHERE userId = :userId AND videoId = :videoId")
    fun isVideoSaved(userId: String, videoId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedVideo(saved: SavedVideoEntity)

    @Query("DELETE FROM saved_videos WHERE userId = :userId AND videoId = :videoId")
    suspend fun deleteSavedVideo(userId: String, videoId: String)

    @Query("SELECT v.* FROM videos v INNER JOIN saved_videos s ON v.id = s.videoId WHERE s.userId = :userId ORDER BY s.timestamp DESC")
    fun getSavedVideosForUser(userId: String): Flow<List<VideoEntity>>

    // --- Watch History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordWatchHistory(history: WatchHistoryEntity)

    @Query("SELECT v.* FROM videos v INNER JOIN watch_history w ON v.id = w.videoId WHERE w.userId = :userId ORDER BY w.timestamp DESC")
    fun getWatchHistoryVideos(userId: String): Flow<List<VideoEntity>>

    @Query("DELETE FROM watch_history WHERE userId = :userId")
    suspend fun clearWatchHistory(userId: String)

    // --- Notifications ---
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: String)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun clearNotifications(userId: String)

    // --- Reports ---
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE status = :status ORDER BY timestamp DESC")
    fun getReportsByStatus(status: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun getReportByIdDirect(id: String): ReportEntity?

    @Query("SELECT COUNT(*) > 0 FROM reports WHERE reporterUserId = :reporterUserId AND targetId = :targetId")
    suspend fun hasUserReportedContent(reporterUserId: String, targetId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Query("UPDATE reports SET status = :status, reviewedBy = :reviewedBy, reviewedAt = :reviewedAt, decision = :decision WHERE id = :id")
    suspend fun updateReportDecision(id: String, status: String, reviewedBy: String, reviewedAt: Long, decision: String)

    // --- Appeals ---
    @Query("SELECT * FROM appeals ORDER BY timestamp DESC")
    fun getAllAppeals(): Flow<List<AppealEntity>>

    @Query("SELECT * FROM appeals WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAppealsForUser(userId: String): Flow<List<AppealEntity>>

    @Query("SELECT * FROM appeals WHERE id = :id LIMIT 1")
    suspend fun getAppealByIdDirect(id: String): AppealEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppeal(appeal: AppealEntity)

    @Update
    suspend fun updateAppeal(appeal: AppealEntity)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // --- Content & User Moderation Updates ---
    @Query("UPDATE users SET warningCount = :warningCount, accountStatus = :accountStatus WHERE id = :userId")
    suspend fun updateUserWarningAndStatus(userId: String, warningCount: Int, accountStatus: String)

    @Query("UPDATE videos SET moderationStatus = :status, moderationReason = :reason WHERE id = :videoId")
    suspend fun updateVideoModeration(videoId: String, status: String, reason: String?)

    // --- Search History ---
    @Query("SELECT * FROM search_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(userId: String, limit: Int = 10): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearch(id: Long)

    @Query("DELETE FROM search_history WHERE userId = :userId")
    suspend fun clearSearchHistory(userId: String)

    // --- Monetization Profiles ---
    @Query("SELECT * FROM monetization_profiles WHERE userId = :userId LIMIT 1")
    fun getMonetizationProfile(userId: String): Flow<MonetizationProfileEntity?>

    @Query("SELECT * FROM monetization_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getMonetizationProfileDirect(userId: String): MonetizationProfileEntity?

    @Query("SELECT * FROM monetization_profiles ORDER BY appliedAt DESC")
    fun getAllMonetizationProfiles(): Flow<List<MonetizationProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonetizationProfile(profile: MonetizationProfileEntity)

    @Update
    suspend fun updateMonetizationProfile(profile: MonetizationProfileEntity)

    // --- Ad Impressions & Revenue Tracking ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordAdImpression(impression: AdImpressionEntity)

    @Query("SELECT * FROM ad_impressions WHERE creatorId = :creatorId ORDER BY timestamp DESC")
    fun getImpressionsForCreator(creatorId: String): Flow<List<AdImpressionEntity>>

    @Query("SELECT * FROM ad_impressions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentImpressions(limit: Int = 100): Flow<List<AdImpressionEntity>>

    @Query("SELECT * FROM ad_impressions WHERE status IN ('SUSPICIOUS', 'HELD_FOR_REVIEW') ORDER BY timestamp DESC")
    fun getFlaggedImpressions(): Flow<List<AdImpressionEntity>>

    @Query("SELECT COUNT(*) FROM ad_impressions WHERE creatorId = :creatorId AND isCreatorEligible = 1")
    suspend fun getEligibleAdCountForCreator(creatorId: String): Long

    @Query("SELECT COALESCE(SUM(creatorShareAmount), 0.0) FROM ad_impressions WHERE creatorId = :creatorId AND isCreatorEligible = 1 AND status = 'VERIFIED'")
    suspend fun getTotalEarningsForCreator(creatorId: String): Double

    // --- Payouts ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayout(payout: PayoutEntity)

    @Update
    suspend fun updatePayout(payout: PayoutEntity)

    @Query("SELECT * FROM payouts WHERE userId = :userId ORDER BY requestedAt DESC")
    fun getPayoutsForUser(userId: String): Flow<List<PayoutEntity>>

    @Query("SELECT * FROM payouts ORDER BY requestedAt DESC")
    fun getAllPayouts(): Flow<List<PayoutEntity>>

    // --- Platform Settings ---
    @Query("SELECT * FROM platform_settings WHERE `key` = 'global' LIMIT 1")
    fun getPlatformSettings(): Flow<PlatformSettingsEntity?>

    @Query("SELECT * FROM platform_settings WHERE `key` = 'global' LIMIT 1")
    suspend fun getPlatformSettingsDirect(): PlatformSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatformSettings(settings: PlatformSettingsEntity)

    // --- User Block & Preference Management ---
    @Query("SELECT * FROM blocked_users WHERE userId = :userId")
    fun getBlockedUsers(userId: String): Flow<List<BlockedUserEntity>>

    @Query("SELECT * FROM blocked_users WHERE userId = :userId")
    suspend fun getBlockedUsersDirect(userId: String): List<BlockedUserEntity>

    @Query("SELECT COUNT(*) > 0 FROM blocked_users WHERE userId = :userId AND blockedUserId = :blockedUserId")
    fun isUserBlocked(userId: String, blockedUserId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUser(blocked: BlockedUserEntity)

    @Query("DELETE FROM blocked_users WHERE userId = :userId AND blockedUserId = :blockedUserId")
    suspend fun deleteBlockedUser(userId: String, blockedUserId: String)

    // --- Not Interested Videos ---
    @Query("SELECT * FROM not_interested_videos WHERE userId = :userId")
    fun getNotInterestedVideos(userId: String): Flow<List<NotInterestedEntity>>

    @Query("SELECT * FROM not_interested_videos WHERE userId = :userId")
    suspend fun getNotInterestedVideosDirect(userId: String): List<NotInterestedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotInterested(notInterested: NotInterestedEntity)

    // --- Disliked Categories ---
    @Query("SELECT * FROM disliked_categories WHERE userId = :userId")
    fun getDislikedCategories(userId: String): Flow<List<DislikedCategoryEntity>>

    @Query("SELECT * FROM disliked_categories WHERE userId = :userId")
    suspend fun getDislikedCategoriesDirect(userId: String): List<DislikedCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDislikedCategory(item: DislikedCategoryEntity)

    // --- Video Editing by Owner ---
    @Query("UPDATE videos SET title = :title, description = :description, category = :category, tags = :tags WHERE id = :videoId")
    suspend fun updateVideoDetails(videoId: String, title: String, description: String, category: String, tags: String)

    @Query("UPDATE videos SET isPublic = :isPublic WHERE id = :videoId")
    suspend fun updateVideoVisibility(videoId: String, isPublic: Boolean)

    // --- Live Streams ---
    @Query("SELECT * FROM live_streams WHERE status = 'LIVE' ORDER BY viewersCount DESC")
    fun getActiveLiveStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams ORDER BY startedAt DESC")
    fun getAllLiveStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE status = 'ENDED' ORDER BY startedAt DESC")
    fun getEndedLiveStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE id = :id LIMIT 1")
    fun getLiveStreamById(id: String): Flow<LiveStreamEntity?>

    @Query("SELECT * FROM live_streams WHERE id = :id LIMIT 1")
    suspend fun getLiveStreamByIdDirect(id: String): LiveStreamEntity?

    @Query("SELECT * FROM live_streams WHERE hostUserId = :hostUserId AND status = 'LIVE' LIMIT 1")
    suspend fun getActiveLiveStreamByHostDirect(hostUserId: String): LiveStreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveStream(stream: LiveStreamEntity)

    @Update
    suspend fun updateLiveStream(stream: LiveStreamEntity)

    @Query("UPDATE live_streams SET status = :status, endedAt = :endedAt WHERE id = :id")
    suspend fun updateLiveStreamStatus(id: String, status: String, endedAt: Long?)

    @Query("UPDATE live_streams SET viewersCount = :viewers, peakViewers = :peak, likesCount = :likes, totalGiftsCount = :gifts, totalCoinsEarned = :coins WHERE id = :id")
    suspend fun updateLiveStreamMetrics(id: String, viewers: Int, peak: Int, likes: Int, gifts: Int, coins: Long)

    @Query("UPDATE live_streams SET pinnedCommentId = :commentId, pinnedCommentText = :text, pinnedCommentUser = :user WHERE id = :streamId")
    suspend fun updateLiveStreamPinnedComment(streamId: String, commentId: String?, text: String?, user: String?)

    // --- Live Comments ---
    @Query("SELECT * FROM live_comments WHERE streamId = :streamId ORDER BY timestamp ASC")
    fun getLiveCommentsForStream(streamId: String): Flow<List<LiveCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveComment(comment: LiveCommentEntity)

    @Query("DELETE FROM live_comments WHERE id = :commentId")
    suspend fun deleteLiveComment(commentId: String)

    @Query("UPDATE live_comments SET isPinned = :isPinned WHERE id = :commentId")
    suspend fun updateLiveCommentPinned(commentId: String, isPinned: Boolean)

    // --- Live Gifts & Transactions ---
    @Query("SELECT * FROM live_gifts ORDER BY displayOrder ASC, coinPrice ASC")
    fun getAllGifts(): Flow<List<LiveGiftEntity>>

    @Query("SELECT * FROM live_gifts WHERE isEnabled = 1 ORDER BY displayOrder ASC, coinPrice ASC")
    fun getActiveGifts(): Flow<List<LiveGiftEntity>>

    @Query("SELECT * FROM live_gifts")
    suspend fun getAllGiftsDirect(): List<LiveGiftEntity>

    @Query("SELECT * FROM live_gifts WHERE id = :id LIMIT 1")
    suspend fun getGiftByIdDirect(id: String): LiveGiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGift(gift: LiveGiftEntity)

    @Update
    suspend fun updateGift(gift: LiveGiftEntity)

    @Query("DELETE FROM live_gifts WHERE id = :id")
    suspend fun deleteGift(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGiftTransaction(transaction: LiveGiftTransactionEntity)

    @Query("SELECT * FROM live_gift_transactions WHERE streamId = :streamId ORDER BY timestamp DESC")
    fun getGiftTransactionsForStream(streamId: String): Flow<List<LiveGiftTransactionEntity>>

    @Query("SELECT * FROM live_gift_transactions WHERE senderUserId = :userId ORDER BY timestamp DESC")
    fun getGiftTransactionsSentByUser(userId: String): Flow<List<LiveGiftTransactionEntity>>

    @Query("SELECT * FROM live_gift_transactions WHERE hostUserId = :hostUserId ORDER BY timestamp DESC")
    fun getGiftTransactionsReceivedByUser(hostUserId: String): Flow<List<LiveGiftTransactionEntity>>

    @Query("SELECT * FROM live_gift_transactions ORDER BY timestamp DESC")
    fun getAllGiftTransactions(): Flow<List<LiveGiftTransactionEntity>>

    // --- Coin Packages & Orders ---
    @Query("SELECT * FROM coin_packages ORDER BY priceUsd ASC")
    fun getAllCoinPackages(): Flow<List<CoinPackageEntity>>

    @Query("SELECT * FROM coin_packages WHERE isEnabled = 1 ORDER BY priceUsd ASC")
    fun getActiveCoinPackages(): Flow<List<CoinPackageEntity>>

    @Query("SELECT * FROM coin_packages")
    suspend fun getAllCoinPackagesDirect(): List<CoinPackageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoinPackage(pkg: CoinPackageEntity)

    @Update
    suspend fun updateCoinPackage(pkg: CoinPackageEntity)

    @Query("DELETE FROM coin_packages WHERE id = :id")
    suspend fun deleteCoinPackage(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoinPurchaseOrder(order: CoinPurchaseOrderEntity)

    @Query("SELECT * FROM coin_purchase_orders WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserPurchaseOrders(userId: String): Flow<List<CoinPurchaseOrderEntity>>

    @Query("SELECT * FROM coin_purchase_orders ORDER BY timestamp DESC")
    fun getAllPurchaseOrders(): Flow<List<CoinPurchaseOrderEntity>>

    @Query("SELECT COUNT(*) > 0 FROM coin_purchase_orders WHERE id = :orderId")
    suspend fun hasPurchaseOrder(orderId: String): Boolean

    // --- User Coins Balance ---
    @Query("UPDATE users SET coinsBalance = :newBalance WHERE id = :userId")
    suspend fun updateUserCoinsBalance(userId: String, newBalance: Long)

    @Query("SELECT coinsBalance FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserCoinsBalanceDirect(userId: String): Long?

    // --- Live Stream Moderation: Mute, Ban, Viewers ---
    @Query("SELECT * FROM live_muted_users WHERE streamId = :streamId")
    fun getMutedUsersForStream(streamId: String): Flow<List<LiveMutedUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMutedUser(muted: LiveMutedUserEntity)

    @Query("DELETE FROM live_muted_users WHERE streamId = :streamId AND userId = :userId")
    suspend fun deleteMutedUser(streamId: String, userId: String)

    @Query("SELECT COUNT(*) > 0 FROM live_muted_users WHERE streamId = :streamId AND userId = :userId")
    suspend fun isUserMutedInStreamDirect(streamId: String, userId: String): Boolean

    @Query("SELECT * FROM live_banned_viewers WHERE streamId = :streamId")
    fun getBannedViewersForStream(streamId: String): Flow<List<LiveBannedViewerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBannedViewer(banned: LiveBannedViewerEntity)

    @Query("SELECT COUNT(*) > 0 FROM live_banned_viewers WHERE streamId = :streamId AND userId = :userId")
    suspend fun isUserBannedFromStreamDirect(streamId: String, userId: String): Boolean

    @Query("SELECT * FROM live_viewer_sessions WHERE streamId = :streamId ORDER BY joinedAt DESC")
    fun getViewerSessions(streamId: String): Flow<List<LiveViewerSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViewerSession(session: LiveViewerSessionEntity)

    @Query("DELETE FROM live_viewer_sessions WHERE streamId = :streamId AND userId = :userId")
    suspend fun removeViewerSession(streamId: String, userId: String)

    @Query("DELETE FROM live_viewer_sessions WHERE streamId = :streamId")
    suspend fun clearViewerSessions(streamId: String)
}
