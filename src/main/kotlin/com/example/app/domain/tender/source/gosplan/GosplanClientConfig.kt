package com.example.app.domain.tender.source.gosplan

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(GosplanProperties::class)
class GosplanClientConfig {

    @Bean
    fun gosplanRestClient(props: GosplanProperties): RestClient {
        val timeoutMs = (props.timeoutSeconds * 1000).toInt()
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(timeoutMs)
            setReadTimeout(timeoutMs)
        }
        val builder = RestClient.builder()
            .baseUrl(props.baseUrl)
            .requestFactory(factory)
            .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; ZakupkiMonitor/1.0)")
        if (!props.apiKey.isNullOrBlank()) {
            builder.defaultHeader("X-Api-Key", props.apiKey!!)
        }
        return builder.build()
    }
}
