package com.example.app.domain.tender.source.gosplan

import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.Timeout
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(GosplanProperties::class)
class GosplanClientConfig {

    @Bean
    fun gosplanRestClient(props: GosplanProperties): RestClient {
        val connConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(props.connTimeoutSeconds))
            .build()

        val connManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnPerRoute(5)
            .setMaxConnTotal(10)
            .setDefaultConnectionConfig(connConfig)
            .build()

        val requestConfig = RequestConfig.custom()
            .setResponseTimeout(Timeout.ofSeconds(props.readTimeoutSeconds))
            .build()

        val httpClient = HttpClients.custom()
            .setConnectionManager(connManager)
            .setDefaultRequestConfig(requestConfig)
            .build()

        val factory = HttpComponentsClientHttpRequestFactory(httpClient)

        val baseUrl = props.baseUrl.ifBlank { "https://v2test.gosplan.info" }

        val builder = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; ZakupkiMonitor/1.0)")

        if (!props.apiKey.isNullOrBlank()) {
            builder.defaultHeader("X-Api-Key", props.apiKey)
        }

        return builder.build()
    }
}
