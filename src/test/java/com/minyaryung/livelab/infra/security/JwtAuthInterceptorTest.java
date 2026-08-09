package com.minyaryung.livelab.infra.security;

import com.minyaryung.livelab.domain.auth.TokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthInterceptorTest {

    private final TokenProvider tokenProvider = mock(TokenProvider.class);
    private final AuthCookieService cookies = new AuthCookieService(false, 86_400_000);
    private final JwtAuthInterceptor interceptor =
            new JwtAuthInterceptor(tokenProvider, "master@example.com", cookies);

    @Test
    void acceptsCookieAuthenticationWithCsrfForWriteRequest() throws Exception {
        when(tokenProvider.parse("jwt")).thenReturn(
                new TokenProvider.TokenClaims("master@example.com", "Master", ""));
        MockHttpServletRequest request = writeRequest();
        request.addHeader(AuthCookieService.CSRF_HEADER, "csrf");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(request.getAttribute("auth.email")).isEqualTo("master@example.com");
    }

    @Test
    void rejectsCookieWriteWithoutCsrf() throws Exception {
        when(tokenProvider.parse("jwt")).thenReturn(
                new TokenProvider.TokenClaims("master@example.com", "Master", ""));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(writeRequest(), response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void keepsBearerAuthenticationForTrustedAutomation() throws Exception {
        when(tokenProvider.parse("automation-token")).thenReturn(
                new TokenProvider.TokenClaims("master@example.com", "Master", ""));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/blog");
        request.addHeader("Authorization", "Bearer automation-token");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    private static MockHttpServletRequest writeRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/blog");
        request.setCookies(
                new Cookie(AuthCookieService.SESSION_COOKIE, "jwt"),
                new Cookie(AuthCookieService.CSRF_COOKIE, "csrf"));
        return request;
    }
}
