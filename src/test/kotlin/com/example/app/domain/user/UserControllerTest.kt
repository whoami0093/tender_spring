package com.example.app.domain.user

import com.example.app.common.exception.NotFoundException
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
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Instant

@WebMvcTest(UserController::class)
@ActiveProfiles("test")
@WithMockUser
class UserControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var userService: UserService

    // ── GET /users ────────────────────────────────────────────────────────────

    @Test
    fun `GET users returns list of all users`() {
        every { userService.findAll() } returns listOf(
            buildResponse(1L, "a@test.com", "Alice"),
            buildResponse(2L, "b@test.com", "Bob"),
        )

        mockMvc.get("/api/v1/users")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].id") { value(1) }
                jsonPath("$[0].email") { value("a@test.com") }
                jsonPath("$[1].id") { value(2) }
            }
    }

    @Test
    fun `GET users returns empty array when no users exist`() {
        every { userService.findAll() } returns emptyList()

        mockMvc.get("/api/v1/users")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
    }

    // ── GET /users/{id} ───────────────────────────────────────────────────────

    @Test
    fun `GET user by id returns 200 with user data`() {
        every { userService.findById(1L) } returns buildResponse(1L, "user@test.com", "Alice")

        mockMvc.get("/api/v1/users/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(1) }
                jsonPath("$.email") { value("user@test.com") }
                jsonPath("$.name") { value("Alice") }
                jsonPath("$.status") { value("ACTIVE") }
            }
    }

    @Test
    fun `GET user by id returns 404 when not found`() {
        every { userService.findById(99L) } throws NotFoundException("User with id=99 not found")

        mockMvc.get("/api/v1/users/99")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("NOT_FOUND") }
            }
    }

    // ── POST /users ───────────────────────────────────────────────────────────

    @Test
    fun `POST users creates user and returns 201`() {
        every { userService.create(any()) } returns buildResponse(10L, "new@test.com", "Bob")

        mockMvc.post("/api/v1/users") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "new@test.com", "name": "Bob"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(10) }
            jsonPath("$.email") { value("new@test.com") }
            jsonPath("$.name") { value("Bob") }
        }
    }

    @Test
    fun `POST users returns 422 when email is blank`() {
        mockMvc.post("/api/v1/users") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "", "name": "Bob"}"""
        }.andExpect {
            status { isUnprocessableEntity() }
            jsonPath("$.code") { value("VALIDATION_ERROR") }
        }
    }

    @Test
    fun `POST users returns 422 when email format is invalid`() {
        mockMvc.post("/api/v1/users") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "not-an-email", "name": "Bob"}"""
        }.andExpect {
            status { isUnprocessableEntity() }
        }
    }

    @Test
    fun `POST users returns 422 when name is too short`() {
        mockMvc.post("/api/v1/users") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "valid@test.com", "name": "A"}"""
        }.andExpect {
            status { isUnprocessableEntity() }
        }
    }

    @Test
    fun `POST users returns 422 when name is blank`() {
        mockMvc.post("/api/v1/users") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "valid@test.com", "name": ""}"""
        }.andExpect {
            status { isUnprocessableEntity() }
        }
    }

    // ── PUT /users/{id} ───────────────────────────────────────────────────────

    @Test
    fun `PUT users updates user name and returns 200`() {
        every { userService.update(1L, any()) } returns buildResponse(1L, "user@test.com", "New Name")

        mockMvc.put("/api/v1/users/1") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": "New Name"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(1) }
            jsonPath("$.name") { value("New Name") }
        }
    }

    @Test
    fun `PUT users returns 404 when user not found`() {
        every { userService.update(99L, any()) } throws NotFoundException("User with id=99 not found")

        mockMvc.put("/api/v1/users/99") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": "New Name"}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("NOT_FOUND") }
        }
    }

    @Test
    fun `PUT users returns 422 when name is too short`() {
        mockMvc.put("/api/v1/users/1") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": "A"}"""
        }.andExpect {
            status { isUnprocessableEntity() }
        }
    }

    // ── DELETE /users/{id} ────────────────────────────────────────────────────

    @Test
    fun `DELETE user returns 204`() {
        every { userService.delete(1L) } just runs

        mockMvc.delete("/api/v1/users/1") { with(csrf()) }
            .andExpect {
                status { isNoContent() }
            }
    }

    @Test
    fun `DELETE user returns 404 when not found`() {
        every { userService.delete(99L) } throws NotFoundException("User with id=99 not found")

        mockMvc.delete("/api/v1/users/99") { with(csrf()) }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("NOT_FOUND") }
            }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildResponse(
        id: Long,
        email: String = "user@test.com",
        name: String = "Test User",
    ) = UserResponse(
        id = id,
        email = email,
        name = name,
        status = UserStatus.ACTIVE,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )
}
