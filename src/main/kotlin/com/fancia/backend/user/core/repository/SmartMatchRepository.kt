package com.fancia.backend.user.core.repository

import com.fancia.backend.shared.user.core.entity.SmartMatch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SmartMatchRepository : JpaRepository<SmartMatch, UUID> {
    fun findByUserId(userId: UUID): List<SmartMatch>

    @Query(
        """
        SELECT s.targetId
        FROM SmartMatch s
        WHERE s.userId = :userId
          AND s.userIdFlag IS NOT NULL
        """,
    )
    fun findFlaggedTargetIdsForUser(@Param("userId") userId: UUID): List<UUID>

    @Query(
        """
        SELECT s.userId
        FROM SmartMatch s
        WHERE s.targetId = :userId
          AND s.targetIdFlag IS NOT NULL
        """,
    )
    fun findFlaggedOwnerIdsWhereUserIsTarget(@Param("userId") userId: UUID): List<UUID>
}
