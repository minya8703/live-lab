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

    public JwtAuthInterceptor(TokenProvider tokenProvider,
                              @Value("${livelab.auth.master-email}") String masterEmail) {
        this.tokenProvider = tokenProvider;
        this.masterEmail = masterEmail;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("GET".equals(method) && !path.startsWith("/api/blog/admin") && !path.startsWith("/api/blog/upload")) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
            return false;
        }
        try {
            TokenProvider.TokenClaims claims = tokenProvider.parse(authHeader.substring(7));
            if (!masterEmail.equalsIgnoreCase(claims.email())) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "Not authorized");
                return false;
            }
            request.setAttribute("auth.email", claims.email());
            return true;
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return false;
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
