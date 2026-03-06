package com.example.app.common.exception

import org.springframework.http.HttpStatus

sealed class AppException(
    message: String,
    val status: HttpStatus,
    val code: String,
) : RuntimeException(message)

class NotFoundException(
    message: String,
    code: String = "NOT_FOUND",
) : AppException(message, HttpStatus.NOT_FOUND, code)

class ConflictException(
    message: String,
    code: String = "CONFLICT",
) : AppException(message, HttpStatus.CONFLICT, code)

class BadRequestException(
    message: String,
    code: String = "BAD_REQUEST",
) : AppException(message, HttpStatus.BAD_REQUEST, code)

class EmailSendException(
    message: String,
    code: String = "EMAIL_SEND_ERROR",
) : AppException(message, HttpStatus.INTERNAL_SERVER_ERROR, code)
