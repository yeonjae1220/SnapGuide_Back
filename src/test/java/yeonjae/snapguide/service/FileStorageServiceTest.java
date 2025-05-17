package yeonjae.snapguide.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
    }

    @Test
    void saveFile_shouldSaveSuccessfully() throws IOException {
        // given
        MultipartFile multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg",
                "dummy content".getBytes());

        // when
        File savedFile = fileStorageService.saveFile(multipartFile);

        // then
        assertTrue(savedFile.exists());
        assertTrue(savedFile.getName().contains("test.jpg"));

        // cleanup
        savedFile.delete();
    }
}