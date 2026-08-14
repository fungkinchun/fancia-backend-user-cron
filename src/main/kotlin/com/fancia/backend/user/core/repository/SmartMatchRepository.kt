package com.fancia.backend.user.core.repository

import com.fancia.backend.shared.user.core.entity.SmartMatch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SmartMatchRepository : JpaRepository<SmartMatch, UUID> {
    fun findByFirstUserId(firstUserId: UUID): List<SmartMatch>

    @Query(
        """
        SELECT s.secondUserId
        FROM SmartMatch s
        WHERE s.firstUserId = :userId
          AND s.firstUserLiked IS NOT NULL
        """,
    )
    fun findFlaggedSecondUserIdsForFirstUser(@Param("userId") userId: UUID): List<UUID>

    @Query(
        """
        SELECT s.firstUserId
        FROM SmartMatch s
        WHERE s.secondUserId = :userId
          AND s.secondUserLiked IS NOT NULL
        """,
    )
    fun findFlaggedFirstUserIdsForSecondUser(@Param("userId") userId: UUID): List<UUID>
}
