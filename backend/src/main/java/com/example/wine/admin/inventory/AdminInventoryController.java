package com.example.wine.admin.inventory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {
    private final AdminInventoryService inventoryService;

    public AdminInventoryController(AdminInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<AdminInventoryItem> listInventory() {
        return inventoryService.listInventory();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminInventoryItem> createWine(@Valid @RequestBody CreateWineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createWine(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminInventoryItem> createWineWithImage(
            @Valid @RequestPart("wine") CreateWineRequest request,
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createWine(request, image));
    }

    @PutMapping("/{id}")
    public AdminInventoryItem updateInventory(@PathVariable Long id,
                                              @Valid @RequestBody InventoryUpdateRequest request) {
        return inventoryService.updateInventory(id, request.getStock(), request.getThreshold());
    }
}