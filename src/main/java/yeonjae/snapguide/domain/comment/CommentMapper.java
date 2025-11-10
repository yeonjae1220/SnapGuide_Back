package yeonjae.snapguide.domain.comment;

import yeonjae.snapguide.domain.member.MemberMapper;

/**
 * Comment Entity ↔ CommentDto 변환을 담당하는 Mapper
 * LocationMapper 패턴을 따라 일관성 있는 변환 로직 제공
 */
public class CommentMapper {

    /**
     * Comment Entity → CommentDto 변환
     * Lazy Loading 방지를 위해 트랜잭션 내에서 호출 필요
     *
     * @param entity Comment Entity
     * @return CommentDto
     */
    public static CommentDto toDto(Comment entity) {
        if (entity == null) {
            return null;
        }

        return CommentDto.builder()
                .id(entity.getId())
                .comment(entity.getComment())
                .author(MemberMapper.toDto(entity.getAuthor()))
                .guideId(entity.getGuide() != null ? entity.getGuide().getId() : null)
                .build();
    }
}
