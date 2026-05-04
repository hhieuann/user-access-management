package com.r2s.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModeratorControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ModeratorController controller = new ModeratorController();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("TC055 - GET /moderator/dashboard: returns 200 with welcome message")
    void getDashboard_Returns200() throws Exception {
        mockMvc.perform(get("/moderator/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Welcome to Moderator Dashboard"));
    }
}