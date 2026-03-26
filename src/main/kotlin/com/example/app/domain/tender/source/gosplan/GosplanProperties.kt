package com.example.app.domain.tender.source.gosplan

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("zakupki.gosplan")
data class GosplanProperties(
    val baseUrl: String = "https://v2test.gosplan.info",
    val apiKey: String? = null,
    val connTimeoutSeconds: Long = 15,
    val readTimeoutSeconds: Long = 60,
)
