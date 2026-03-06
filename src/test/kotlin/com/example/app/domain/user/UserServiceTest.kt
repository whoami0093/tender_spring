package com.example.app.domain.user

import com.example.app.common.exception.ConflictException
import com.example.app.common.exception.NotFoundException
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

class UserServiceTest {

    private val userRepository: UserRepository = mockk()
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository)
    }

    // ── findAll ─────────────────────────────────────────────────────────────

    @Test
    fun `findAll returns mapped list`() {
        val users = listOf(buildUser(id = 1, email = "a@test.com"), buildUser(id = 2, email = "b@test.com"))
        every { userRepository.findAll() } returns users

        val result = userService.findAll()

        assertThat(result).hasSize(2)
        assertThat(result.map { it.email }).containsExactly("a@test.com", "b@test.com")
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    fun `findById returns user when found`() {
        val user = buildUser(id = 1, email = "user@test.com", name = "Alice")
        every { userRepository.findByIdOrNull(1L) } returns user

        val result = userService.findById(1L)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.email).isEqualTo("user@test.com")
        assertThat(result.name).isEqualTo("Alice")
    }

    @Test
    fun `findById throws NotFoundException when user not found`() {
        every { userRepository.findByIdOrNull(99L) } returns null

        assertThatThrownBy { userService.findById(99L) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessageContaining("99")
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    fun `create saves and returns new user`() {
        val request = CreateUserRequest(email = "new@test.com", name = "Bob")
        val savedUser = buildUser(id = 10, email = "new@test.com", name = "Bob")

        every { userRepository.existsByEmail("new@test.com") } returns false
        val slot = slot<User>()
        every { userRepository.save(capture(slot)) } returns savedUser

        val result = userService.create(request)

        assertThat(result.email).isEqualTo("new@test.com")
        assertThat(result.name).isEqualTo("Bob")
        assertThat(slot.captured.email).isEqualTo("new@test.com")
    }

    @Test
    fun `create throws ConflictException when email already exists`() {
        every { userRepository.existsByEmail("dup@test.com") } returns true

        assertThatThrownBy {
            userService.create(CreateUserRequest(email = "dup@test.com", name = "Dave"))
        }.isInstanceOf(ConflictException::class.java)
            .hasMessageContaining("dup@test.com")

        verify(exactly = 0) { userRepository.save(any()) }
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    fun `update modifies name and returns updated user`() {
        val existing = buildUser(id = 1, name = "Old Name")
        val updated = buildUser(id = 1, name = "New Name")

        every { userRepository.findByIdOrNull(1L) } returns existing
        every { userRepository.save(any()) } returns updated

        val result = userService.update(1L, UpdateUserRequest(name = "New Name"))

        assertThat(result.name).isEqualTo("New Name")
    }

    @Test
    fun `update throws NotFoundException for missing user`() {
        every { userRepository.findByIdOrNull(99L) } returns null

        assertThatThrownBy {
            userService.update(99L, UpdateUserRequest(name = "X"))
        }.isInstanceOf(NotFoundException::class.java)
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    fun `delete removes user when found`() {
        every { userRepository.existsById(1L) } returns true
        every { userRepository.deleteById(1L) } just runs

        userService.delete(1L)

        verify { userRepository.deleteById(1L) }
    }

    @Test
    fun `delete throws NotFoundException when user not found`() {
        every { userRepository.existsById(99L) } returns false

        assertThatThrownBy { userService.delete(99L) }
            .isInstanceOf(NotFoundException::class.java)

        verify(exactly = 0) { userRepository.deleteById(any()) }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildUser(
        id: Long = 1L,
        email: String = "test@test.com",
        name: String = "Test User",
        status: UserStatus = UserStatus.ACTIVE,
    ) = User(id = id, email = email, name = name, status = status)
}
