package com.example.wine.image;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.example.wine.model.WineImage;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("production")
public class AzureBlobWineImageStorage implements WineImageStorage {
    private final BlobContainerClient containerClient;

    public AzureBlobWineImageStorage(BlobContainerClient containerClient) {
        this.containerClient = containerClient;
    }

    @Override
    public void save(WineImage image) {
        BlobClient blobClient = containerClient.getBlobClient(image.getId());
        blobClient.upload(BinaryData.fromBytes(image.getContent()), false);
        blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(image.getContentType()));
    }

    @Override
    public Optional<WineImage> findById(String id) {
        BlobClient blobClient = containerClient.getBlobClient(id);
        try {
            WineImage image = new WineImage();
            image.setId(id);
            image.setContentType(blobClient.getProperties().getContentType());
            image.setContent(blobClient.downloadContent().toBytes());
            return Optional.of(image);
        } catch (BlobStorageException exception) {
            if (exception.getStatusCode() == 404) return Optional.empty();
            throw exception;
        }
    }
}