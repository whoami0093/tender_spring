package com.example.app.domain.tender.source

import com.example.app.common.exception.NotFoundException
import org.springframework.stereotype.Component

@Component
class TenderSourceRegistry(
    sources: List<TenderSource>,
) {
    private val registry = sources.associateBy { it.sourceKey }

    fun get(key: String): TenderSource = registry[key] ?: throw NotFoundException("Unknown tender source: $key")

    fun keys(): Set<String> = registry.keys
}
