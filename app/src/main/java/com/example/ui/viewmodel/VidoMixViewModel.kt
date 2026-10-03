package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.InitialDataSeeder
import com.example.data.local.VidoMixDatabase
import com.example.data.model.*
import com.example.data.repository.AuthManager
import com.example.data.repository.RecommendationEngine
import com.example.data.repository.VidoMixRepository
import com.example.player.VidoMixPlayerController
import com.example.ads.AdMobManager
import com.example.core.AppCrashReporter
import com.example.core.NetworkManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VidoMixViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VidoMixDatabase.getInstance(application)
    private val dao = database.vidoMixDao()
    val networkManager = NetworkManager.getInstance(application)
    val isOnline: StateFlow<Boolean> = networkManager.isOnline

    val authManager = AuthManager(dao, application)
    val recommendationEngine = RecommendationEngine(dao)
    val repository = VidoMixRepository(dao, authManager, recommendationEngine)
    val playerController = VidoMixPlayerController(application)

    // Current User
    val currentUser: StateFlow<UserEntity?> = authManager.currentUser

    // Dark Mode Preference
    val isDarkMode = MutableStateFlow(true)

    // Channels Map & List
    val allChannels: StateFlow<List<ChannelEntity>> = repository.getAllChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category selection in Home and Videos tab
    val selectedCategory = MutableStateFlow("الكل")

    // Feeds
    val rawLongVideos: StateFlow<List<VideoEntity>> = repository.getLongVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendingVideos: StateFlow<List<VideoEntity>> = repository.getTrendingLongVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedUsers: StateFlow<List<BlockedUserEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getBlockedUsers(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortsList: StateFlow<List<VideoEntity>> = combine(
        repository.getShorts(),
        currentUser,
        allChannels,
        blockedUsers
    ) { shorts, user, channels, blocks ->
        if (user == null) {
            shorts
        } else {
            val blockedIds = blocks.map { it.blockedUserId }.toSet()
            val notInterestedIds = dao.getNotInterestedVideosDirect(user.id).map { it.videoId }.toSet()
            val channelMap = channels.associateBy { it.id }
            shorts.filter { short ->
                if (notInterestedIds.contains(short.id)) return@filter false
                val channel = channelMap[short.channelId]
                if (channel != null && blockedIds.contains(channel.userId)) return@filter false
                true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Personalized Home Feed (Ranked by Recommendation Engine)
    private val _recommendedHomeVideos = MutableStateFlow<List<VideoEntity>>(emptyList())
    val recommendedHomeVideos: StateFlow<List<VideoEntity>> = _recommendedHomeVideos.asStateFlow()

    // Currently Playing / Inspected Video
    val activeVideo = MutableStateFlow<VideoEntity?>(null)
    val activeChannel = MutableStateFlow<ChannelEntity?>(null)
    val activeComments = MutableStateFlow<List<CommentEntity>>(emptyList())

    val isCurrentVideoLiked = MutableStateFlow(false)
    val isCurrentVideoSaved = MutableStateFlow(false)
    val isCurrentChannelFollowed = MutableStateFlow(false)

    // User Profile Data
    val savedVideos: StateFlow<List<VideoEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getSavedVideos(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<VideoEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getWatchHistory(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getNotifications(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search
    val searchQuery = MutableStateFlow("")
    val searchCategoryFilter = MutableStateFlow("الكل")

    val searchResultsVideos = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) flowOf(emptyList())
        else repository.searchVideos(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResultsChannels = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) flowOf(emptyList())
        else repository.searchChannels(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<SearchHistoryEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getRecentSearches(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monetization & Ads
    val platformSettings: StateFlow<PlatformSettingsEntity> = repository.getPlatformSettings()
        .map { it ?: PlatformSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlatformSettingsEntity())

    val currentUserMonetization: StateFlow<MonetizationProfileEntity?> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getMonetizationProfile(user.id)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allMonetizationProfiles: StateFlow<List<MonetizationProfileEntity>> = repository.getAllMonetizationProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserPayouts: StateFlow<List<PayoutEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getPayoutsForUser(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayouts: StateFlow<List<PayoutEntity>> = repository.getAllPayouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Content Moderation & Reports
    val allReports: StateFlow<List<ReportEntity>> = repository.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAppeals: StateFlow<List<AppealEntity>> = repository.getAllAppeals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentImpressions: StateFlow<List<AdImpressionEntity>> = repository.getRecentImpressions(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flaggedImpressions: StateFlow<List<AdImpressionEntity>> = repository.getFlaggedImpressions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Live Streams & Gifts ---
    val activeLiveStreams: StateFlow<List<LiveStreamEntity>> = repository.getActiveLiveStreams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLiveStreams: StateFlow<List<LiveStreamEntity>> = repository.getAllLiveStreams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val endedLiveStreams: StateFlow<List<LiveStreamEntity>> = repository.getEndedLiveStreams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableGifts: StateFlow<List<LiveGiftEntity>> = repository.getAllGifts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGifts: StateFlow<List<LiveGiftEntity>> = repository.getActiveGifts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCoinPackages: StateFlow<List<CoinPackageEntity>> = repository.getAllCoinPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCoinPackages: StateFlow<List<CoinPackageEntity>> = repository.getActiveCoinPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLiveGiftTransactions: StateFlow<List<LiveGiftTransactionEntity>> = repository.getAllGiftTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchaseOrders: StateFlow<List<CoinPurchaseOrderEntity>> = repository.getAllPurchaseOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentActiveLiveId = MutableStateFlow<String?>(null)

    val currentLiveStream: StateFlow<LiveStreamEntity?> = currentActiveLiveId.flatMapLatest { id ->
        if (id != null) repository.getLiveStreamById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentLiveComments: StateFlow<List<LiveCommentEntity>> = currentActiveLiveId.flatMapLatest { id ->
        if (id != null) repository.getLiveComments(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentStreamViewers: StateFlow<List<LiveViewerSessionEntity>> = currentActiveLiveId.flatMapLatest { id ->
        if (id != null) repository.getViewerSessions(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserPurchaseOrders: StateFlow<List<CoinPurchaseOrderEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getUserPurchaseOrders(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserSentGifts: StateFlow<List<LiveGiftTransactionEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getGiftTransactionsSent(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserReceivedGifts: StateFlow<List<LiveGiftTransactionEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getGiftTransactionsReceived(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            AdMobManager.initialize(application)
            // Seed initial data if first launch
            InitialDataSeeder.seedDatabaseIfEmpty(dao)
            authManager.initialize()

            // Observe raw videos and re-rank with recommendation engine
            combine(rawLongVideos, currentUser, selectedCategory) { videos, user, cat ->
                val filtered = if (cat == "الكل") videos else videos.filter { it.category == cat }
                recommendationEngine.rankVideosForUser(user?.id, filtered)
            }.collect { ranked ->
                _recommendedHomeVideos.value = ranked
            }
        }
    }

    fun openVideoDetail(videoId: String) {
        viewModelScope.launch {
            val video = repository.getVideoByIdDirect(videoId)
            activeVideo.value = video
            if (video != null) {
                activeChannel.value = repository.getChannelByIdDirect(video.channelId)

                // Load comments
                repository.getComments(videoId).collect {
                    activeComments.value = it
                }

                // Check liked, saved, followed state
                currentUser.value?.let { user ->
                    repository.isLiked(user.id, "VIDEO", video.id).collect {
                        isCurrentVideoLiked.value = it
                    }
                }
            }
        }

        viewModelScope.launch {
            val video = repository.getVideoByIdDirect(videoId) ?: return@launch
            currentUser.value?.let { user ->
                repository.isSaved(user.id, video.id).collect {
                    isCurrentVideoSaved.value = it
                }
            }
        }

        viewModelScope.launch {
            val video = repository.getVideoByIdDirect(videoId) ?: return@launch
            currentUser.value?.let { user ->
                repository.isFollowing(user.id, video.channelId).collect {
                    isCurrentChannelFollowed.value = it
                }
            }
        }

        // Record watch history
        currentUser.value?.let { user ->
            viewModelScope.launch {
                repository.recordWatch(user.id, videoId, watchedSeconds = 1, completed = false)
            }
        }
    }

    fun toggleLikeActiveVideo() {
        val user = currentUser.value ?: return
        val video = activeVideo.value ?: return
        viewModelScope.launch {
            val nowLiked = repository.toggleLike(user.id, if (video.isShort) "SHORT" else "VIDEO", video.id)
            isCurrentVideoLiked.value = nowLiked
            // Refresh video entity
            activeVideo.value = repository.getVideoByIdDirect(video.id)
        }
    }

    fun toggleLikeShort(shortVideo: VideoEntity) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleLike(user.id, "SHORT", shortVideo.id)
        }
    }

    fun toggleFollowActiveChannel() {
        val user = currentUser.value ?: return
        val channel = activeChannel.value ?: return
        viewModelScope.launch {
            val nowFollowed = repository.toggleFollow(user.id, channel.id)
            isCurrentChannelFollowed.value = nowFollowed
            activeChannel.value = repository.getChannelByIdDirect(channel.id)
        }
    }

    fun toggleFollowChannel(channelId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleFollow(user.id, channelId)
        }
    }

    fun toggleSaveActiveVideo() {
        val user = currentUser.value ?: return
        val video = activeVideo.value ?: return
        viewModelScope.launch {
            val nowSaved = repository.toggleSave(user.id, video.id)
            isCurrentVideoSaved.value = nowSaved
        }
    }

    fun toggleSaveVideoById(videoId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleSave(user.id, videoId)
        }
    }

    fun addCommentToActiveVideo(content: String, parentId: String?) {
        val user = currentUser.value ?: return
        val video = activeVideo.value ?: return
        viewModelScope.launch {
            repository.addComment(video.id, user.id, content, parentId)
            // Refresh active comments
            repository.getComments(video.id).firstOrNull()?.let {
                activeComments.value = it
            }
        }
    }

    fun addCommentToVideo(videoId: String, content: String, parentId: String?) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.addComment(videoId, user.id, content, parentId)
        }
    }

    fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val user = currentUser.value
        if (user == null) {
            onResult(false, "يرجى تسجيل الدخول أولاً لتقديم بلاغ")
            return
        }
        viewModelScope.launch {
            val res = repository.submitReport(
                reporterUserId = user.id,
                targetId = targetId,
                reportType = targetType,
                reason = reason,
                details = details
            )
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun confirmReportViolation(reportId: String, violationCategory: String, takeDownContent: Boolean = true) {
        val admin = currentUser.value ?: return
        if (!admin.isAdmin) return
        viewModelScope.launch {
            repository.confirmViolation(
                reportId = reportId,
                adminId = admin.id,
                violationCategory = violationCategory,
                takeDownContent = takeDownContent
            )
        }
    }

    fun dismissReport(reportId: String, reason: String) {
        val admin = currentUser.value ?: return
        if (!admin.isAdmin) return
        viewModelScope.launch {
            repository.dismissReport(
                reportId = reportId,
                adminId = admin.id,
                reason = reason
            )
        }
    }

    fun submitAppeal(
        contentId: String?,
        contentType: String,
        strikeNumber: Int,
        reason: String,
        additionalInfo: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val user = currentUser.value
        if (user == null) {
            onResult(false, "يجب تسجيل الدخول أولاً")
            return
        }
        viewModelScope.launch {
            val res = repository.submitAppeal(
                userId = user.id,
                contentId = contentId,
                contentType = contentType,
                strikeNumber = strikeNumber,
                reason = reason,
                additionalInfo = additionalInfo
            )
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun reviewAppeal(appealId: String, isApproved: Boolean, reviewNotes: String) {
        val admin = currentUser.value ?: return
        if (!admin.isAdmin) return
        viewModelScope.launch {
            repository.reviewAppeal(
                appealId = appealId,
                adminId = admin.id,
                isApproved = isApproved,
                reviewNotes = reviewNotes
            )
        }
    }

    val userUploadedVideos: StateFlow<List<VideoEntity>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else {
            dao.getChannelByUserId(user.id).flatMapLatest { ch ->
                if (ch == null) flowOf(emptyList())
                else dao.getAllVideosByChannel(ch.id)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun uploadNewVideo(
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
    ): Job {
        return viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            // Find or get user channel
            val channel = dao.getChannelByUserIdDirect(user.id)
                ?: dao.getAllChannels().firstOrNull()?.firstOrNull()
                ?: return@launch

            repository.uploadVideo(
                channelId = channel.id,
                title = title,
                description = description,
                videoUrl = videoUrl,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = durationSeconds,
                category = category,
                tags = tags,
                isShort = isShort,
                isPublic = isPublic,
                soundTrackTitle = soundTrackTitle
            )
        }
    }

    fun deleteUploadedVideo(videoId: String) {
        viewModelScope.launch {
            dao.deleteVideoById(videoId)
        }
    }

    fun saveSearch(query: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.saveSearchQuery(user.id, query)
        }
    }

    fun deleteSearch(id: Long) {
        viewModelScope.launch { repository.deleteSearch(id) }
    }

    fun clearAllSearches() {
        val user = currentUser.value ?: return
        viewModelScope.launch { repository.clearSearchHistory(user.id) }
    }

    fun clearWatchHistory() {
        val user = currentUser.value ?: return
        viewModelScope.launch { repository.clearWatchHistory(user.id) }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repository.markNotificationAsRead(id) }
    }

    fun clearNotifications() {
        val user = currentUser.value ?: return
        viewModelScope.launch { repository.clearNotifications(user.id) }
    }

    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            repository.deleteVideo(videoId)
        }
    }

    fun recordAdImpression(
        adUnitId: String,
        videoId: String?,
        adFormat: String,
        revenueValue: Double = 0.015,
        currency: String = "USD",
        precision: String = "ESTIMATED"
    ) {
        val viewerId = currentUser.value?.id
        val impId = "imp_" + java.util.UUID.randomUUID().toString().take(12)
        viewModelScope.launch {
            repository.recordAdImpression(
                impressionId = impId,
                adUnitId = adUnitId,
                videoId = videoId,
                viewerId = viewerId,
                adFormat = adFormat,
                revenueValue = revenueValue,
                currency = currency,
                precision = precision
            )
        }
    }

    fun submitMonetizationApplication() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitMonetizationApplication(user.id)
        }
    }

    fun reviewMonetization(userId: String, approve: Boolean, reason: String = "") {
        viewModelScope.launch {
            repository.reviewMonetization(userId, approve, reason)
        }
    }

    fun suspendMonetization(userId: String, reason: String) {
        viewModelScope.launch {
            repository.suspendMonetization(userId, reason)
        }
    }

    suspend fun requestPayout(amount: Double, method: String, details: String): Result<PayoutEntity> {
        val user = currentUser.value ?: return Result.failure(Exception("يرجى تسجيل الدخول أولاً"))
        return repository.requestPayout(user.id, amount, method, details)
    }

    fun updatePayoutStatus(payout: PayoutEntity, newStatus: String, notes: String = "") {
        viewModelScope.launch {
            repository.updatePayoutStatus(payout, newStatus, notes)
        }
    }

    fun updatePlatformSettings(settings: PlatformSettingsEntity) {
        viewModelScope.launch {
            repository.updatePlatformSettings(settings)
        }
    }

    // --- Video More Options Actions ---
    fun markVideoNotInterested(video: VideoEntity, onResult: (String) -> Unit = {}) {
        val user = currentUser.value
        viewModelScope.launch {
            if (user != null) {
                repository.markNotInterested(user.id, video)
            }
            refreshHomeVideos()
            onResult("حسنًا، سنعرض لك محتوى أقل من هذا النوع.")
        }
    }

    fun blockCreator(channelUserId: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val user = currentUser.value
        if (user == null) {
            onResult(false, "يرجى تسجيل الدخول أولاً")
            return
        }
        viewModelScope.launch {
            val res = repository.blockUser(user.id, channelUserId)
            if (res.isSuccess) {
                refreshHomeVideos()
                onResult(true, "تم حظر الحساب بنجاح، ولن يظهر محتواه لك مجدداً.")
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "حدث خطأ أثناء الحظر")
            }
        }
    }

    fun unblockCreator(channelUserId: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.unblockUser(user.id, channelUserId)
            refreshHomeVideos()
            onResult(true, "تم إلغاء حظر الحساب")
        }
    }

    fun dislikeCategory(category: String, onResult: (String) -> Unit = {}) {
        val user = currentUser.value
        viewModelScope.launch {
            if (user != null) {
                repository.dislikeCategory(user.id, category)
            }
            refreshHomeVideos()
            onResult("تم حفظ تفضيلك، وسنقلل من اقتراح مقاطع من تصنيف '$category'.")
        }
    }

    fun editVideoDetails(
        videoId: String,
        title: String,
        description: String,
        category: String,
        tags: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val res = repository.updateVideoDetails(videoId, title, description, category, tags)
            if (res.isSuccess) {
                if (activeVideo.value?.id == videoId) {
                    activeVideo.value = activeVideo.value?.copy(
                        title = title,
                        description = description,
                        category = category,
                        tags = tags
                    )
                }
                refreshHomeVideos()
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun toggleVideoVisibility(videoId: String, isPublic: Boolean, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.toggleVideoVisibility(videoId, isPublic)
            if (activeVideo.value?.id == videoId) {
                activeVideo.value = activeVideo.value?.copy(isPublic = isPublic)
            }
            refreshHomeVideos()
            onResult(if (isPublic) "تم جعل الفيديو عاماً ومتاحاً للجميع" else "تم إخفاء الفيديو وجعله خاصاً")
        }
    }

    fun refreshHomeVideos() {
        viewModelScope.launch {
            val videos = rawLongVideos.value
            val cat = selectedCategory.value
            val filtered = if (cat == "الكل") videos else videos.filter { it.category == cat }
            _recommendedHomeVideos.value = recommendationEngine.rankVideosForUser(currentUser.value?.id, filtered)
        }
    }

    fun refreshAllData() {
        viewModelScope.launch {
            try {
                authManager.initialize()
            } catch (e: Exception) {
                AppCrashReporter.recordError("VidoMixViewModel", "refreshAllData", e)
            }
        }
    }

    fun toggleDarkMode() {
        isDarkMode.value = !isDarkMode.value
    }

    // --- Live Stream Operations ---
    suspend fun checkLiveEligibility(): Result<Boolean> {
        val user = currentUser.value ?: return Result.failure(Exception("يجب تسجيل الدخول أولاً"))
        return repository.checkLiveEligibility(user.id)
    }

    fun isHostFollowed(hostUserId: String): Flow<Boolean> {
        val user = currentUser.value ?: return flowOf(false)
        val channel = allChannels.value.find { it.userId == hostUserId } ?: return flowOf(false)
        return repository.isFollowing(user.id, channel.id)
    }

    fun toggleFollowHostUser(hostUserId: String) {
        val user = currentUser.value ?: return
        val channel = allChannels.value.find { it.userId == hostUserId } ?: return
        viewModelScope.launch {
            repository.toggleFollow(user.id, channel.id)
        }
    }

    suspend fun startLiveStream(
        title: String,
        coverUrl: String,
        category: String,
        isFrontCamera: Boolean,
        isMicEnabled: Boolean,
        commentsAllowed: Boolean,
        giftsAllowed: Boolean
    ): Result<LiveStreamEntity> {
        val user = currentUser.value ?: return Result.failure(Exception("يجب تسجيل الدخول أولاً"))
        val result = repository.startLiveStream(
            hostUserId = user.id,
            title = title,
            coverUrl = coverUrl,
            category = category,
            isFrontCamera = isFrontCamera,
            isMicEnabled = isMicEnabled,
            commentsAllowed = commentsAllowed,
            giftsAllowed = giftsAllowed
        )
        result.onSuccess { stream ->
            currentActiveLiveId.value = stream.id
        }
        return result
    }

    fun openLiveStream(streamId: String) {
        currentActiveLiveId.value = streamId
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null) {
                repository.joinLiveStream(streamId, user)
            }
        }
    }

    fun closeLiveStream(streamId: String) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null) {
                repository.leaveLiveStream(streamId, user.id)
            }
            if (currentActiveLiveId.value == streamId) {
                currentActiveLiveId.value = null
            }
        }
    }

    suspend fun endLiveStream(streamId: String): Result<Unit> {
        val user = currentUser.value ?: return Result.failure(Exception("غير مسجل"))
        val res = repository.endLiveStream(streamId, user.id)
        if (currentActiveLiveId.value == streamId) {
            currentActiveLiveId.value = null
        }
        return res
    }

    suspend fun sendLiveComment(streamId: String, text: String): Result<LiveCommentEntity> {
        val user = currentUser.value ?: return Result.failure(Exception("يجب تسجيل الدخول للتعليق"))
        return repository.sendLiveComment(streamId, user, text)
    }

    suspend fun pinLiveComment(streamId: String, comment: LiveCommentEntity): Result<Unit> {
        return repository.pinLiveComment(streamId, comment)
    }

    suspend fun unpinLiveComment(streamId: String): Result<Unit> {
        return repository.unpinLiveComment(streamId)
    }

    suspend fun deleteLiveComment(commentId: String): Result<Unit> {
        return repository.deleteLiveComment(commentId)
    }

    suspend fun muteUserInLive(streamId: String, targetUserId: String, targetUsername: String): Result<Unit> {
        return repository.muteUserInLive(streamId, targetUserId, targetUsername)
    }

    suspend fun unmuteUserInLive(streamId: String, targetUserId: String): Result<Unit> {
        return repository.unmuteUserInLive(streamId, targetUserId)
    }

    suspend fun banUserFromLive(streamId: String, targetUserId: String, targetUsername: String): Result<Unit> {
        return repository.banUserFromLive(streamId, targetUserId, targetUsername)
    }

    fun addLiveLike(streamId: String, count: Int = 1) {
        viewModelScope.launch {
            repository.addLiveLike(streamId, count)
        }
    }

    suspend fun sendLiveGift(streamId: String, giftId: String, quantity: Int = 1): Result<LiveGiftTransactionEntity> {
        val user = currentUser.value ?: return Result.failure(Exception("يجب تسجيل الدخول لإرسال الهدايا"))
        val result = repository.sendLiveGift(streamId, user.id, giftId, quantity)
        result.onSuccess {
            authManager.refreshCurrentUser()
        }
        return result
    }

    suspend fun purchaseCoins(packageId: String): Result<CoinPurchaseOrderEntity> {
        val user = currentUser.value ?: return Result.failure(Exception("يجب تسجيل الدخول"))
        val result = repository.purchaseCoins(user.id, packageId)
        result.onSuccess {
            authManager.refreshCurrentUser()
        }
        return result
    }

    suspend fun adminBanLiveStream(streamId: String, reason: String): Result<Unit> {
        val user = currentUser.value ?: return Result.failure(Exception("غير مسجل"))
        return repository.adminBanLiveStream(streamId, user.id, reason)
    }

    suspend fun adminSaveGift(gift: LiveGiftEntity): Result<Unit> {
        return repository.adminSaveGift(gift)
    }

    suspend fun adminDeleteGift(giftId: String): Result<Unit> {
        return repository.adminDeleteGift(giftId)
    }

    suspend fun adminSaveCoinPackage(pkg: CoinPackageEntity): Result<Unit> {
        return repository.adminSaveCoinPackage(pkg)
    }

    suspend fun adminDeleteCoinPackage(pkgId: String): Result<Unit> {
        return repository.adminDeleteCoinPackage(pkgId)
    }

    suspend fun adminUpdateLiveRevenueShare(platformShare: Double, creatorShare: Double): Result<Unit> {
        return repository.adminUpdateLiveRevenueShare(platformShare, creatorShare)
    }

    suspend fun toggleLiveNotifications(enabled: Boolean): Result<Unit> {
        val user = currentUser.value ?: return Result.failure(Exception("غير مسجل"))
        val res = repository.toggleLiveNotifications(user.id, enabled)
        res.onSuccess { authManager.refreshCurrentUser() }
        return res
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
