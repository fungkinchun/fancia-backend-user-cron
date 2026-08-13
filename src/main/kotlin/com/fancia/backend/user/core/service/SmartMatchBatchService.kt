package com.fancia.backend.user.core.service

import com.fancia.backend.shared.user.core.entity.SmartMatch
import com.fancia.backend.shared.user.core.entity.User
import com.fancia.backend.shared.user.core.enums.AccountStatus
import com.fancia.backend.shared.user.core.enums.ProfileVisibility
import com.fancia.backend.shared.user.core.support.smartMatchEligible
import com.fancia.backend.user.core.repository.SmartMatchRepository
import com.fancia.backend.user.core.repository.UserRepository
import com.fancia.backend.user.core.support.RankedUser
import com.fancia.backend.user.core.support.SmartMatchUserPreferences
import com.fancia.backend.user.core.support.SmartMatchUserRanker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SmartMatchBatchService(
    private val userRepository: UserRepository,
    private val smartMatchRepository: SmartMatchRepository,
    private val smartMatchUserRanker: SmartMatchUserRanker,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateAll(): Int {
        val users = userRepository.findByStatus(AccountStatus.ACTIVE)
            .filter { it.smartMatchEligible() }
        log.info("Starting Smart Match batch generation for {} smart-match-enabled users", users.size)
        var touched = 0
        for (user in users) {
            val userId = user.id ?: continue
            if (generateForUser(userId) > 0) touched++
        }
        log.info("Finished Smart Match batch generation: usersTouched={}", touched)
        return touched
    }

    @Transactional
    fun generateForUser(userId: UUID): Int {
        val user = userRepository.findById(userId).orElse(null)
        if (user == null) {
            log.debug("Skipping Smart Match batch for userId={}: user not found", userId)
            return 0
        }
        if (!user.smartMatchEligible()) {
            log.debug(
                "Skipping Smart Match batch for userId={}: not eligible status={} visibility={} smartMatchEnabled={}",
                userId,
                user.status,
                user.visibility,
                user.settings?.privacy?.smartMatchEnabled,
            )
            return 0
        }
        val ranked = rankCandidatesForUser(user)
        val flaggedTargets = smartMatchRepository.findFlaggedTargetIdsForUser(userId).toSet()
        val flaggedAsTarget = smartMatchRepository.findFlaggedOwnerIdsWhereUserIsTarget(userId).toSet()
        val excluded = flaggedTargets + flaggedAsTarget
        log.debug(
            "Smart Match ranking for userId={}: ranked={} flaggedTargets={} flaggedAsTarget={}",
            userId,
            ranked.size,
            flaggedTargets.size,
            flaggedAsTarget.size,
        )

        val selected = ranked
            .asSequence()
            .mapNotNull { rankedUser ->
                val targetId = rankedUser.user.id ?: return@mapNotNull null
                if (targetId in excluded) {
                    log.debug(
                        "Excluding already-flagged match userId={} targetId={} score={}",
                        userId,
                        targetId,
                        rankedUser.score,
                    )
                    return@mapNotNull null
                }
                rankedUser
            }
            .take(BATCH_SIZE)
            .toList()
        log.debug("Selected Smart Match batch for userId={}: size={}", userId, selected.size)

        val existingByTarget = smartMatchRepository.findByUserId(userId)
            .filter { it.targetId != null }
            .associateBy { it.targetId!! }

        val keepTargetIds = mutableSetOf<UUID>()
        var upserted = 0
        selected.forEachIndexed { index, rankedUser ->
            val targetId = rankedUser.user.id!!
            keepTargetIds += targetId
            val rank = index + 1
            val existing = existingByTarget[targetId]
            if (existing != null) {
                if (existing.userIdFlag != null) {
                    log.debug(
                        "Keeping flagged Smart Match userId={} targetId={} rank={} score={} userIdFlag={}",
                        userId,
                        targetId,
                        existing.rank,
                        existing.score,
                        existing.userIdFlag,
                    )
                    return@forEachIndexed
                }
                existing.rank = rank
                existing.score = rankedUser.score
                smartMatchRepository.save(existing)
                upserted++
                log.debug(
                    "Updated Smart Match userId={} targetId={} rank={} score={}",
                    userId,
                    targetId,
                    rank,
                    rankedUser.score,
                )
            } else {
                val row = SmartMatch().apply {
                    createdBy = userId
                    this.userId = userId
                    this.targetId = targetId
                    userIdFlag = null
                    targetIdFlag = null
                    this.rank = rank
                    score = rankedUser.score
                }
                smartMatchRepository.save(row)
                upserted++
                log.debug(
                    "Created Smart Match userId={} targetId={} rank={} score={}",
                    userId,
                    targetId,
                    rank,
                    rankedUser.score,
                )
            }
        }

        val staleUnseen = existingByTarget.values.filter { row ->
            row.userIdFlag == null && row.targetId !in keepTargetIds
        }
        if (staleUnseen.isNotEmpty()) {
            log.debug(
                "Deleting stale unseen Smart Match rows for userId={}: count={} targetIds={}",
                userId,
                staleUnseen.size,
                staleUnseen.map { it.targetId },
            )
            smartMatchRepository.deleteAll(staleUnseen)
        }
        return upserted
    }

    fun rankCandidatesForUser(user: User): List<RankedUser> {
        val userId = user.id ?: return emptyList()
        val preferences = SmartMatchUserPreferences(
            tagIds = user.tags,
            blacklistedIds = user.blacklistedIds,
            locationLabel = user.locationLabel,
        )
        val candidates = findSmartMatchUserCandidates(userId, preferences, CANDIDATE_FETCH_SIZE)
        return smartMatchUserRanker.rank(candidates, preferences, userId)
    }

    fun findSmartMatchUserCandidates(
        currentUserId: UUID,
        preferences: SmartMatchUserPreferences,
        fetchSize: Int,
    ): List<User> {
        val visibility = ProfileVisibility.PUBLIC
        val status = AccountStatus.ACTIVE
        val candidates = if (preferences.tagIds.isEmpty()) {
            userRepository.findPublicActiveUsersExcluding(
                currentUserId,
                visibility,
                status,
            )
        } else {
            val expandedTagIds = smartMatchUserRanker.expandTagWeights(preferences).keys
            val tagFilter = expandedTagIds.ifEmpty { preferences.tagIds }
            userRepository.findPublicActiveUsersWithSharedTags(
                tagFilter,
                currentUserId,
                visibility,
                status,
            )
        }
        return candidates
            .filter { it.smartMatchEligible() }
            .take(fetchSize)
    }

    companion object {
        const val BATCH_SIZE = 30
        const val CANDIDATE_FETCH_SIZE = 200
    }
}
