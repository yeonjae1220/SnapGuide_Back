package yeonjae.snapguide.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.MemberMapper;

/**
 * Member Entity를 표현하는 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberDto {
    private Long id;
    private String email;

    /**
     * @deprecated MemberMapper.toDto()를 사용하세요
     * 하위 호환성을 위해 유지되지만 내부적으로 MemberMapper 위임
     */
    @Deprecated
    public static MemberDto fromEntity(Member member) {
        return MemberMapper.toDto(member);
    }
}
