package yeonjae.snapguide.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import yeonjae.snapguide.domain.member.Member;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberDto {
    private Long id;
    private String email;

    // Member 엔티티를 받아서 DTO를 생성하는 정적 팩토리 메서드
    public static MemberDto fromEntity(Member member) {
        return new MemberDto(member.getId(), member.getEmail());
    }
}
