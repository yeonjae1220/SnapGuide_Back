package yeonjae.snapguide.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.service.fileStorageService.LocalFileStorageService;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class LocalFileStorageServiceTest {
    private LocalFileStorageService localFileStorageService;

    @BeforeEach
    void setUp() {
        localFileStorageService = new LocalFileStorageService();
    }

    @Test
    void saveFile_shouldSaveSuccessfully() throws IOException {
        // given
        MultipartFile multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg",
                "dummy content".getBytes());

        // when
        File savedFile = localFileStorageService.saveFile(multipartFile);

        // then
        assertTrue(savedFile.exists());
//        assertTrue(savedFile.getName().contains("test.jpg")); // 메서드 내부에서 저장할 때 파일 이름 UUID 으로 변경함

        // cleanup
        savedFile.delete();
    }
}