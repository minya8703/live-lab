package com.minyaryung.livelab.infra.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    void addsBrowserSecurityHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lab/kafka.html");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String csp = response.getHeader("Content-Security-Policy");
        assertThat(csp)
                .contains("default-src 'self'", "object-src 'none'", "frame-ancestors 'none'",
                        "script-src 'self' https://accounts.google.com https://static.cloudflareinsights.com",
                        "connect-src 'self' https://accounts.google.com https://cloudflareinsights.com");
        assertThat(csp.split("; "))
                .doesNotContain("script-src 'self' https:", "connect-src 'self' https:");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(response.getHeader("Permissions-Policy"))
                .isEqualTo("camera=(), microphone=(), geolocation=()");
        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
    }

    @Test
    void sendsHstsEvenWhenTlsTerminatesBeforeTheApplication() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/"), response, new MockFilterChain());

        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
    }
}
