package com.example.wine.controller;

import com.example.wine.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemoController.class)
@Import(CorsConfig.class)
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

    @Test
    void allowsConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/demo/incidents")
                        .header(ORIGIN, "http://localhost:3001")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3001"));
    }

    @Test
    void rejectsUnknownFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/demo/incidents")
                        .header(ORIGIN, "https://untrusted.example")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
