package com.example.wine.image;

import com.example.wine.model.WineImage;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Profile("!production")
public class InMemoryWineImageStorage implements WineImageStorage {
    private final ConcurrentMap<String, WineImage> images = new ConcurrentHashMap<>();

    @Override
    public void save(WineImage image) {
        images.put(image.getId(), image);
    }

    @Override
    public Optional<WineImage> findById(String id) {
        return Optional.ofNullable(images.get(id));
    }
}