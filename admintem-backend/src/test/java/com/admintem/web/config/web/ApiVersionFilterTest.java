package com.admintem.web.config.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiVersionFilterTest {

    @Test
    void stripsVersionPrefixAndAddsResponseHeader() throws Exception {
        ApiVersionFilter filter = new ApiVersionFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRequestURI("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> forwardedUri = new AtomicReference<>();

        filter.doFilter(request, response,
                (req, res) -> forwardedUri.set(((HttpServletRequest) req).getRequestURI()));

        assertEquals("/auth/login", forwardedUri.get());
        assertEquals("v1", response.getHeader(ApiVersionFilter.VERSION_HEADER));
    }

    @Test
    void leavesLegacyPathUntouched() throws Exception {
        ApiVersionFilter filter = new ApiVersionFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRequestURI("/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> forwardedUri = new AtomicReference<>();

        filter.doFilter(request, response,
                (req, res) -> forwardedUri.set(((HttpServletRequest) req).getRequestURI()));

        assertEquals("/auth/login", forwardedUri.get());
        assertNull(response.getHeader(ApiVersionFilter.VERSION_HEADER));
    }
}
