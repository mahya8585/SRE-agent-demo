package com.example.wine.admin.inventory;

import com.example.wine.image.WineImageService;
import com.example.wine.model.Wine;
import com.example.wine.repository.WineRepository;
import com.example.wine.telemetry.ApplicationTelemetry;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminInventoryService {
    private final WineRepository wineRepository;
    private final ApplicationTelemetry telemetry;
    private final WineImageService imageService;

    public AdminInventoryService(WineRepository wineRepository, ApplicationTelemetry telemetry,
                                 WineImageService imageService) {
        this.wineRepository = wineRepository;
        this.telemetry = telemetry;
        this.imageService = imageService;
    }

    @Transactional(readOnly = true)
    public List<AdminInventoryItem> listInventory() {
        return wineRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(AdminInventoryItem::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminInventoryItem createWine(CreateWineRequest request) {
        return createWine(request, null);
    }

    @Transactional
    public AdminInventoryItem createWine(CreateWineRequest request, MultipartFile image) {
        Wine wine = new Wine();
        wine.setName(request.getName().trim());
        wine.setCategory(request.getCategory().trim());
        wine.setRegion(trimToNull(request.getRegion()));
        wine.setVariety(trimToNull(request.getVariety()));
        wine.setVintage(trimToNull(request.getVintage()));
        String uploadedImagePath = imageService.store(image);
        wine.setImage(uploadedImagePath == null ? trimToNull(request.getImage()) : uploadedImagePath);
        wine.setPrice(request.getPrice());
        wine.setStock(request.getStock());
        wine.setThreshold(0);
        Wine saved = wineRepository.save(wine);
        telemetry.adminWineCreated(saved.getId(), saved.getStock());
        return AdminInventoryItem.from(saved);
    }

    @Transactional
    public AdminInventoryItem updateInventory(Long id, int stock, int threshold) {
        Wine wine = wineRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wine not found"));
        int previousStock = wine.getStock();
        int previousThreshold = wine.getThreshold();
        wine.setStock(stock);
        wine.setThreshold(threshold);
        Wine saved = wineRepository.save(wine);
        telemetry.adminInventoryUpdated(id, previousStock, stock, previousThreshold, threshold);
        return AdminInventoryItem.from(saved);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}