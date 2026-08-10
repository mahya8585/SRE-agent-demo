package com.example.wine.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemoController.class)
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void injectingLatencyScenarioReturnsUpdatedIncidentDetails() throws Exception {
        mockMvc.perform(post("/api/demo/scenarios/latency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenario").value("latency"))
                .andExpect(jsonPath("$.status").value("Investigating"))
                .andExpect(jsonPath("$.report").exists())
                .andExpect(jsonPath("$.activeIncidents[0].title").value("Latency spike injected into inventory lookups"));
    }
}
