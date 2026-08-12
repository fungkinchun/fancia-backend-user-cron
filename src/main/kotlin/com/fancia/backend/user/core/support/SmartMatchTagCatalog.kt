package com.fancia.backend.user.core.support

import com.fancia.backend.shared.common.tag.core.dto.TagResponse
import com.fancia.backend.shared.common.tag.core.enums.TagType
import java.util.UUID

interface SmartMatchTagCatalog {
    fun getTagsByIds(ids: Set<UUID>): List<TagResponse>

    fun searchTags(
        search: Set<String>,
        type: TagType,
        page: Int = 0,
        size: Int = 20,
    ): List<TagResponse>
}
