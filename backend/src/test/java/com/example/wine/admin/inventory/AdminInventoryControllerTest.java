package com.example.wine.admin.inventory;

import com.example.wine.telemetry.ApplicationTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminInventoryController.class)
class AdminInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminInventoryService inventoryService;

    @MockBean
    private ApplicationTelemetry telemetry;

    @Test
    void returnsInventory() throws Exception {
        AdminInventoryItem item = new AdminInventoryItem(1L, "Château Lueur Noire", "Red", 24, 8, 7400.0,
            "黒系果実の凝縮感。");
        when(inventoryService.listInventory()).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Château Lueur Noire"))
                .andExpect(jsonPath("$[0].stock").value(24))
                .andExpect(jsonPath("$[0].threshold").value(8))
                .andExpect(jsonPath("$[0].description").value("黒系果実の凝縮感。"));
    }

    @Test
    void updatesInventoryLevels() throws Exception {
        AdminInventoryItem item = new AdminInventoryItem(1L, "Château Lueur Noire", "Red", 30, 10, 7400.0,
            "更新した説明");
        when(inventoryService.updateInventory(1L, 30, 10, "更新した説明")).thenReturn(item);

        mockMvc.perform(put("/api/admin/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":30,\"threshold\":10,\"description\":\"更新した説明\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(30))
                .andExpect(jsonPath("$.threshold").value(10))
                .andExpect(jsonPath("$.description").value("更新した説明"));
    }

    @Test
    void createsWineWithInitialStock() throws Exception {
        AdminInventoryItem item = new AdminInventoryItem(7L, "North Ridge", "Red", 18, 0, 5200.0, "商品説明");
        when(inventoryService.createWine(org.mockito.ArgumentMatchers.any(CreateWineRequest.class))).thenReturn(item);

        mockMvc.perform(post("/api/admin/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"North Ridge\",\"category\":\"Red\",\"region\":\"Napa\","
                                + "\"variety\":\"Merlot\",\"vintage\":\"2022\",\"image\":\"/wines/north-ridge.jpg\","
                                + "\"description\":\"商品説明\",\"price\":5200,\"stock\":18}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("North Ridge"))
                .andExpect(jsonPath("$.stock").value(18))
                .andExpect(jsonPath("$.description").value("商品説明"));
    }

    @Test
    void rejectsInvalidWine() throws Exception {
        mockMvc.perform(post("/api/admin/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"category\":\"Red\",\"price\":-1,\"stock\":-1}"))
                .andExpect(status().isBadRequest());
    }

            @Test
            void createsWineWithUploadedImage() throws Exception {
            AdminInventoryItem item = new AdminInventoryItem(7L, "North Ridge", "Red", 18, 0, 5200.0, null);
            when(inventoryService.createWine(
                org.mockito.ArgumentMatchers.any(CreateWineRequest.class),
                org.mockito.ArgumentMatchers.any(org.springframework.web.multipart.MultipartFile.class)))
                .thenReturn(item);
            MockMultipartFile wine = new MockMultipartFile(
                "wine", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"name\":\"North Ridge\",\"category\":\"Red\",\"price\":5200,\"stock\":18}".getBytes());
            MockMultipartFile image = new MockMultipartFile(
                "image", "north-ridge.png", MediaType.IMAGE_PNG_VALUE,
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

            mockMvc.perform(multipart("/api/admin/inventory").file(wine).file(image))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.stock").value(18));
            }
}