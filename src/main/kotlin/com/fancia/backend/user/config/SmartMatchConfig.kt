package com.fancia.backend.user.config

import com.fancia.backend.shared.common.tag.core.dto.TagResponse
import com.fancia.backend.shared.common.tag.core.enums.TagType
import com.fancia.backend.user.core.repository.TagRepository
import com.fancia.backend.user.core.support.SmartMatchTagCatalog
import com.fancia.backend.user.core.support.SmartMatchUserRanker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.PageRequest
import java.util.UUID

@Configuration
class SmartMatchConfig {
    @Bean
    fun smartMatchTagCatalog(tagRepository: TagRepository): SmartMatchTagCatalog =
        object : SmartMatchTagCatalog {
            override fun getTagsByIds(ids: Set<UUID>): List<TagResponse> {
                if (ids.isEmpty()) return emptyList()
                return tagRepository.findAllById(ids).map { it.toResponse() }
            }

            override fun searchTags(
                search: Set<String>,
                type: TagType,
                page: Int,
                size: Int,
            ): List<TagResponse> {
                if (search.isEmpty()) return emptyList()
                val pageable = PageRequest.of(page, size)
                return search.flatMap { term ->
                    tagRepository.findByTypeAndNameContainingIgnoreCase(type, term.trim(), pageable)
                }.distinctBy { it.id }.map { it.toResponse() }
            }
        }

    @Bean
    fun smartMatchUserRanker(tagCatalog: SmartMatchTagCatalog): SmartMatchUserRanker =
        SmartMatchUserRanker(tagCatalog)

    private fun com.fancia.backend.shared.common.tag.core.entity.Tag.toResponse() =
        TagResponse(
            id = id,
            name = name,
            type = type,
            createdBy = createdBy,
            createdAt = createdAt,
        )
}
