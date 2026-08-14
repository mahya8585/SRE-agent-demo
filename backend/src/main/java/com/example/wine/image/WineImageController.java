package com.example.wine.image;

import com.example.wine.model.WineImage;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/wine-images")
public class WineImageController {
    private final WineImageService imageService;

    public WineImageController(WineImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable String id) {
        WineImage image = imageService.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
                .body(image.getContent());
    }
}