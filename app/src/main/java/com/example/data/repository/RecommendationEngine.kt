package com.example.data.repository

import com.example.data.local.VidoMixDao
import com.example.data.model.VideoEntity
import kotlinx.coroutines.flow.firstOrNull

class RecommendationEngine(private val dao: VidoMixDao) {

    suspend fun rankVideosForUser(
        userId: String?,
        candidateVideos: List<VideoEntity>
    ): List<VideoEntity> {
        if (userId == null || candidateVideos.isEmpty()) {
            return candidateVideos.sortedByDescending { it.viewsCount + (it.likesCount * 3) }
        }

        // Gather user interaction signals
        val watchedList = dao.getWatchHistoryVideos(userId).firstOrNull() ?: emptyList()
        val savedList = dao.getSavedVideosForUser(userId).firstOrNull() ?: emptyList()
        val recentSearches = dao.getRecentSearches(userId, 10).firstOrNull() ?: emptyList()

        val watchedIds = watchedList.map { it.id }.toSet()
        val preferredCategories = (watchedList + savedList)
            .groupingBy { it.category }
            .eachCount()

        val searchTerms = recentSearches.map { it.query.lowercase() }

        // Calculate score for each candidate video
        val scoredVideos = candidateVideos.map { video ->
            var score = 0.0

            // Baseline popularity weighting
            score += (video.viewsCount * 0.001)
            score += (video.likesCount * 0.05)

            // Category affinity boost
            val categoryWeight = preferredCategories[video.category] ?: 0
            score += categoryWeight * 25.0

            // Search history keywords affinity
            for (query in searchTerms) {
                if (video.title.lowercase().contains(query) || video.tags.lowercase().contains(query)) {
                    score += 40.0
                }
            }

            // Freshness boost (videos within last 48 hours get boost)
            val ageHours = (System.currentTimeMillis() - video.uploadTimestamp) / (1000 * 60 * 60)
            if (ageHours < 48) {
                score += (48 - ageHours) * 0.5
            }

            // Penalty for already watched (deprioritize recently watched unless re-watching)
            if (watchedIds.contains(video.id)) {
                score -= 15.0
            }

            video to score
        }

        return scoredVideos.sortedByDescending { it.second }.map { it.first }
    }
}
