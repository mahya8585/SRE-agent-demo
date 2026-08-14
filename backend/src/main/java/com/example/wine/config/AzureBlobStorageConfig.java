package com.example.wine.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("production")
public class AzureBlobStorageConfig {
    @Bean
    public BlobContainerClient wineImageBlobContainerClient(
            @Value("${azure.storage.blob.endpoint}") String endpoint,
            @Value("${azure.storage.blob.container-name}") String containerName,
            @Value("${azure.client-id}") String managedIdentityClientId) {
        return new BlobContainerClientBuilder()
                .endpoint(endpoint)
                .containerName(containerName)
                .credential(new DefaultAzureCredentialBuilder()
                        .managedIdentityClientId(managedIdentityClientId)
                        .build())
                .buildClient();
    }
}