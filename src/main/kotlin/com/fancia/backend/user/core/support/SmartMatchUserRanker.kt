package com.fancia.backend.user.core.support

import com.fancia.backend.shared.common.tag.core.dto.TagResponse
import com.fancia.backend.shared.common.tag.core.enums.TagType
import com.fancia.backend.shared.user.core.entity.User
import com.fancia.backend.shared.user.core.support.PremiumLimits
import java.util.UUID

class SmartMatchUserRanker(
    private val tagCatalog: SmartMatchTagCatalog,
) {
    fun expandTagWeights(preferences: SmartMatchUserPreferences): Map<UUID, Double> {
        val weights = mutableMapOf<UUID, Double>()
        if (preferences.tagIds.isEmpty()) return weights

        val seedTags = tagCatalog.getTagsByIds(preferences.tagIds)
            .filter { it.id != null && it.id in preferences.tagIds }
        for (tag in seedTags) {
            val tagId = tag.id!!
            weights.merge(tagId, EXACT_TAG_WEIGHT) { existing, added -> maxOf(existing, added) }
            expandSimilarTags(tag, weights)
        }
        return weights
    }

    fun isBlocked(candidate: User, preferences: SmartMatchUserPreferences): Boolean {
        if (candidate.id != null && candidate.id in preferences.blockedUserIds) return true
        if (preferences.blockedTagIds.isNotEmpty() && candidate.tags.any { it in preferences.blockedTagIds }) {
            return true
        }
        return false
    }

    fun score(
        candidate: User,
        tagWeights: Map<UUID, Double>,
        preferences: SmartMatchUserPreferences,
    ): Double {
        var score = BASE_SCORE
        for (candidateTag in candidate.tags) {
            score += tagWeights[candidateTag] ?: 0.0
        }
        val userLocation = preferences.locationLabel?.trim()?.lowercase()
        val candidateLocation = candidate.locationLabel?.trim()?.lowercase()
        if (!userLocation.isNullOrBlank() && !candidateLocation.isNullOrBlank()) {
            if (candidateLocation.contains(userLocation) || userLocation.contains(candidateLocation)) {
                score += LOCATION_BONUS
            }
        }
        if (candidate.premiumActive) {
            score += PremiumLimits.EXPOSURE_SCORE_BONUS
        }
        return score
    }

    fun rank(
        candidates: List<User>,
        preferences: SmartMatchUserPreferences,
        currentUserId: UUID,
    ): List<RankedUser> {
        val tagWeights = expandTagWeights(preferences)
        return candidates
            .asSequence()
            .filter { user -> user.id != null && user.id != currentUserId }
            .filter { user -> !isBlocked(user, preferences) }
            .map { user -> RankedUser(user, score(user, tagWeights, preferences)) }
            .sortedWith(compareByDescending<RankedUser> { it.score }.thenBy { it.user.id })
            .toList()
    }

    private fun expandSimilarTags(seedTag: TagResponse, weights: MutableMap<UUID, Double>) {
        val name = seedTag.name.trim()
        if (name.isEmpty()) return
        val types = linkedSetOf(seedTag.type, TagType.INTEREST, TagType.TOPIC, TagType.EVENT)
        for (type in types) {
            val similar = tagCatalog.searchTags(setOf(name), type, page = 0, size = 8)
            for (tag in similar) {
                val similarId = tag.id ?: continue
                if (similarId == seedTag.id) continue
                weights.merge(similarId, SIMILAR_TAG_WEIGHT) { existing, added -> maxOf(existing, added) }
            }
        }
    }

    companion object {
        const val BASE_SCORE = 1.0
        const val EXACT_TAG_WEIGHT = 10.0
        const val SIMILAR_TAG_WEIGHT = 3.5
        const val LOCATION_BONUS = 4.0
    }
}
