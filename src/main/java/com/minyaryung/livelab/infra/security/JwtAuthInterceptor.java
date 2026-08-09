package com.minyaryung.livelab.infra.security;

import com.minyaryung.livelab.domain.auth.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final TokenProvider tokenProvider;
    private final String masterEmail;
    private final AuthCookieService cookies;

    public JwtAuthInterceptor(TokenProvider tokenProvider,
                              @Value("${livelab.auth.master-email}") String masterEmail,
                              AuthCookieService cookies) {
        this.tokenProvider = tokenProvider;
        this.masterEmail = masterEmail;
        this.cookies = cookies;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("GET".equals(method) && !path.startsWith("/api/blog/admin") && !path.startsWith("/api/blog/upload")) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        boolean bearerRequest = authHeader != null && authHeader.startsWith("Bearer ");
        String token = bearerRequest ? authHeader.substring(7) : cookies.sessionToken(request);
        if (token == null || token.isBlank()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing authentication");
            return false;
        }
        try {
            TokenProvider.TokenClaims claims = tokenProvider.parse(token);
            if (!masterEmail.equalsIgnoreCase(claims.email())) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "Not authorized");
                return false;
            }
            if (!bearerRequest && !isSafeMethod(method) && !cookies.hasValidCsrfToken(request)) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return false;
            }
            request.setAttribute("auth.email", claims.email());
            return true;
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return false;
        }
    }

    private static boolean isSafeMethod(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
