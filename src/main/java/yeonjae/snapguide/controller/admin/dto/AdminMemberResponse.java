package yeonjae.snapguide.controller.admin.dto;

import lombok.Builder;
import lombok.Getter;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.domain.member.Member;

import java.time.LocalDateTime;
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
                .authority(member.getAuthority())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
