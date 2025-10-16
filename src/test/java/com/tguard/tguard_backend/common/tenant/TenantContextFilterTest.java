package com.tguard.tguard_backend.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextFilterTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("헤더 기반으로 테넌트 컨텍스트를 설정하고 요청 종료 후 정리한다")
    void bindsTenantFromHeaderAndClearsAfterRequest() throws ServletException, IOException {
        TenantProperties props = new TenantProperties("X-Tenant-Id", "fallback");
        TenantContextFilter filter = new TenantContextFilter(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> tenantDuringChain = new AtomicReference<>();

        FilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                tenantDuringChain.set(TenantContextHolder.getTenantId());
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(tenantDuringChain).hasValue("tenant-123");
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    @DisplayName("헤더가 없을 때 기본 테넌트가 적용된다")
    void fallsBackToDefaultTenantWhenHeaderMissing() throws ServletException, IOException {
        TenantProperties props = new TenantProperties("X-Unknown", "default-tenant");
        TenantContextFilter filter = new TenantContextFilter(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> tenantDuringChain = new AtomicReference<>();

        FilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                tenantDuringChain.set(TenantContextHolder.getTenantId());
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(tenantDuringChain).hasValue("default-tenant");
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }
}
