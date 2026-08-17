package com.example.wine.admin.inventory;

import com.example.wine.image.WineImageService;
import com.example.wine.model.Wine;
import com.example.wine.repository.WineRepository;
import com.example.wine.telemetry.ApplicationTelemetry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInventoryServiceTest {

    @Mock
    private WineRepository wineRepository;
    @Mock
    private ApplicationTelemetry telemetry;
    @Mock
    private WineImageService imageService;

    @Test
    void createsWineWithDetailsAndInitialStock() {
        AdminInventoryService service = new AdminInventoryService(wineRepository, telemetry, imageService);
        CreateWineRequest request = new CreateWineRequest();
        request.setName(" North Ridge ");
        request.setCategory("Red");
        request.setRegion("Napa");
        request.setVariety("Merlot");
        request.setVintage("2022");
        request.setImage("/assets/wines/north-ridge.jpg");
        request.setDescription("  熟した果実と穏やかな樽香。  ");
        request.setPrice(5200.0);
        request.setStock(18);
        when(wineRepository.save(any(Wine.class))).thenAnswer(invocation -> {
            Wine wine = invocation.getArgument(0);
            wine.setId(7L);
            return wine;
        });

        AdminInventoryItem result = service.createWine(request);

        ArgumentCaptor<Wine> savedWine = ArgumentCaptor.forClass(Wine.class);
        verify(wineRepository).save(savedWine.capture());
        assertThat(savedWine.getValue().getName()).isEqualTo("North Ridge");
        assertThat(savedWine.getValue().getRegion()).isEqualTo("Napa");
        assertThat(savedWine.getValue().getVariety()).isEqualTo("Merlot");
        assertThat(savedWine.getValue().getVintage()).isEqualTo("2022");
        assertThat(savedWine.getValue().getDescription()).isEqualTo("熟した果実と穏やかな樽香。");
        assertThat(savedWine.getValue().getStock()).isEqualTo(18);
        assertThat(savedWine.getValue().getThreshold()).isZero();
        assertThat(result.getId()).isEqualTo(7L);
        verify(telemetry).adminWineCreated(7L, 18);
    }
}