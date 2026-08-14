package com.example.wine.image;

import com.example.wine.model.WineImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WineImageServiceTest {

    @Mock
    private WineImageStorage imageStorage;

    @Test
    void storesPngUsingDetectedContentType() {
        WineImageService service = new WineImageService(imageStorage);
        byte[] content = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("image", "wine.png", "text/plain", content);

        String path = service.store(file);

        ArgumentCaptor<WineImage> savedImage = ArgumentCaptor.forClass(WineImage.class);
        verify(imageStorage).save(savedImage.capture());
        assertThat(savedImage.getValue().getContentType()).isEqualTo("image/png");
        assertThat(savedImage.getValue().getContent()).isEqualTo(content);
        assertThat(path).isEqualTo("/api/wine-images/" + savedImage.getValue().getId());
    }

    @Test
    void rejectsUnsupportedContent() {
        WineImageService service = new WineImageService(imageStorage);
        MockMultipartFile file = new MockMultipartFile("image", "wine.svg", "image/svg+xml", "<svg/>".getBytes());

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void rejectsImageLargerThanFiveMegabytes() {
        WineImageService service = new WineImageService(imageStorage);
        MockMultipartFile file = new MockMultipartFile(
                "image", "wine.png", "image/png", new byte[(int) WineImageService.MAX_IMAGE_SIZE + 1]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }
}