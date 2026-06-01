package com.r2s.core.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResponseBuilder - consistent response construction")
class ResponseBuilderTest {

    private final ResponseBuilder builder = new ResponseBuilder();

    @Test
    @DisplayName("RB01 - buildSuccessResponse(data, message) -> 200 + ApiResponse")
    void buildSuccess_WithMessage() {
        ResponseEntity<ApiResponse<String>> res =
                builder.buildSuccessResponse("data", "ok");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody().isSuccess());
        assertEquals("data", res.getBody().getData());
        assertEquals("ok", res.getBody().getMessage());
    }

    @Test
    @DisplayName("RB02 - buildSuccessResponse(data) -> 200 + message default")
    void buildSuccess_DefaultMessage() {
        ResponseEntity<ApiResponse<String>> res = builder.buildSuccessResponse("data");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("Success", res.getBody().getMessage());
    }

    @Test
    @DisplayName("RB03 - buildCreatedResponse -> 201 CREATED")
    void buildCreated() {
        ResponseEntity<ApiResponse<String>> res =
                builder.buildCreatedResponse("new", "created");

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertTrue(res.getBody().isSuccess());
        assertEquals("created", res.getBody().getMessage());
    }

    @Test
    @DisplayName("RB04 - buildNoContentResponse -> 204")
    void buildNoContent() {
        ResponseEntity<Void> res = builder.buildNoContentResponse();
        assertEquals(HttpStatus.NO_CONTENT, res.getStatusCode());
    }

    @Test
    @DisplayName("RB05 - buildErrorResponse -> 400")
    void buildError() {
        ResponseEntity<ApiResponse<Void>> res = builder.buildErrorResponse("bad");
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertFalse(res.getBody().isSuccess());
        assertEquals("bad", res.getBody().getMessage());
    }

    @Test
    @DisplayName("RB06 - buildNotFoundResponse -> 404")
    void buildNotFound() {
        ResponseEntity<ApiResponse<Void>> res = builder.buildNotFoundResponse("missing");
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
        assertFalse(res.getBody().isSuccess());
    }

    @Test
    @DisplayName("RB07 - buildUnauthorizedResponse -> 401")
    void buildUnauthorized() {
        ResponseEntity<ApiResponse<Void>> res = builder.buildUnauthorizedResponse("no");
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    @DisplayName("RB08 - buildForbiddenResponse -> 403")
    void buildForbidden() {
        ResponseEntity<ApiResponse<Void>> res = builder.buildForbiddenResponse("denied");
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    @Test
    @DisplayName("RB09 - buildConflictResponse -> 409")
    void buildConflict() {
        ResponseEntity<ApiResponse<Void>> res = builder.buildConflictResponse("dup");
        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        assertFalse(res.getBody().isSuccess());
    }
}
