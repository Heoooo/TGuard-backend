package com.tguard.tguard_backend.common.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.tenant.service.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantProperties tenantProperties;
    private final TenantService tenantService;
    private final ObjectMapper objectMapper;

    private static final Set<String> WHITELIST_PATHS = Set.of(
            "/api/health",
            "/api/auth/login",
            "/api/auth/signup"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (WHITELIST_PATHS.contains(path) || path.startsWith("/api/auth/")) {
            return true;
        }
        return path.startsWith("/api/tenants");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String headerName = tenantProperties.headerOrDefault();
        String tenantId = request.getHeader(headerName);
        if (!StringUtils.hasText(tenantId)) {
            tenantId = tenantProperties.defaultTenantOr(TenantService.GLOBAL_TENANT_ID);
        }
        if (!StringUtils.hasText(tenantId)) {
            writeError(response, HttpStatus.BAD_REQUEST, headerName + " header is required");
            return;
        }
        tenantId = tenantId.trim();
        try {
            tenantService.ensureActiveTenantOrThrow(tenantId);
        } catch (ResponseStatusException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            HttpStatus resolvedStatus = HttpStatus.BAD_REQUEST;
            if (status != null) {
                resolvedStatus = status;
            }
            writeError(response, resolvedStatus, ex.getReason());
            return;
        }

        TenantContextHolder.setTenantId(tenantId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String bodyMessage = status.getReasonPhrase();
        if (message != null) {
            bodyMessage = message;
        }
        ApiResponse<Void> body = ApiResponse.fail(bodyMessage);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
