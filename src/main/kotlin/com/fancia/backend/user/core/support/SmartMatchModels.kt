package com.fancia.backend.user.core.support

import com.fancia.backend.shared.user.core.entity.User
import java.util.UUID

data class SmartMatchUserPreferences(
    val tagIds: Set<UUID> = emptySet(),
    val blockedUserIds: Set<UUID> = emptySet(),
    val blockedTagIds: Set<UUID> = emptySet(),
    val locationLabel: String? = null,
)

data class RankedUser(
    val user: User,
    val score: Double,
)
