package com.r2s.core.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiResponseWriter - shared security filter response writer")
class ApiResponseWriterTest {

    private final ApiResponseWriter writer =
            new ApiResponseWriter(new ObjectMapper().findAndRegisterModules());

    @Test
    @DisplayName("ARW01 - writeError ghi JSON ApiResponse với status + body đúng")
    void writeError_WritesApiResponseJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.writeError(response, 401, "Invalid token");

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertEquals("UTF-8", response.getCharacterEncoding());

        String body = response.getContentAsString();
        assertTrue(body.contains("\"success\":false"));
        assertTrue(body.contains("\"message\":\"Invalid token\""));
        assertTrue(body.contains("\"timestamp\""));
    }

    @Test
    @DisplayName("ARW02 - writeError với status 429 cũng đúng format")
    void writeError_429() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.writeError(response, 429, "Too many requests");

        assertEquals(429, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains("\"success\":false"));
        assertTrue(body.contains("Too many requests"));
    }
}
