package com.example.app.domain.tender.subscription

import com.example.app.domain.tender.source.TenderSourceRegistry
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.Instant

@WebMvcTest(SubscriptionController::class)
@ActiveProfiles("test")
@WithMockUser
class SubscriptionControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var service: SubscriptionService

    @MockkBean
    lateinit var registry: TenderSourceRegistry

    // ── GET /subscriptions ────────────────────────────────────────────────────

    @Test
    fun `GET subscriptions returns list`() {
        every { service.findAll() } returns listOf(buildResponse(1L), buildResponse(2L))

        mockMvc
            .get("/api/v1/subscriptions")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].id") { value(1) }
            }
    }

    // ── GET /subscriptions/{id} ───────────────────────────────────────────────

    @Test
    fun `GET subscription by id returns 200`() {
        every { service.findById(1L) } returns buildResponse(1L)

        mockMvc
            .get("/api/v1/subscriptions/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(1) }
                jsonPath("$.source") { value("GOSPLAN_44") }
            }
    }

    // ── POST /subscriptions ───────────────────────────────────────────────────

    @Test
    fun `POST subscriptions creates subscription and returns 201`() {
        every { service.create(any()) } returns buildResponse(10L)

        mockMvc
            .post("/api/v1/subscriptions") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                      "source": "GOSPLAN_44",
                      "label": "Test",
                      "emails": ["user@example.com"],
                      "filters": { "regions": [77], "objectInfo": "строительство" }
                    }
                    """.trimIndent()
            }.andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(10) }
            }
    }

    @Test
    fun `POST subscriptions returns 422 when source is blank`() {
        mockMvc
            .post("/api/v1/subscriptions") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"source": "", "emails": ["user@example.com"]}"""
            }.andExpect {
                status { isUnprocessableEntity() }
            }
    }

    @Test
    fun `POST subscriptions returns 422 when emails list is empty`() {
        mockMvc
            .post("/api/v1/subscriptions") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"source": "GOSPLAN_44", "emails": []}"""
            }.andExpect {
                status { isUnprocessableEntity() }
            }
    }

    // ── PATCH /subscriptions/{id}/status ──────────────────────────────────────

    @Test
    fun `PATCH status pauses subscription`() {
        every { service.updateStatus(1L, SubscriptionStatus.PAUSED) } returns
            buildResponse(1L, status = "PAUSED")

        mockMvc
            .patch("/api/v1/subscriptions/1/status") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"status": "PAUSED"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.status") { value("PAUSED") }
            }
    }

    // ── DELETE /subscriptions/{id} ────────────────────────────────────────────

    @Test
    fun `DELETE subscription returns 204`() {
        every { service.delete(1L) } just runs

        mockMvc
            .delete("/api/v1/subscriptions/1") { with(csrf()) }
            .andExpect {
                status { isNoContent() }
            }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildResponse(
        id: Long,
        status: String = "ACTIVE",
    ) = SubscriptionResponse(
        id = id,
        source = "GOSPLAN_44",
        label = "Test subscription",
        emails = listOf("user@example.com"),
        status = status,
        filters = SubscriptionFiltersRequest(regions = listOf(77)),
        lastCheckedAt = null,
        createdAt = Instant.now(),
    )
}
