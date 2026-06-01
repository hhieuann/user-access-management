package com.r2s.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.response.ApiResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("RateLimitFilter - production hardening")
class RateLimitFilterTest {

    // findAndRegisterModules() de ObjectMapper ho tro LocalDateTime (JSR310),
    // giong ObjectMapper ma Spring Boot auto-config trong production.
    private final ApiResponseWriter apiResponseWriter =
            new ApiResponseWriter(new ObjectMapper().findAndRegisterModules());

    /** Tao filter khong co trusted proxy (default). */
    private RateLimitFilter newFilter() {
        return new RateLimitFilter("", apiResponseWriter);
    }

    /** Tao filter co trusted proxy. */
    private RateLimitFilter newFilterWithProxy(String csv) {
        return new RateLimitFilter(csv, apiResponseWriter);
    }

    @Test
    @DisplayName("TC101 - Khong rate limit endpoint khac /auth/login")
    void doFilter_NonLoginEndpoint_PassesThrough() throws Exception {
        RateLimitFilter filter = newFilter();
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/users/me");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        assertEquals(200, res.getStatus()); // chua set -> default 200
    }

    @Test
    @DisplayName("TC102 - Cho phep 5 lan login dau roi block lan thu 6")
    void doFilter_OverLimit_Returns429() throws Exception {
        RateLimitFilter filter = newFilter();
        FilterChain chain = mock(FilterChain.class);

        // 5 lan dau OK
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr("10.0.0.5");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilterInternal(req, res, chain);
            assertNotEquals(429, res.getStatus(), "Lan " + (i + 1) + " phai duoc qua");
        }
        // Lan thu 6 phai 429
        MockHttpServletRequest blocked = new MockHttpServletRequest("POST", "/auth/login");
        blocked.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilterInternal(blocked, blockedRes, chain);
        assertEquals(429, blockedRes.getStatus());
        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    @DisplayName("TC107 - Rate limit 429 tra ApiResponse format (success/message/timestamp)")
    void doFilter_OverLimit_ReturnsApiResponseBody() throws Exception {
        RateLimitFilter filter = newFilter();
        FilterChain chain = mock(FilterChain.class);

        // Dung het quota
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr("10.0.0.77");
            filter.doFilterInternal(req, new MockHttpServletResponse(), chain);
        }
        // Lan thu 6 bi block
        MockHttpServletRequest blocked = new MockHttpServletRequest("POST", "/auth/login");
        blocked.setRemoteAddr("10.0.0.77");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilterInternal(blocked, res, chain);

        assertEquals(429, res.getStatus());
        assertTrue(res.getContentType().contains("application/json"));

        // Verify body theo ApiResponse format
        String body = res.getContentAsString();
        assertTrue(body.contains("\"success\":false"), "Body phai co success=false");
        assertTrue(body.contains("\"message\":\"Too many login attempts. Please try again later.\""),
                "Body phai co message rate limit");
        assertTrue(body.contains("\"timestamp\""), "Body phai co timestamp");
    }

    @Test
    @DisplayName("TC103 - IP khac nhau co quota doc lap")
    void doFilter_DifferentIps_HaveIndependentBuckets() throws Exception {
        RateLimitFilter filter = newFilter();
        FilterChain chain = mock(FilterChain.class);

        // IP A dung het quota
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest reqA = new MockHttpServletRequest("POST", "/auth/login");
            reqA.setRemoteAddr("10.0.0.10");
            filter.doFilterInternal(reqA, new MockHttpServletResponse(), chain);
        }
        // IP B van con quota
        MockHttpServletRequest reqB = new MockHttpServletRequest("POST", "/auth/login");
        reqB.setRemoteAddr("10.0.0.11");
        MockHttpServletResponse resB = new MockHttpServletResponse();
        filter.doFilterInternal(reqB, resB, chain);
        assertNotEquals(429, resB.getStatus());
    }

    @Test
    @DisplayName("TC104 - X-Forwarded-For chi duoc tin neu request den tu trusted proxy")
    void resolveClientIp_TrustedProxy_ReadsXForwardedFor() {
        RateLimitFilter filter = newFilterWithProxy("172.18.0.1");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
        req.setRemoteAddr("172.18.0.1");
        req.addHeader("X-Forwarded-For", "203.0.113.7, 172.18.0.1");

        String ip = filter.resolveClientIp(req);

        assertEquals("203.0.113.7", ip, "Phai parse client that tu X-Forwarded-For");
    }

    @Test
    @DisplayName("TC105 - X-Forwarded-For BI BO QUA neu remoteAddr khong phai trusted proxy")
    void resolveClientIp_UntrustedRemote_IgnoresXForwardedFor() {
        RateLimitFilter filter = newFilterWithProxy("172.18.0.1");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
        req.setRemoteAddr("203.0.113.99");
        req.addHeader("X-Forwarded-For", "1.2.3.4"); // attacker fake

        String ip = filter.resolveClientIp(req);

        assertEquals("203.0.113.99", ip, "Khong duoc tin XFF tu non-trusted source");
    }

    @Test
    @DisplayName("TC106 - Bucket cu (qua TTL) bi evict")
    void evictExpiredBuckets_RemovesStaleEntries() throws Exception {
        RateLimitFilter filter = newFilter();

        // Tao 1 bucket bang cach goi /auth/login
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
        req.setRemoteAddr("10.0.0.99");
        filter.doFilterInternal(req, new MockHttpServletResponse(), chain);
        assertEquals(1, filter.bucketCount());

        // Reflect: set lastAccess thanh 1 gio truoc -> qua TTL (10 phut)
        Field bucketsField = RateLimitFilter.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, RateLimitFilter.BucketEntry> bucketsMap =
                (Map<String, RateLimitFilter.BucketEntry>) bucketsField.get(filter);
        RateLimitFilter.BucketEntry entry = bucketsMap.get("10.0.0.99");
        Field lastAccessField = entry.getClass().getDeclaredField("lastAccess");
        lastAccessField.setAccessible(true);
        lastAccessField.set(entry, Instant.now().minusSeconds(3600));

        // Trigger eviction
        filter.evictExpiredBuckets();

        assertEquals(0, filter.bucketCount(), "Bucket cu phai bi don");
    }
}
