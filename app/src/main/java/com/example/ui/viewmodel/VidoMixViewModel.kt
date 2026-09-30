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

    val shortsList: StateFlow<List<VideoEntity>> = repository.getShorts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Admin Reports
    val allReports: StateFlow<List<ReportEntity>> = repository.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val recentImpressions: StateFlow<List<AdImpressionEntity>> = repository.getRecentImpressions(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flaggedImpressions: StateFlow<List<AdImpressionEntity>> = repository.getFlaggedImpressions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun submitReport(targetType: String, targetId: String, reason: String, details: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitReport(
                reporterUserId = user.id,
                reportType = targetType,
                targetId = targetId,
                reason = reason,
                details = details
            )
        }
    }

    fun uploadNewVideo(
        title: String,
        description: String,
        videoUrl: String,
        thumbnailUrl: String,
        durationSeconds: Int,
        category: String,
        tags: String,
        isShort: Boolean,
        isPublic: Boolean
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
                isPublic = isPublic
            )
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

    fun updateReportStatus(reportId: String, status: String) {
        viewModelScope.launch {
            repository.updateReportStatus(reportId, status)
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

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
