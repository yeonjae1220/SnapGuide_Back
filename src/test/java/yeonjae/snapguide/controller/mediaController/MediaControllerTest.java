package yeonjae.snapguide.controller.mediaController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.service.fileStorageService.FileServingService;
import yeonjae.snapguide.service.mediaSerivce.MediaService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
@TestPropertySource(properties = {"storage.local.base-dir=/tmp/uploads"})
@DisplayName("MediaController")
@org.springframework.security.test.context.support.WithMockUser
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private FileServingService fileServingService;

    private MockMultipartFile mockImageFile;

    @BeforeEach
    void setUp() {
        mockImageFile = new MockMultipartFile(
                "files",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );
    }

    @Nested
    @DisplayName("POST /media/upload")
    class Upload {

        @Test
        @DisplayName("파일 업로드에 성공하면 저장된 미디어 ID 목록을 반환한다")
        void shouldReturnSavedMediaIdsWhenUploadSucceeds() throws Exception {
            // Arrange
            List<Long> savedIds = Arrays.asList(1L, 2L);
            given(mediaService.saveAll(anyList())).willReturn(savedIds);

            MockMultipartFile file1 = new MockMultipartFile("files", "image1.jpg",
                    MediaType.IMAGE_JPEG_VALUE, "image1 content".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("files", "image2.jpg",
                    MediaType.IMAGE_JPEG_VALUE, "image2 content".getBytes());

            // Act & Assert
            mockMvc.perform(multipart("/media/upload")
                            .file(file1)
                            .file(file2)
                            .param("tip", "Test tip")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0]").value(1))
                    .andExpect(jsonPath("$[1]").value(2));

            verify(mediaService).saveAll(anyList());
        }

        @Test
        @DisplayName("tip 없이 파일만 업로드해도 성공한다")
        void shouldSucceedWhenUploadingWithoutTip() throws Exception {
            // Arrange
            given(mediaService.saveAll(anyList())).willReturn(List.of(1L));

            // Act & Assert
            mockMvc.perform(multipart("/media/upload")
                            .file(mockImageFile)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /media/list")
    class ListMedia {

        @Test
        @DisplayName("미디어 목록을 URL 형태로 반환한다")
        void shouldReturnMediaListWithUrls() throws Exception {
            // Arrange
            List<MediaDto> mediaDtoList = Arrays.asList(
                    new MediaDto("test1.jpg", "/media/files/test1.jpg"),
                    new MediaDto("test2.jpg", "/media/files/test2.jpg")
            );
            given(mediaService.getAllMedia()).willReturn(mediaDtoList);

            // Act & Assert
            mockMvc.perform(get("/media/list"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));

            verify(mediaService).getAllMedia();
        }

        @Test
        @DisplayName("미디어가 없을 때 빈 배열을 반환한다")
        void shouldReturnEmptyListWhenNoMedia() throws Exception {
            // Arrange
            given(mediaService.getAllMedia()).willReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/media/list"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
