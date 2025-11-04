package com.tguard.tguard_backend.common.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tguard.tguard_backend.tenant.service.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TenantContextFilterTest {

    private final TenantService tenantService = mock(TenantService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        Mockito.reset(tenantService);
    }

    @Test
    @DisplayName("헤더 기반으로 테넌트 컨텍스트를 설정한다")
    void bindsTenantFromHeaderAndClearsAfterRequest() throws ServletException, IOException {
        TenantProperties props = new TenantProperties("X-Tenant-Id", "fallback");
        TenantContextFilter filter = new TenantContextFilter(props, tenantService, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                chainInvoked.set(true);
                assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-123");
            }
        };

        filter.doFilter(request, response, chain);

        verify(tenantService).ensureActiveTenantOrThrow("tenant-123");
        assertThat(chainInvoked).isTrue();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    @DisplayName("헤더가 없으면 400 에러를 반환한다")
    void returnsBadRequestWhenHeaderMissing() throws ServletException, IOException {
        TenantProperties props = new TenantProperties("X-Tenant-Id", "fallback");
        TenantContextFilter filter = new TenantContextFilter(props, tenantService, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                chainInvoked.set(true);
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(chainInvoked).isFalse();
        verifyNoInteractions(tenantService);
    }
}
