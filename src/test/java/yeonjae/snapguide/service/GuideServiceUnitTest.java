package yeonjae.snapguide.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.repository.guideLikeRepository.GuideLikeRepository;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.service.guideSerivce.GuideLikeService;
import yeonjae.snapguide.service.guideSerivce.GuideService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("GuideService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class GuideServiceUnitTest {

    @Mock private GuideRepository guideRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private MediaRepository mediaRepository;
    @Mock private GuideLikeRepository guideLikeRepository;
    @Mock private GuideLikeService guideLikeService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GuideService guideService;

    private Member testMember;
    private Guide testGuide;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .email("author@example.com")
                .nickname("testAuthor")
                .build();

        testGuide = Guide.builder()
                .id(100L)
                .tip("Original tip")
                .author(testMember)
                .build();

        userDetails = mock(UserDetails.class);
    }

    @Nested
    @DisplayName("updateTip 메서드")
    class UpdateTip {

        @Test
        @DisplayName("본인 가이드의 팁을 수정하면 업데이트된 DTO를 반환한다")
        void shouldUpdateTipAndReturnDtoWhenAuthorMatches() {
            // Arrange
            given(userDetails.getUsername()).willReturn("author@example.com");
            given(guideRepository.findByIdWithFetch(100L)).willReturn(Optional.of(testGuide));
            given(memberRepository.findByEmail("author@example.com")).willReturn(Optional.of(testMember));

            // Act
            GuideResponseDto result = guideService.updateTip(100L, "Updated tip", userDetails);

            // Assert
            assertThat(result.getTip()).isEqualTo("Updated tip");
        }

        @Test
        @DisplayName("다른 사용자의 가이드 팁 수정 시도는 AccessDeniedException을 던진다")
        void shouldThrowAccessDeniedWhenNotAuthor() {
            // Arrange
            Member anotherMember = Member.builder().id(2L).email("other@example.com").build();
            given(userDetails.getUsername()).willReturn("other@example.com");
            given(guideRepository.findByIdWithFetch(100L)).willReturn(Optional.of(testGuide));
            given(memberRepository.findByEmail("other@example.com")).willReturn(Optional.of(anotherMember));

            // Act & Assert
            assertThatThrownBy(() -> guideService.updateTip(100L, "Malicious tip", userDetails))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("본인의 가이드만 수정할 수 있습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 가이드 수정 시도는 IllegalArgumentException을 던진다")
        void shouldThrowExceptionWhenGuideNotFound() {
            // Arrange
            given(guideRepository.findByIdWithFetch(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> guideService.updateTip(999L, "tip", userDetails))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Guide not found");
        }

        @Test
        @DisplayName("존재하지 않는 사용자가 수정 시도하면 UsernameNotFoundException을 던진다")
        void shouldThrowUsernameNotFoundWhenMemberNotFound() {
            // Arrange
            given(userDetails.getUsername()).willReturn("ghost@example.com");
            given(guideRepository.findByIdWithFetch(100L)).willReturn(Optional.of(testGuide));
            given(memberRepository.findByEmail("ghost@example.com")).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> guideService.updateTip(100L, "tip", userDetails))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteGuide 메서드")
    class DeleteGuide {

        @Test
        @DisplayName("본인 가이드 삭제 시 guideRepository.delete가 호출된다")
        void shouldDeleteGuideWhenAuthorMatches() {
            // Arrange
            given(userDetails.getUsername()).willReturn("author@example.com");
            given(guideRepository.findByIdWithFetch(100L)).willReturn(Optional.of(testGuide));
            given(memberRepository.findByEmail("author@example.com")).willReturn(Optional.of(testMember));

            // Act
            guideService.deleteGuide(100L, userDetails);

            // Assert
            verify(guideRepository).delete(testGuide);
        }

        @Test
        @DisplayName("다른 사용자의 가이드 삭제 시도는 AccessDeniedException을 던진다")
        void shouldThrowAccessDeniedWhenNotAuthor() {
            // Arrange
            Member anotherMember = Member.builder().id(2L).email("other@example.com").build();
            given(userDetails.getUsername()).willReturn("other@example.com");
            given(guideRepository.findByIdWithFetch(100L)).willReturn(Optional.of(testGuide));
            given(memberRepository.findByEmail("other@example.com")).willReturn(Optional.of(anotherMember));

            // Act & Assert
            assertThatThrownBy(() -> guideService.deleteGuide(100L, userDetails))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("본인의 가이드만 삭제할 수 있습니다.");

            verify(guideRepository, never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 가이드 삭제 시도는 IllegalArgumentException을 던진다")
        void shouldThrowExceptionWhenGuideNotFound() {
            // Arrange
            given(guideRepository.findByIdWithFetch(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> guideService.deleteGuide(999L, userDetails))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Guide not found");
        }
    }

    @Nested
    @DisplayName("getMyGuides 메서드")
    class GetMyGuides {

        @Test
        @DisplayName("회원 ID로 해당 회원의 가이드 목록을 반환한다")
        void shouldReturnGuidesForMemberId() {
            // Arrange
            List<GuideResponseDto> expected = List.of(
                    GuideResponseDto.builder().id(1L).tip("tip1").build(),
                    GuideResponseDto.builder().id(2L).tip("tip2").build()
            );
            given(guideRepository.findAllByMemberId(1L)).willReturn(expected);

            // Act
            List<GuideResponseDto> result = guideService.getMyGuides(1L);

            // Assert
            assertThat(result).hasSize(2)
                    .extracting(GuideResponseDto::getTip)
                    .containsExactly("tip1", "tip2");
        }

        @Test
        @DisplayName("가이드가 없는 회원은 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenNoGuides() {
            // Arrange
            given(guideRepository.findAllByMemberId(99L)).willReturn(List.of());

            // Act
            List<GuideResponseDto> result = guideService.getMyGuides(99L);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createGuideWithMedia 메서드")
    class CreateGuideWithMedia {

        @Test
        @DisplayName("미디어 없이 팁만으로 가이드를 생성하면 guideRepository.save가 호출된다")
        void shouldSaveGuideAndCallRepository() {
            // Act
            guideService.createGuideWithMedia(testMember, "New tip", List.of(), true);

            // Assert
            verify(guideRepository).save(argThat(g ->
                    "New tip".equals(g.getTip()) && testMember.equals(g.getAuthor())));
        }
    }
}
