package com.example.wine.controller;

import com.example.wine.model.Wine;
import com.example.wine.repository.WineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WineRepository wineRepository;

    @Test
    void createsOrderAndReducesStock() throws Exception {
        Wine wine = new Wine();
        wine.setId(1L);
        wine.setName("Château Lueur Noire");
        wine.setPrice(7400.0);
        wine.setStock(24);
        when(wineRepository.findById(1L)).thenReturn(Optional.of(wine));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"山田 太郎\",\"email\":\"taro@example.com\",\"address\":\"東京都港区1-1\",\"items\":[{\"wineId\":1,\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.subtotal").value(14800.0))
                .andExpect(jsonPath("$.shipping").value(800.0))
                .andExpect(jsonPath("$.total").value(15600.0));

        verify(wineRepository).saveAll(anyList());
        org.junit.jupiter.api.Assertions.assertEquals(22, wine.getStock());
    }

    @Test
    void rejectsOrderWhenRequestedQuantityExceedsStock() throws Exception {
        Wine wine = new Wine();
        wine.setId(1L);
        wine.setName("Château Lueur Noire");
        wine.setPrice(7400.0);
        wine.setStock(1);
        when(wineRepository.findById(1L)).thenReturn(Optional.of(wine));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"山田 太郎\",\"email\":\"taro@example.com\",\"address\":\"東京都港区1-1\",\"items\":[{\"wineId\":1,\"quantity\":2}]}"))
                .andExpect(status().isConflict());

        verify(wineRepository, never()).saveAll(anyList());
        org.junit.jupiter.api.Assertions.assertEquals(1, wine.getStock());
    }
}