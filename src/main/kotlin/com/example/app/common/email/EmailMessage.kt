package com.example.app.common.email

data class EmailMessage(
    val to: List<String>,
    val subject: String,
    val body: String,
    val isHtml: Boolean = false,
)
