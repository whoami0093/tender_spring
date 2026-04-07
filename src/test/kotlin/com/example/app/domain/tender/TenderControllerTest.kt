package com.example.app.domain.tender

import com.example.app.common.exception.NotFoundException
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import java.math.BigDecimal
import java.time.Instant

@WebMvcTest(TenderController::class)
@ActiveProfiles("test")
@WithMockUser
class TenderControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var service: TenderService

    // ── GET /tenders ──────────────────────────────────────────────────────────

    @Test
    fun `GET tenders returns page response`() {
        every { service.findAll(any(), any()) } returns buildPage(listOf(buildResponse(1L), buildResponse(2L)))

        mockMvc
            .get("/api/v1/tenders")
            .andExpect {
                status { isOk() }
                jsonPath("$.totalElements") { value(2) }
                jsonPath("$.content.length()") { value(2) }
                jsonPath("$.content[0].id") { value(1) }
                jsonPath("$.content[0].purchaseNumber") { value("NUM-1") }
                jsonPath("$.page") { value(0) }
                jsonPath("$.size") { value(20) }
            }
    }

    @Test
    fun `GET tenders with filters returns filtered result`() {
        every { service.findAll(any(), any()) } returns buildPage(listOf(buildResponse(1L)))

        mockMvc
            .get("/api/v1/tenders?region=Москва&takenInWork=false&status=SENT")
            .andExpect {
                status { isOk() }
                jsonPath("$.content.length()") { value(1) }
            }
    }

    @Test
    fun `GET tenders with invalid sort field returns 400`() {
        mockMvc
            .get("/api/v1/tenders?sort=hackedField,asc")
            .andExpect {
                status { isBadRequest() }
            }
    }

    // ── GET /tenders/{id} ────────────────────────────────────────────────────

    @Test
    fun `GET tender by id returns 200`() {
        every { service.findById(1L) } returns buildResponse(1L)

        mockMvc
            .get("/api/v1/tenders/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(1) }
                jsonPath("$.purchaseNumber") { value("NUM-1") }
                jsonPath("$.takenInWork") { value(false) }
            }
    }

    @Test
    fun `GET tender by id returns 404 when not found`() {
        every { service.findById(99L) } throws NotFoundException("Tender with id=99 not found")

        mockMvc
            .get("/api/v1/tenders/99")
            .andExpect {
                status { isNotFound() }
            }
    }

    // ── PATCH /tenders/{id} ──────────────────────────────────────────────────

    @Test
    fun `PATCH tender updates takenInWork and returns 200`() {
        every { service.patch(1L, PatchTenderRequest(takenInWork = true)) } returns buildResponse(1L, takenInWork = true)

        mockMvc
            .patch("/api/v1/tenders/1") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"takenInWork": true}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.takenInWork") { value(true) }
            }
    }

    @Test
    fun `PATCH tender returns 404 when not found`() {
        every { service.patch(99L, any()) } throws NotFoundException("Tender with id=99 not found")

        mockMvc
            .patch("/api/v1/tenders/99") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"takenInWork": true}"""
            }.andExpect {
                status { isNotFound() }
            }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun buildResponse(
        id: Long,
        takenInWork: Boolean = false,
    ) = TenderResponse(
        id = id,
        purchaseNumber = "NUM-$id",
        title = "Tender $id",
        region = "Москва",
        customer = "ООО Тест",
        customerInn = "7701234567",
        amount = BigDecimal("100000.00"),
        currency = "RUB",
        status = TenderStatus.SENT,
        deadline = Instant.parse("2026-05-01T00:00:00Z"),
        publishedAt = Instant.parse("2026-04-01T10:00:00Z"),
        eisUrl = "https://zakupki.gov.ru/$id",
        takenInWork = takenInWork,
        createdAt = Instant.parse("2026-04-07T12:00:00Z"),
        updatedAt = Instant.parse("2026-04-07T12:00:00Z"),
    )

    private fun buildPage(content: List<TenderResponse>) =
        TenderPageResponse(
            content = content,
            totalElements = content.size.toLong(),
            totalPages = 1,
            page = 0,
            size = 20,
        )
}
