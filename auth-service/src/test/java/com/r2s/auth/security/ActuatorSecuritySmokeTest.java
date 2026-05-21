package com.r2s.auth.security;

import com.r2s.core.event.UserRegisteredEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke test cho actuator endpoint: phai accessible khong can auth.
 *
 * <p>Yeu cau Round 2: thong nhat actuator policy giua 2 service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Actuator security smoke (auth-service)")
class ActuatorSecuritySmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @Test
    @DisplayName("TC107 - GET /actuator/health khong can auth -> 200")
    void actuatorHealth_NoAuth_Returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC108 - GET /actuator/info khong can auth -> 200")
    void actuatorInfo_NoAuth_Returns200() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }
}
