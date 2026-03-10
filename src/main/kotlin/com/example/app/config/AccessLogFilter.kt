package com.example.app.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper

@Component
class AccessLogFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(AccessLogFilter::class.java)

    private val sensitiveFields = setOf("password", "token", "authorization", "secret", "key")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val start = System.currentTimeMillis()
        val wrapped = ContentCachingResponseWrapper(response)

        chain.doFilter(request, wrapped)

        val duration = System.currentTimeMillis() - start
        val uri = request.requestURI
        val query = request.queryString?.let { maskQuery(it) } ?: ""
        val fullUri = if (query.isNotEmpty()) "$uri?$query" else uri

        log.info(
            "method={} uri={} status={} duration_ms={} response_size={}",
            request.method,
            fullUri,
            wrapped.status,
            duration,
            wrapped.contentSize,
        )

        wrapped.copyBodyToResponse()
    }

    public override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.requestURI.startsWith("/actuator")

    internal fun maskQuery(query: String): String =
        query.split("&").joinToString("&") { param ->
            val eqIdx = param.indexOf('=')
            if (eqIdx == -1) return@joinToString param
            val key = param.substring(0, eqIdx)
            val value = param.substring(eqIdx + 1)
            if (key.lowercase() in sensitiveFields) "$key=***" else "$key=$value"
        }
}
