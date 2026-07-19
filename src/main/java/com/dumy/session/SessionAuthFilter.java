package com.dumy.session;

import com.dumy.common.ApiResponse;
import com.dumy.entity.Session;
import com.dumy.exception.BusinessException;
import com.dumy.exception.ErrorCode;
import com.dumy.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SessionAuthFilter extends OncePerRequestFilter {

    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    private static final List<String> ALLOWLIST_PREFIXES = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/swagger-ui",
            "/v3/api-docs",
            "/h2-console"
    );

    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return ALLOWLIST_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(SESSION_TOKEN_HEADER);

        if (token == null || token.isBlank()) {
            writeError(response, ErrorCode.ERROR_401_3008);
            return;
        }

        try {
            Session session = sessionService.validate(token);
            SessionContext.set(session.getId(), session.getUserId(), session.getTenantId());
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            writeError(response, ex.getErrorCode());
        } finally {
            SessionContext.clear();
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(errorCode)));
    }
}
