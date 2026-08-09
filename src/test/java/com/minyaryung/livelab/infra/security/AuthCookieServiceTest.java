package com.minyaryung.livelab.infra.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTest {

    private final AuthCookieService cookies = new AuthCookieService(true, 86_400_000);

    @Test
    void createsHttpOnlyStrictSecureSessionCookie() {
        String value = cookies.sessionCookie("signed-jwt").toString();

        assertThat(value).contains("livelab_session=signed-jwt", "Path=/", "Secure", "HttpOnly", "SameSite=Strict");
    }

    @Test
    void validatesDoubleSubmitCsrfToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/blog");
        request.setCookies(new Cookie(AuthCookieService.CSRF_COOKIE, "csrf-value"));
        request.addHeader(AuthCookieService.CSRF_HEADER, "csrf-value");

        assertThat(cookies.hasValidCsrfToken(request)).isTrue();

        request.removeHeader(AuthCookieService.CSRF_HEADER);
        request.addHeader(AuthCookieService.CSRF_HEADER, "different");
        assertThat(cookies.hasValidCsrfToken(request)).isFalse();
    }
}
