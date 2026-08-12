package com.fancia.backend.user.core.repository

import com.fancia.backend.shared.common.tag.core.entity.Tag
import com.fancia.backend.shared.common.tag.core.enums.TagType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TagRepository : JpaRepository<Tag, UUID> {
    fun findByTypeAndNameContainingIgnoreCase(
        type: TagType,
        name: String,
        pageable: Pageable,
    ): List<Tag>
}
