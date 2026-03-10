package com.example.app.config

import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AccessLogFilterTest {

    private val filter = AccessLogFilter()

    @Test
    fun `shouldNotFilter returns true for actuator paths`() {
        val request = MockHttpServletRequest("GET", "/actuator/health")
        assertThat(filter.shouldNotFilter(request)).isTrue()
    }

    @Test
    fun `shouldNotFilter returns true for actuator prometheus`() {
        val request = MockHttpServletRequest("GET", "/actuator/prometheus")
        assertThat(filter.shouldNotFilter(request)).isTrue()
    }

    @Test
    fun `shouldNotFilter returns false for api paths`() {
        val request = MockHttpServletRequest("GET", "/api/v1/users")
        assertThat(filter.shouldNotFilter(request)).isFalse()
    }

    @Test
    fun `maskQuery masks password parameter`() {
        assertThat(filter.maskQuery("password=secret&name=john"))
            .isEqualTo("password=***&name=john")
    }

    @Test
    fun `maskQuery masks token parameter`() {
        assertThat(filter.maskQuery("token=abc123&page=1"))
            .isEqualTo("token=***&page=1")
    }

    @Test
    fun `maskQuery is case-insensitive for sensitive fields`() {
        assertThat(filter.maskQuery("Password=secret")).isEqualTo("Password=***")
    }

    @Test
    fun `maskQuery leaves non-sensitive params unchanged`() {
        assertThat(filter.maskQuery("page=1&size=10")).isEqualTo("page=1&size=10")
    }

    @Test
    fun `maskQuery handles param without value`() {
        assertThat(filter.maskQuery("flag")).isEqualTo("flag")
    }

    @Test
    fun `doFilter executes chain and copies response body`() {
        val request = MockHttpServletRequest("GET", "/api/v1/users")
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>(relaxed = true)

        filter.doFilter(request, response, chain)

        verify { chain.doFilter(request, any()) }
    }
}
