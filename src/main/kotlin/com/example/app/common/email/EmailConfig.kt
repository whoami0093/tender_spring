package com.example.app.common.email

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.email")
data class EmailConfig(
    val from: String,
)
