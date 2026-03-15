package yeonjae.snapguide.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.like.GuideLike;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.repository.guideLikeRepository.GuideLikeRepository;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.service.guideSerivce.GuideLikeService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@DisplayName("GuideLikeService")
@ExtendWith(MockitoExtension.class)
class GuideLikeServiceTest {

    @Mock
    private GuideLikeRepository guideLikeRepository;

    @Mock
    private GuideRepository guideRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private GuideLikeService guideLikeService;

    private Member testMember;
    private Guide testGuide;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        testGuide = Guide.builder()
                .id(10L)
                .tip("Test guide tip")
                .author(testMember)
                .build();

        userDetails = mock(UserDetails.class);
        lenient().when(userDetails.getUsername()).thenReturn("user@example.com");
    }

    @Nested
    @DisplayName("toggleLike 메서드")
    class ToggleLike {

        @Test
        @DisplayName("좋아요가 없을 때 toggleLike를 호출하면 좋아요를 추가하고 true를 반환한다")
        void shouldAddLikeAndReturnTrueWhenLikeDoesNotExist() {
            // Arrange
            given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(testMember));
            given(guideLikeRepository.existsByMemberIdAndGuideId(1L, 10L)).willReturn(false);
            given(guideRepository.getReferenceById(10L)).willReturn(testGuide);

            // Act
            boolean result = guideLikeService.toggleLike(10L, userDetails);

            // Assert
            assertThat(result).isTrue();
            verify(guideLikeRepository).save(any(GuideLike.class));
            verify(guideRepository).incrementLikeCount(10L);
        }

        @Test
        @DisplayName("좋아요가 있을 때 toggleLike를 호출하면 좋아요를 취소하고 false를 반환한다")
        void shouldRemoveLikeAndReturnFalseWhenLikeExists() {
            // Arrange
            given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(testMember));
            given(guideLikeRepository.existsByMemberIdAndGuideId(1L, 10L)).willReturn(true);

            // Act
            boolean result = guideLikeService.toggleLike(10L, userDetails);

            // Assert
            assertThat(result).isFalse();
            verify(guideLikeRepository).deleteByMemberIdAndGuideId(1L, 10L);
            verify(guideRepository).decrementLikeCount(10L);
        }

        @Test
        @DisplayName("userDetails가 null이면 IllegalArgumentException을 던진다")
        void shouldThrowExceptionWhenUserDetailsIsNull() {
            assertThatThrownBy(() -> guideLikeService.toggleLike(10L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("로그인이 필요한 서비스입니다.");
        }
    }

    @Nested
    @DisplayName("hasUserLiked 메서드")
    class HasUserLiked {

        @Test
        @DisplayName("사용자가 가이드에 좋아요를 눌렀으면 true를 반환한다")
        void shouldReturnTrueWhenUserHasLiked() {
            // Arrange
            given(guideLikeRepository.findByMemberAndGuide(testMember, testGuide))
                    .willReturn(Optional.of(new GuideLike(testMember, testGuide)));

            // Act
            boolean result = guideLikeService.hasUserLiked(testMember, testGuide);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("사용자가 가이드에 좋아요를 누르지 않았으면 false를 반환한다")
        void shouldReturnFalseWhenUserHasNotLiked() {
            // Arrange
            given(guideLikeRepository.findByMemberAndGuide(testMember, testGuide))
                    .willReturn(Optional.empty());

            // Act
            boolean result = guideLikeService.hasUserLiked(testMember, testGuide);

            // Assert
            assertThat(result).isFalse();
        }
    }
}
