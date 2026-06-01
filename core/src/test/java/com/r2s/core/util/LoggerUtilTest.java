package com.r2s.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoggerUtil")
class LoggerUtilTest {

    @Test
    @DisplayName("LU01 - getLogger trả logger đúng tên class")
    void getLogger_ReturnsNamedLogger() {
        Logger logger = LoggerUtil.getLogger(LoggerUtilTest.class);

        assertNotNull(logger);
        assertEquals(LoggerUtilTest.class.getName(), logger.getName());
    }
}
