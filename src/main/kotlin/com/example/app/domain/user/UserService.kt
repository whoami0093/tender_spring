package com.example.app.domain.user

import com.example.app.common.exception.ConflictException
import com.example.app.common.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun findAll(): List<UserResponse> = userRepository.findAll().map { it.toResponse() }

    @Cacheable(cacheNames = ["users"], key = "#id")
    fun findById(id: Long): UserResponse =
        userRepository.findByIdOrNull(id)?.toResponse()
            ?: throw NotFoundException("User with id=$id not found")

    @Transactional
    fun create(request: CreateUserRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("User with email '${request.email}' already exists")
        }
        val user = User(email = request.email, name = request.name)
        return userRepository.save(user).toResponse().also {
            log.info("Created user id={} email={}", it.id, it.email)
        }
    }

    @Transactional
    @CacheEvict(cacheNames = ["users"], key = "#id")
    fun update(
        id: Long,
        request: UpdateUserRequest,
    ): UserResponse {
        val user =
            userRepository.findByIdOrNull(id)
                ?: throw NotFoundException("User with id=$id not found")
        user.name = request.name
        user.updatedAt = Instant.now()
        return userRepository.save(user).toResponse()
    }

    @Transactional
    @CacheEvict(cacheNames = ["users"], key = "#id")
    fun delete(id: Long) {
        if (!userRepository.existsById(id)) {
            throw NotFoundException("User with id=$id not found")
        }
        userRepository.deleteById(id)
        log.info("Deleted user id={}", id)
    }
}
