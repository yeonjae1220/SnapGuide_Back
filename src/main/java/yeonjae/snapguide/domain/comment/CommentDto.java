package yeonjae.snapguide.domain.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import yeonjae.snapguide.domain.member.dto.MemberDto;

/**
 * Comment Entity를 표현하는 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private String comment;
    private MemberDto author;
    private Long guideId;
}
