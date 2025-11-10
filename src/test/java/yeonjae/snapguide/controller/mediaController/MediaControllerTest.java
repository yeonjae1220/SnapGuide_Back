package yeonjae.snapguide.controller.mediaController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.service.fileStorageService.FileStorageService;
import yeonjae.snapguide.service.fileStorageService.MediaResponseDto;
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

/**
 * MediaController 단위 테스트
 * 미디어 업로드, 조회, 다운로드 기능 테스트
 */
@WebMvcTest(MediaController.class)
@TestPropertySource(properties = {
        "storage.local.base-dir=/tmp/uploads"
})
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private FileStorageService fileStorageService;

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

    @Test
    @DisplayName("POST /media/upload - 파일 업로드 성공")
    void upload_Success() throws Exception {
        // given
        List<Long> savedIds = Arrays.asList(1L, 2L);
        given(mediaService.saveAll(anyList())).willReturn(savedIds);

        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "image1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image1 content".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "image2.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image2 content".getBytes()
        );

        // when & then
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

        verify(mediaService, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("POST /media/upload - tip 없이 파일만 업로드")
    void upload_WithoutTip_Success() throws Exception {
        // given
        List<Long> savedIds = Arrays.asList(1L);
        given(mediaService.saveAll(anyList())).willReturn(savedIds);

        // when & then
        mockMvc.perform(multipart("/media/upload")
                        .file(mockImageFile)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(mediaService, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("GET /media/list - 미디어 목록 조회")
    void list_Success() throws Exception {
        // given
        Media media1 = Media.builder()
                .id(1L)
                .mediaUrl("/media/files/test1.jpg")
                .build();

        Media media2 = Media.builder()
                .id(2L)
                .mediaUrl("/media/files/test2.jpg")
                .build();

        List<Media> mediaList = Arrays.asList(media1, media2);
        given(mediaService.getAllMedia()).willReturn(mediaList);

        // when & then
        mockMvc.perform(get("/media/list"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].mediaUrl").value("/media/files/test1.jpg"))
                .andExpect(jsonPath("$[1].mediaUrl").value("/media/files/test2.jpg"));

        verify(mediaService, times(1)).getAllMedia();
    }

    @Test
    @DisplayName("GET /media/list - 미디어가 없을 때")
    void list_Empty() throws Exception {
        // given
        given(mediaService.getAllMedia()).willReturn(Arrays.asList());

        // when & then
        mockMvc.perform(get("/media/list"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(mediaService, times(1)).getAllMedia();
    }
}
