package com.r2s.core.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiResponse - wrapper format")
class ApiResponseTest {

    @Test
    @DisplayName("AR01 - success(data, message) set đúng field")
    void success_WithMessage() {
        ApiResponse<String> res = ApiResponse.success("payload", "done");

        assertTrue(res.isSuccess());
        assertEquals("payload", res.getData());
        assertEquals("done", res.getMessage());
        assertNotNull(res.getTimestamp());
    }

    @Test
    @DisplayName("AR02 - success(data) dùng message default 'Success'")
    void success_DefaultMessage() {
        ApiResponse<Integer> res = ApiResponse.success(42);

        assertTrue(res.isSuccess());
        assertEquals(42, res.getData());
        assertEquals("Success", res.getMessage());
        assertNotNull(res.getTimestamp());
    }

    @Test
    @DisplayName("AR03 - error(message) set success=false, data=null")
    void error_SetsFailure() {
        ApiResponse<Void> res = ApiResponse.error("something wrong");

        assertFalse(res.isSuccess());
        assertNull(res.getData());
        assertEquals("something wrong", res.getMessage());
        assertNotNull(res.getTimestamp());
    }

    @Test
    @DisplayName("AR04 - builder + setter hoạt động (Lombok)")
    void builderAndSetters() {
        ApiResponse<String> res = ApiResponse.<String>builder()
                .success(true)
                .data("x")
                .message("m")
                .build();
        assertEquals("x", res.getData());

        res.setData("y");
        res.setMessage("m2");
        res.setSuccess(false);
        assertEquals("y", res.getData());
        assertEquals("m2", res.getMessage());
        assertFalse(res.isSuccess());
    }
}
