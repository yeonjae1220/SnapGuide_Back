package yeonjae.snapguide.controller.admin.dto;

import lombok.Builder;
import lombok.Getter;
import yeonjae.snapguide.domain.comment.Comment;

@Getter
@Builder
public class AdminCommentResponse {
    private Long id;
    private String comment;
    private String authorNickname;
    private Long guideId;

    public static AdminCommentResponse of(Comment comment) {
        return AdminCommentResponse.builder()
                .id(comment.getId())
                .comment(comment.getComment())
                .authorNickname(comment.getAuthor() != null ? comment.getAuthor().getNickname() : null)
                .guideId(comment.getGuide() != null ? comment.getGuide().getId() : null)
                .build();
    }
}
