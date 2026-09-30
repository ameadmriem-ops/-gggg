package com.example.ads

import com.example.data.local.VidoMixDao
import java.util.concurrent.ConcurrentHashMap

object AntiFraudEngine {

    // Cache of recent impressions by viewer ID: ViewerId -> List of timestamps
    private val viewerImpressionTimes = ConcurrentHashMap<String, MutableList<Long>>()
    // Set of seen impression IDs to guarantee idempotency and prevent duplicate records
    private val seenImpressionIds = ConcurrentHashMap.newKeySet<String>()

    enum class FraudCheckResult {
        VALID,
        DUPLICATE_EVENT,
        CREATOR_SELF_VIEW,
        BURST_RATE_DETECTED,
        BOT_SUSPICIOUS
    }

    suspend fun evaluateImpression(
        impressionId: String,
        viewerId: String?,
        creatorId: String?,
        dao: VidoMixDao
    ): FraudCheckResult {
        // 1. Deduplication check
        if (seenImpressionIds.contains(impressionId)) {
            return FraudCheckResult.DUPLICATE_EVENT
        }
        seenImpressionIds.add(impressionId)

        // 2. Creator self-view check (prevents creators from inflating their own earnings)
        if (viewerId != null && creatorId != null && viewerId == creatorId) {
            return FraudCheckResult.CREATOR_SELF_VIEW
        }

        // 3. Rapid click / burst rate check
        if (viewerId != null) {
            val now = System.currentTimeMillis()
            val timestamps = viewerImpressionTimes.getOrPut(viewerId) { mutableListOf() }
            synchronized(timestamps) {
                // Remove timestamps older than 60 seconds
                timestamps.removeAll { now - it > 60_000L }
                // If more than 5 ad impressions occurred within 60 seconds from same viewer
                if (timestamps.size >= 5) {
                    return FraudCheckResult.BURST_RATE_DETECTED
                }
                timestamps.add(now)
            }
        }

        return FraudCheckResult.VALID
    }
}
