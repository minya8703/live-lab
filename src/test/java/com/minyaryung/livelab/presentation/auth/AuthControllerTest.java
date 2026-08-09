package com.minyaryung.livelab.presentation.auth;

import com.minyaryung.livelab.application.auth.AuthService;
import com.minyaryung.livelab.domain.auth.OAuthVerifier;
import com.minyaryung.livelab.infra.security.AuthCookieService;
import com.minyaryung.livelab.infra.common.LoginAttemptRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void loginReturnsProfileWithoutExposingJwtAndSetsProtectedCookies() {
        AuthService authService = mock(AuthService.class);
        when(authService.verifyOAuth("google-credential")).thenReturn(
                new OAuthVerifier.OAuthResult("master@example.com", "Master", "https://example.com/avatar.png"));
        when(authService.isMaster("master@example.com")).thenReturn(true);
        when(authService.issueToken("master@example.com", "Master", "https://example.com/avatar.png"))
                .thenReturn("signed-jwt");
        LoginAttemptRateLimiter limiter = mock(LoginAttemptRateLimiter.class);
        when(limiter.tryAcquire("203.0.113.10")).thenReturn(true);
        AuthController controller = new AuthController(
                authService, new AuthCookieService(true, 86_400_000), limiter);

        var response = controller.googleLogin(Map.of("credential", "google-credential"), request());

        assertThat(response.getBody()).doesNotContainKey("token");
        assertThat(response.getBody()).containsEntry("master", true);
        List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).hasSize(2);
        assertThat(setCookies.get(0)).contains("livelab_session=signed-jwt", "HttpOnly", "Secure", "SameSite=Strict");
        assertThat(setCookies.get(1)).contains("livelab_csrf=", "Secure", "SameSite=Strict");
        assertThat(setCookies.get(1)).doesNotContain("HttpOnly");
    }

    @Test
    void rejectsRateLimitedLoginBeforeCallingGoogleVerifier() {
        AuthService authService = mock(AuthService.class);
        LoginAttemptRateLimiter limiter = mock(LoginAttemptRateLimiter.class);
        when(limiter.tryAcquire("203.0.113.10")).thenReturn(false);
        AuthController controller = new AuthController(
                authService, new AuthCookieService(true, 86_400_000), limiter);

        assertThatThrownBy(() -> controller.googleLogin(
                Map.of("credential", "google-credential"), request()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(429);
        verify(authService, never()).verifyOAuth("google-credential");
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/google");
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}
