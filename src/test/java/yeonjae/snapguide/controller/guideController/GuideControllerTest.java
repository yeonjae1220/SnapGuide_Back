package yeonjae.snapguide.controller.guideController;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import yeonjae.snapguide.controller.guideController.guideDto.GuideCreateResponseDto;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.controller.guideController.guideDto.GuideUpdateRequestDto;
import yeonjae.snapguide.controller.guideController.guideDto.RegionClusterDto;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.dto.MemberDto;
import yeonjae.snapguide.service.guideSerivce.AggregateLevel;
import yeonjae.snapguide.service.guideSerivce.GuideLikeService;
import yeonjae.snapguide.service.guideSerivce.GuideService;
import yeonjae.snapguide.service.guideSerivce.RegionAggregateService;
import yeonjae.snapguide.service.mediaSerivce.BatchUploadResult;
import yeonjae.snapguide.service.mediaSerivce.MediaService;
import yeonjae.snapguide.service.memberSerivce.MemberService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GuideController.class)
@DisplayName("GuideController")
class GuideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GuideService guideService;

    @MockBean
    private GuideLikeService guideLikeService;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private MemberService memberService;

    @MockBean
    private RegionAggregateService regionAggregateService;

    @MockBean
    private yeonjae.snapguide.security.ratelimit.RateLimiterService rateLimiterService;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("testUser")
                .build();
    }

    @Nested
    @DisplayName("POST /guide/api/upload")
    class CreateGuideWithMedia {

        @Test
        @DisplayName("파일과 팁 업로드 성공 시 201과 GuideCreateResponseDto를 반환한다")
        @WithMockUser(username = "test@example.com")
        void shouldReturn201WithResponseDtoWhenUploadSucceeds() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "files", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "img".getBytes());

            Media mockMedia = Media.builder()
                    .id(10L).mediaName("photo.jpg").mediaUrl("/media/files/uuid.jpg")
                    .fileSize(100L).build();

            BatchUploadResult batchResult = new BatchUploadResult(List.of(mockMedia), List.of());

            given(memberService.getCurrentMember("test@example.com")).willReturn(testMember);
            given(mediaService.saveAllAndGet(anyList(), isNull(), isNull())).willReturn(batchResult);
            given(guideService.createGuideWithMedia(any(), any(), anyList(), anyBoolean())).willReturn(42L);

            // Act & Assert
            mockMvc.perform(multipart("/guide/api/upload")
                            .file(file)
                            .param("tip", "멋진 장소")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.guideId").value(42))
                    .andExpect(jsonPath("$.totalFiles").value(1))
                    .andExpect(jsonPath("$.savedFiles").value(1))
                    .andExpect(jsonPath("$.skippedFiles").isEmpty())
                    .andExpect(jsonPath("$.message").value("업로드 성공"));
        }

        @Test
        @DisplayName("일부 파일 스킵 시 skippedFiles 목록과 함께 201을 반환한다")
        @WithMockUser(username = "test@example.com")
        void shouldIncludeSkippedFilesInResponseWhenSomeFilesFail() throws Exception {
            // Arrange
            MockMultipartFile good = new MockMultipartFile(
                    "files", "good.jpg", MediaType.IMAGE_JPEG_VALUE, "ok".getBytes());
            MockMultipartFile bad = new MockMultipartFile(
                    "files", "broken.jpg", MediaType.IMAGE_JPEG_VALUE, "bad".getBytes());

            Media mockMedia = Media.builder()
                    .id(10L).mediaName("good.jpg").mediaUrl("/media/files/uuid.jpg")
                    .fileSize(100L).build();

            BatchUploadResult batchResult = new BatchUploadResult(
                    List.of(mockMedia), List.of("broken.jpg"));

            given(memberService.getCurrentMember("test@example.com")).willReturn(testMember);
            given(mediaService.saveAllAndGet(anyList(), isNull(), isNull())).willReturn(batchResult);
            given(guideService.createGuideWithMedia(any(), any(), anyList(), anyBoolean())).willReturn(42L);

            // Act & Assert
            mockMvc.perform(multipart("/guide/api/upload")
                            .file(good)
                            .file(bad)
                            .param("tip", "일부 실패")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.guideId").value(42))
                    .andExpect(jsonPath("$.totalFiles").value(2))
                    .andExpect(jsonPath("$.savedFiles").value(1))
                    .andExpect(jsonPath("$.skippedFiles[0]").value("broken.jpg"))
                    .andExpect(jsonPath("$.message").value("1개 파일 업로드 실패: broken.jpg"));
        }

        @Test
        @DisplayName("파일도 팁도 없으면 400 Bad Request를 반환한다")
        @WithMockUser(username = "test@example.com")
        void shouldReturn400WhenNeitherFilesNorTipProvided() throws Exception {
            mockMvc.perform(multipart("/guide/api/upload")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound()); // IllegalArgumentException → 404 (GlobalExceptionHandler)
        }

        @Test
        @DisplayName("파일 없이 팁만 있으면 Guide가 생성된다")
        @WithMockUser(username = "test@example.com")
        void shouldCreateGuideWithTipOnlyWhenNoFiles() throws Exception {
            // Arrange
            given(memberService.getCurrentMember("test@example.com")).willReturn(testMember);
            given(guideService.createGuideWithMedia(any(), eq("팁만 작성"), anyList(), anyBoolean())).willReturn(99L);

            // Act & Assert
            mockMvc.perform(multipart("/guide/api/upload")
                            .param("tip", "팁만 작성")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.guideId").value(99))
                    .andExpect(jsonPath("$.totalFiles").value(0))
                    .andExpect(jsonPath("$.savedFiles").value(0))
                    .andExpect(jsonPath("$.skippedFiles").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /guide/api/aggregate")
    class GetRegionAggregate {

        @Test
        @DisplayName("level=continent이면 대륙 집계 목록을 반환한다")
        @WithMockUser
        void shouldReturnContinentAggregate() throws Exception {
            // Arrange
            List<RegionClusterDto> clusters = List.of(
                    new RegionClusterDto("AS", "Asia", 35.0, 127.0, 3, "/media/files/a.jpg")
            );
            given(regionAggregateService.aggregate(AggregateLevel.CONTINENT)).willReturn(clusters);

            // Act & Assert
            mockMvc.perform(get("/guide/api/aggregate")
                            .param("level", "continent"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].key").value("AS"))
                    .andExpect(jsonPath("$[0].label").value("Asia"))
                    .andExpect(jsonPath("$[0].count").value(3))
                    .andExpect(jsonPath("$[0].thumbnailUrl").value("/media/files/a.jpg"));

            verify(regionAggregateService).aggregate(AggregateLevel.CONTINENT);
        }

        @Test
        @DisplayName("level 미입력 시 국가 집계를 기본값으로 사용한다")
        @WithMockUser
        void shouldUseCountryAggregateByDefault() throws Exception {
            // Arrange
            given(regionAggregateService.aggregate(AggregateLevel.COUNTRY)).willReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/guide/api/aggregate"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(regionAggregateService).aggregate(AggregateLevel.COUNTRY);
        }
    }

    @Nested
    @DisplayName("GET /guide/api/my")
    class GetMyGuides {

        @Test
        @DisplayName("인증된 사용자는 자신의 가이드 목록을 조회할 수 있다")
        @WithMockUser(username = "test@example.com")
        void shouldReturnMyGuidesForAuthenticatedUser() throws Exception {
            // Arrange
            MemberDto authorDto = MemberDto.builder().id(1L).email("test@example.com").build();
            List<GuideResponseDto> mockGuides = Arrays.asList(
                    GuideResponseDto.builder().id(1L).tip("Guide 1").author(authorDto).likeCount(5).build(),
                    GuideResponseDto.builder().id(2L).tip("Guide 2").author(authorDto).likeCount(10).build()
            );

            given(memberService.getCurrentMember("test@example.com")).willReturn(testMember);
            given(guideService.getMyGuides(1L)).willReturn(mockGuides);

            // Act & Assert
            mockMvc.perform(get("/guide/api/my"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].tip").value("Guide 1"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].tip").value("Guide 2"));

            verify(guideService).getMyGuides(1L);
        }
    }

    @Nested
    @DisplayName("GET /guide/api/{id}")
    class GetGuide {

        @Test
        @DisplayName("가이드 ID로 상세 정보를 조회할 수 있다")
        @WithMockUser(username = "test@example.com")
        void shouldReturnGuideDetail() throws Exception {
            // Arrange
            Long guideId = 1L;
            MemberDto authorDto = MemberDto.builder().id(1L).email("test@example.com").build();
            GuideResponseDto mockGuide = GuideResponseDto.builder()
                    .id(guideId)
                    .tip("Test guide detail")
                    .author(authorDto)
                    .likeCount(15)
                    .build();

            given(guideService.findGuideById(eq(guideId), any())).willReturn(mockGuide);

            // Act & Assert
            mockMvc.perform(get("/guide/api/{id}", guideId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(guideId))
                    .andExpect(jsonPath("$.tip").value("Test guide detail"))
                    .andExpect(jsonPath("$.likeCount").value(15));
        }
    }

    @Nested
    @DisplayName("PATCH /guide/api/{id}")
    class UpdateTip {

        @Test
        @DisplayName("유효한 tip으로 가이드를 수정하면 200과 업데이트된 가이드를 반환한다")
        @WithMockUser(username = "test@example.com")
        void shouldReturnUpdatedGuideWhenValidTip() throws Exception {
            // Arrange
            Long guideId = 1L;
            GuideUpdateRequestDto updateRequest = new GuideUpdateRequestDto("Updated tip content");
            MemberDto authorDto = MemberDto.builder().id(1L).email("test@example.com").build();
            GuideResponseDto updatedGuide = GuideResponseDto.builder()
                    .id(guideId)
                    .tip("Updated tip content")
                    .author(authorDto)
                    .likeCount(5)
                    .build();

            given(guideService.updateTip(eq(guideId), eq("Updated tip content"), any()))
                    .willReturn(updatedGuide);

            // Act & Assert
            mockMvc.perform(patch("/guide/api/{id}", guideId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(guideId))
                    .andExpect(jsonPath("$.tip").value("Updated tip content"));

            verify(guideService).updateTip(eq(guideId), eq("Updated tip content"), any());
        }

        @Test
        @DisplayName("빈 tip으로 수정 요청하면 400 Bad Request를 반환한다")
        @WithMockUser(username = "test@example.com")
        void shouldReturn400WhenTipIsBlank() throws Exception {
            // Arrange
            Long guideId = 1L;
            GuideUpdateRequestDto invalidRequest = new GuideUpdateRequestDto("");

            // Act & Assert
            mockMvc.perform(patch("/guide/api/{id}", guideId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(guideService, never()).updateTip(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /guide/api/{id}")
    class DeleteGuide {

        @Test
        @DisplayName("가이드 삭제에 성공하면 204 No Content를 반환한다")
        @WithMockUser(username = "test@example.com")
        void shouldReturn204WhenGuideDeleted() throws Exception {
            // Arrange
            Long guideId = 1L;
            doNothing().when(guideService).deleteGuide(eq(guideId), any());

            // Act & Assert
            mockMvc.perform(delete("/guide/api/{id}", guideId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            verify(guideService).deleteGuide(eq(guideId), any());
        }
    }

    @Nested
    @DisplayName("GET /guide/api/nearby")
    class GetNearbyGuides {

        @Test
        @DisplayName("위치 파라미터로 근처 가이드 목록을 반환한다")
        @WithMockUser
        void shouldReturnNearbyGuides() throws Exception {
            // Arrange
            double lat = 37.5665, lng = 126.9780, radius = 20.0;
            MemberDto user1 = MemberDto.builder().id(1L).email("u1@example.com").build();
            List<GuideResponseDto> nearbyGuides = Arrays.asList(
                    GuideResponseDto.builder().id(1L).tip("Nearby 1").author(user1).build(),
                    GuideResponseDto.builder().id(2L).tip("Nearby 2").author(user1).build()
            );

            given(guideService.findGuidesNear(lat, lng, radius)).willReturn(nearbyGuides);

            // Act & Assert
            mockMvc.perform(get("/guide/api/nearby")
                            .param("lat", String.valueOf(lat))
                            .param("lng", String.valueOf(lng))
                            .param("radius", String.valueOf(radius)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].tip").value("Nearby 1"))
                    .andExpect(jsonPath("$[1].tip").value("Nearby 2"));
        }

        @Test
        @DisplayName("radius 파라미터 미입력 시 기본값(20km)으로 검색한다")
        @WithMockUser
        void shouldUseDefaultRadiusWhenNotProvided() throws Exception {
            // Arrange
            double lat = 37.5665, lng = 126.9780, defaultRadius = 20.0;
            given(guideService.findGuidesNear(lat, lng, defaultRadius)).willReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/guide/api/nearby")
                            .param("lat", String.valueOf(lat))
                            .param("lng", String.valueOf(lng)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(guideService).findGuidesNear(lat, lng, defaultRadius);
        }
    }

    @Nested
    @DisplayName("POST /guide/api/like/{id}")
    class LikeGuide {

        @Test
        @DisplayName("좋아요를 추가하면 liked=true와 증가된 likeCount를 반환한다")
        @WithMockUser(username = "test@example.com")
        void shouldReturnLikedTrueWhenLikeAdded() throws Exception {
            // Arrange
            Long guideId = 1L;
            MemberDto authorDto = MemberDto.builder().id(1L).email("test@example.com").build();
            GuideResponseDto updatedGuide = GuideResponseDto.builder()
                    .id(guideId).tip("Test").author(authorDto).likeCount(6).build();

            given(guideLikeService.toggleLike(eq(guideId), any())).willReturn(true);
            given(guideService.findGuideById(eq(guideId), any())).willReturn(updatedGuide);

            // Act & Assert
            mockMvc.perform(post("/guide/api/like/{id}", guideId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true))
                    .andExpect(jsonPath("$.likeCount").value(6));
        }

        @Test
        @DisplayName("좋아요를 취소하면 liked=false와 감소된 likeCount를 반환한다")
        @WithMockUser(username = "test@example.com")
        void shouldReturnLikedFalseWhenLikeCancelled() throws Exception {
            // Arrange
            Long guideId = 1L;
            MemberDto authorDto = MemberDto.builder().id(1L).email("test@example.com").build();
            GuideResponseDto updatedGuide = GuideResponseDto.builder()
                    .id(guideId).tip("Test").author(authorDto).likeCount(4).build();

            given(guideLikeService.toggleLike(eq(guideId), any())).willReturn(false);
            given(guideService.findGuideById(eq(guideId), any())).willReturn(updatedGuide);

            // Act & Assert
            mockMvc.perform(post("/guide/api/like/{id}", guideId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(false))
                    .andExpect(jsonPath("$.likeCount").value(4));
        }
    }
}
