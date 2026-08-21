package com.tguard.tguard_backend.security;

import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import com.tguard.tguard_backend.user.service.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/webhooks/")
                || uri.startsWith("/api/tenants/")
                || uri.startsWith("/actuator/")
                || uri.equals("/api/health")
                || uri.equals("/")
                || uri.equals("/favicon.ico")
                || uri.equals("/api/auth/login")
                || uri.equals("/api/auth/signup");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String username = jwtTokenProvider.getUsername(token);
                String role = jwtTokenProvider.getRole(token);
                String tokenTenantId = jwtTokenProvider.getTenantId(token);

                if (!isSameTenant(tokenTenantId, TenantContextHolder.getTenantId())) {
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant id does not match token");
                    return;
                }

                List<SimpleGrantedAuthority> authorities = List.of();
                if (role != null) {
                    authorities = List.of(new SimpleGrantedAuthority(role));
                }

                var auth = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isSameTenant(String tokenTenantId, String requestTenantId) {
        if (!StringUtils.hasText(tokenTenantId) || !StringUtils.hasText(requestTenantId)) {
            return false;
        }
        return Objects.equals(tokenTenantId.trim(), requestTenantId.trim());
    }
}
