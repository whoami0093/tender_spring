package com.example.app.domain.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateUserRequest(
    @field:Email(message = "must be a valid email")
    @field:NotBlank(message = "must not be blank")
    val email: String,

    @field:NotBlank(message = "must not be blank")
    @field:Size(min = 2, max = 100, message = "must be between 2 and 100 characters")
    val name: String,
)

data class UpdateUserRequest(
    @field:NotBlank(message = "must not be blank")
    @field:Size(min = 2, max = 100, message = "must be between 2 and 100 characters")
    val name: String,
)

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val status: UserStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun User.toResponse() = UserResponse(
    id = id,
    email = email,
    name = name,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
