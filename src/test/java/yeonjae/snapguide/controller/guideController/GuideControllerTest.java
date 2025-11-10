package yeonjae.snapguide.controller.guideController;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.controller.guideController.guideDto.GuideUpdateRequestDto;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.dto.MemberDto;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.service.guideSerivce.GuideService;
import yeonjae.snapguide.service.mediaSerivce.MediaService;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GuideController 단위 테스트
 * @WebMvcTest를 사용하여 Controller 레이어만 테스트
 */
@WebMvcTest(GuideController.class)
class GuideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GuideService guideService;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private MemberRepository memberRepository;

    private UserDetails userDetails;
    private Member testMember;

    @BeforeEach
    void setUp() {
        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .roles("USER")
                .build();

        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("testUser")
                .build();
    }

    @Test
    @DisplayName("GET /guide/api/my - 내 가이드 목록 조회")
    @WithMockUser(username = "test@example.com")
    void getMyGuides_Success() throws Exception {
        // given
        given(memberRepository.findByEmail("test@example.com"))
                .willReturn(Optional.of(testMember));

        MemberDto authorDto = MemberDto.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        List<GuideResponseDto> mockGuides = Arrays.asList(
                GuideResponseDto.builder()
                        .id(1L)
                        .tip("Test guide 1")
                        .author(authorDto)
                        .likeCount(5)
                        .build(),
                GuideResponseDto.builder()
                        .id(2L)
                        .tip("Test guide 2")
                        .author(authorDto)
                        .likeCount(10)
                        .build()
        );

        given(guideService.getMyGuides(1L)).willReturn(mockGuides);

        // when & then
        mockMvc.perform(get("/guide/api/my"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tip").value("Test guide 1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].tip").value("Test guide 2"));

        verify(guideService, times(1)).getMyGuides(1L);
    }

    @Test
    @DisplayName("GET /guide/api/{id} - 가이드 상세 조회")
    @WithMockUser(username = "test@example.com")
    void getGuide_Success() throws Exception {
        // given
        Long guideId = 1L;
        MemberDto authorDto = MemberDto.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        GuideResponseDto mockGuide = GuideResponseDto.builder()
                .id(guideId)
                .tip("Test guide detail")
                .author(authorDto)
                .likeCount(15)
                .build();

        given(guideService.findGuideById(eq(guideId), any(UserDetails.class)))
                .willReturn(mockGuide);

        // when & then
        mockMvc.perform(get("/guide/api/{id}", guideId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(guideId))
                .andExpect(jsonPath("$.tip").value("Test guide detail"))
                .andExpect(jsonPath("$.author.email").value("test@example.com"))
                .andExpect(jsonPath("$.likeCount").value(15));

        verify(guideService, times(1)).findGuideById(eq(guideId), any(UserDetails.class));
    }

    @Test
    @DisplayName("PUT /guide/api/update - 가이드 팁 수정")
    @WithMockUser(username = "test@example.com")
    void updateTip_Success() throws Exception {
        // given
        GuideUpdateRequestDto updateRequest = new GuideUpdateRequestDto(1L, "Updated tip content");

        MemberDto authorDto = MemberDto.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        GuideResponseDto updatedGuide = GuideResponseDto.builder()
                .id(1L)
                .tip("Updated tip content")
                .author(authorDto)
                .likeCount(5)
                .build();

        given(guideService.updateTip(eq(1L), eq("Updated tip content"), any(UserDetails.class)))
                .willReturn(updatedGuide);

        // when & then
        mockMvc.perform(put("/guide/api/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tip").value("Updated tip content"));

        verify(guideService, times(1)).updateTip(eq(1L), eq("Updated tip content"), any(UserDetails.class));
    }

    @Test
    @DisplayName("DELETE /guide/api/delete/{id} - 가이드 삭제")
    @WithMockUser(username = "test@example.com")
    void deleteGuide_Success() throws Exception {
        // given
        Long guideId = 1L;
        doNothing().when(guideService).deleteGuide(eq(guideId), any(UserDetails.class));

        // when & then
        mockMvc.perform(delete("/guide/api/delete/{id}", guideId))
                .andDo(print())
                .andExpect(status().isOk());

        verify(guideService, times(1)).deleteGuide(eq(guideId), any(UserDetails.class));
    }

    @Test
    @DisplayName("GET /guide/api/nearby - 근처 가이드 검색")
    void getNearbyGuides_Success() throws Exception {
        // given
        double lat = 37.5665;
        double lng = 126.9780;
        double radius = 20.0;

        MemberDto user1 = MemberDto.builder().id(1L).email("user1@example.com").build();
        MemberDto user2 = MemberDto.builder().id(2L).email("user2@example.com").build();

        List<GuideResponseDto> nearbyGuides = Arrays.asList(
                GuideResponseDto.builder()
                        .id(1L)
                        .tip("Nearby guide 1")
                        .author(user1)
                        .build(),
                GuideResponseDto.builder()
                        .id(2L)
                        .tip("Nearby guide 2")
                        .author(user2)
                        .build()
        );

        given(guideService.findGuidesNear(lat, lng, radius))
                .willReturn(nearbyGuides);

        // when & then
        mockMvc.perform(get("/guide/api/nearby")
                        .param("lat", String.valueOf(lat))
                        .param("lng", String.valueOf(lng))
                        .param("radius", String.valueOf(radius)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tip").value("Nearby guide 1"))
                .andExpect(jsonPath("$[1].tip").value("Nearby guide 2"));

        verify(guideService, times(1)).findGuidesNear(lat, lng, radius);
    }

    @Test
    @DisplayName("GET /guide/api/nearby - 기본 반경값으로 검색")
    void getNearbyGuides_WithDefaultRadius() throws Exception {
        // given
        double lat = 37.5665;
        double lng = 126.9780;
        double defaultRadius = 20.0;

        given(guideService.findGuidesNear(lat, lng, defaultRadius))
                .willReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/guide/api/nearby")
                        .param("lat", String.valueOf(lat))
                        .param("lng", String.valueOf(lng)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(guideService, times(1)).findGuidesNear(lat, lng, defaultRadius);
    }

    @Test
    @DisplayName("POST /guide/api/like/{id} - 가이드 좋아요 토글")
    @WithMockUser(username = "test@example.com")
    void likeGuide_Success() throws Exception {
        // given
        Long guideId = 1L;

        given(guideService.toggleLike(eq(guideId), any(UserDetails.class)))
                .willReturn(true);

        MemberDto authorDto = MemberDto.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        GuideResponseDto updatedGuide = GuideResponseDto.builder()
                .id(guideId)
                .tip("Test guide")
                .author(authorDto)
                .likeCount(6)
                .build();

        given(guideService.findGuideById(eq(guideId), any(UserDetails.class)))
                .willReturn(updatedGuide);

        // when & then
        mockMvc.perform(post("/guide/api/like/{id}", guideId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(6));

        verify(guideService, times(1)).toggleLike(eq(guideId), any(UserDetails.class));
        verify(guideService, times(1)).findGuideById(eq(guideId), any(UserDetails.class));
    }

    @Test
    @DisplayName("POST /guide/api/like/{id} - 좋아요 취소")
    @WithMockUser(username = "test@example.com")
    void unlikeGuide_Success() throws Exception {
        // given
        Long guideId = 1L;

        given(guideService.toggleLike(eq(guideId), any(UserDetails.class)))
                .willReturn(false);

        MemberDto authorDto = MemberDto.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        GuideResponseDto updatedGuide = GuideResponseDto.builder()
                .id(guideId)
                .tip("Test guide")
                .author(authorDto)
                .likeCount(4)
                .build();

        given(guideService.findGuideById(eq(guideId), any(UserDetails.class)))
                .willReturn(updatedGuide);

        // when & then
        mockMvc.perform(post("/guide/api/like/{id}", guideId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(4));
    }
}
