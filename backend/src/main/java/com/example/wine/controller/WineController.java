package com.example.wine.controller;

import com.example.wine.model.Wine;
import com.example.wine.repository.WineRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wines")
@CrossOrigin(origins = "*")
public class WineController {
    private final WineRepository wineRepository;

    public WineController(WineRepository wineRepository) {
        this.wineRepository = wineRepository;
    }

    @GetMapping
    public List<Wine> list() {
        return wineRepository.findAll();
    }
}
