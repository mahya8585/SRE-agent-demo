package com.example.wine.image;

import com.example.wine.model.WineImage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@Service
public class WineImageService {
    public static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;

    private final WineImageStorage imageStorage;

    public WineImageService(WineImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image must not exceed 5 MB");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read image", exception);
        }

        String contentType = detectContentType(content);
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG, and WebP images are supported");
        }

        WineImage image = new WineImage();
        image.setId(UUID.randomUUID().toString());
        image.setContentType(contentType);
        image.setContent(content);
        imageStorage.save(image);
        return "/api/wine-images/" + image.getId();
    }

    public WineImage get(String id) {
        return imageStorage.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
    }

    private String detectContentType(byte[] content) {
        if (startsWith(content, new int[] { 0xFF, 0xD8, 0xFF })) return "image/jpeg";
        if (startsWith(content, new int[] { 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A })) return "image/png";
        if (content.length >= 12
                && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private boolean startsWith(byte[] content, int[] signature) {
        if (content.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if ((content[index] & 0xFF) != signature[index]) return false;
        }
        return true;
    }
}