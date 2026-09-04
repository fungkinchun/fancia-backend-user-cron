package com.fancia.backend.user.core.repository

import com.fancia.backend.shared.common.moderation.core.entity.BlockedResource
import com.fancia.backend.shared.common.moderation.core.entity.BlockedResourceId
import com.fancia.backend.shared.common.moderation.core.enums.BlockedResourceType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BlockedResourceRepository : JpaRepository<BlockedResource, BlockedResourceId> {
    fun findAllByIdUserIdAndIdResourceTypeIn(
        userId: UUID,
        resourceTypes: Collection<BlockedResourceType>,
    ): List<BlockedResource>
}
