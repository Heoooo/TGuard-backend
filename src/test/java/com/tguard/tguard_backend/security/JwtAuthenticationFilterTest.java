package com.tguard.tguard_backend.security;

import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import com.tguard.tguard_backend.user.service.JwtTokenProvider;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final ExposedJwtAuthenticationFilter filter = new ExposedJwtAuthenticationFilter(jwtTokenProvider);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void skipsActuatorHealth() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");

        assertThat(filter.shouldSkip(request)).isTrue();
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void doesNotSkipActuatorMetrics() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/metrics");

        assertThat(filter.shouldSkip(request)).isFalse();
    }

    @Test
    void authenticatesActuatorRequestWhenTokenTenantMatchesHeaderTenant() throws ServletException, IOException {
        TenantContextHolder.setTenantId("0");
        when(jwtTokenProvider.getUsername("admin-token")).thenReturn("admin");
        when(jwtTokenProvider.getRole("admin-token")).thenReturn("ROLE_ADMIN");
        when(jwtTokenProvider.getTenantId("admin-token")).thenReturn("0");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/metrics");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void rejectsActuatorRequestWhenTokenTenantDoesNotMatchHeaderTenant() throws ServletException, IOException {
        TenantContextHolder.setTenantId("tenant-a");
        when(jwtTokenProvider.getUsername("admin-token")).thenReturn("admin");
        when(jwtTokenProvider.getRole("admin-token")).thenReturn("ROLE_ADMIN");
        when(jwtTokenProvider.getTenantId("admin-token")).thenReturn("tenant-b");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/metrics");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static class ExposedJwtAuthenticationFilter extends JwtAuthenticationFilter {

        ExposedJwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
            super(jwtTokenProvider);
        }

        boolean shouldSkip(MockHttpServletRequest request) {
            return shouldNotFilter(request);
        }
    }
}
