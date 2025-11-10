package yeonjae.snapguide.controller.memberController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import yeonjae.snapguide.domain.member.dto.MemberDto;
import yeonjae.snapguide.service.memberSerivce.MemberService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MemberController 단위 테스트
 * 회원 목록 조회 기능 테스트
 */
@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    private List<MemberDto> mockMembers;

    @BeforeEach
    void setUp() {
        MemberDto member1 = MemberDto.builder()
                .id(1L)
                .email("user1@example.com")
                .build();

        MemberDto member2 = MemberDto.builder()
                .id(2L)
                .email("user2@example.com")
                .build();

        MemberDto member3 = MemberDto.builder()
                .id(3L)
                .email("user3@example.com")
                .build();

        mockMembers = Arrays.asList(member1, member2, member3);
    }

    @Test
    @DisplayName("GET /api/members - 모든 회원 조회 성공")
    @WithMockUser(username = "test@example.com")
    void getMembers_Success() throws Exception {
        // given
        given(memberService.getAllMembers()).willReturn(mockMembers);

        // when & then
        mockMvc.perform(get("/api/members")
                        .header("Authorization", "Bearer mock-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("user1@example.com"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].email").value("user2@example.com"))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].email").value("user3@example.com"));

        verify(memberService, times(1)).getAllMembers();
    }

    @Test
    @DisplayName("GET /api/members - 회원이 없을 때")
    @WithMockUser(username = "test@example.com")
    void getMembers_EmptyList() throws Exception {
        // given
        given(memberService.getAllMembers()).willReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/members")
                        .header("Authorization", "Bearer mock-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(memberService, times(1)).getAllMembers();
    }

    @Test
    @DisplayName("GET /api/members - 인증 헤더 확인")
    @WithMockUser(username = "test@example.com")
    void getMembers_WithAuthorizationHeader() throws Exception {
        // given
        given(memberService.getAllMembers()).willReturn(mockMembers);

        // when & then
        mockMvc.perform(get("/api/members")
                        .header("Authorization", "Bearer valid-jwt-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        verify(memberService, times(1)).getAllMembers();
    }
}
