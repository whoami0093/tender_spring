package com.example.app.domain.tender.source

import com.example.app.common.exception.NotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class TenderSourceRegistryTest {
    private val sourceA =
        object : TenderSource {
            override val sourceKey = "SOURCE_A"

            override fun fetch(
                filters: TenderFilters,
                publishedAfter: Instant,
            ) = emptyList<Tender>()
        }
    private val sourceB =
        object : TenderSource {
            override val sourceKey = "SOURCE_B"

            override fun fetch(
                filters: TenderFilters,
                publishedAfter: Instant,
            ) = emptyList<Tender>()
        }

    private val registry = TenderSourceRegistry(listOf(sourceA, sourceB))

    @Test
    fun `get returns correct source by key`() {
        assertThat(registry.get("SOURCE_A")).isSameAs(sourceA)
        assertThat(registry.get("SOURCE_B")).isSameAs(sourceB)
    }

    @Test
    fun `get throws NotFoundException for unknown key`() {
        assertThatThrownBy { registry.get("UNKNOWN") }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessageContaining("UNKNOWN")
    }

    @Test
    fun `keys returns all registered source keys`() {
        assertThat(registry.keys()).containsExactlyInAnyOrder("SOURCE_A", "SOURCE_B")
    }
}
