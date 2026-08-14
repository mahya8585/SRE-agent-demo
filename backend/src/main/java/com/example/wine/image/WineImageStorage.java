package com.example.wine.image;

import com.example.wine.model.WineImage;

import java.util.Optional;

public interface WineImageStorage {
    void save(WineImage image);
    Optional<WineImage> findById(String id);
}