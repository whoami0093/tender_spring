package com.example.app.domain.tender.source.gosplan

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.Executors

@Configuration
@EnableConfigurationProperties(GosplanProperties::class)
class GosplanClientConfig {

    @Bean
    fun gosplanRestClient(props: GosplanProperties): RestClient {
        val connTimeout = Duration.ofSeconds(props.connTimeoutSeconds)
        val readTimeout = Duration.ofSeconds(props.readTimeoutSeconds)

        val httpClient = HttpClient.newBuilder()
            .connectTimeout(connTimeout)
            .version(HttpClient.Version.HTTP_1_1)
            .executor(Executors.newFixedThreadPool(10))
            .build()

        val factory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(readTimeout)
        }

        val baseUrl = props.baseUrl.ifBlank { "https://v2test.gosplan.info" }

        val builder = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; ZakupkiMonitor/1.0)")
            .defaultHeader("Connection", "close")

        if (!props.apiKey.isNullOrBlank()) {
            builder.defaultHeader("X-Api-Key", props.apiKey)
        }

        return builder.build()
    }
}
