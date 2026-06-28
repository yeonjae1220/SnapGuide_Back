package yeonjae.snapguide.controller.admin.dto;

import lombok.Builder;
import lombok.Getter;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.domain.member.Member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class AdminMemberResponse {
    private Long id;
    private String email;
    private String nickname;
    private String provider;
    private List<Authority> authority;
    private LocalDateTime createdAt;

    public static AdminMemberResponse of(Member member) {
        return AdminMemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .provider(member.getProvider() != null ? member.getProvider().name() : null)
                // authority는 @ElementCollection(LAZY). OSIV가 꺼져 있어(open-in-view:false)
                // lazy 프록시를 그대로 담으면 뷰 렌더링(세션 밖)에서 LazyInitializationException이
                // 발생한다. 트랜잭션 안에서 새 리스트로 복사해 즉시 초기화한다.
                .authority(member.getAuthority() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(member.getAuthority()))
                .createdAt(member.getCreatedAt())
                .build();
    }
}
