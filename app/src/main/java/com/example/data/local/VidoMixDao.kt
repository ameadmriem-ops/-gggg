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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("UPDATE reports SET status = :status WHERE id = :id")
    suspend fun updateReportStatus(id: String, status: String)

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
}
