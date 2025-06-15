package yeonjae.snapguide.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import yeonjae.snapguide.domain.member.Authority;
import yeonjae.snapguide.domain.member.Member;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberResponseDto {
    private Long id;
    private String email;
    private String nickname;
    private List<Authority> authority;

    public static MemberResponseDto of(Member member) {
        return MemberResponseDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .authority(member.getAuthority())
                .build();
    }
}
