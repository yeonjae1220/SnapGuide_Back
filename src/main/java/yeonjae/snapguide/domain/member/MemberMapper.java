package yeonjae.snapguide.domain.member;

import yeonjae.snapguide.domain.member.dto.MemberDto;
import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
import yeonjae.snapguide.domain.member.dto.MemberResponseDto;

/**
 * Member Entity ↔ DTO 변환을 담당하는 Mapper
 * LocationMapper 패턴을 따라 일관성 있는 변환 로직 제공
 */
public class MemberMapper {

    /**
     * Member Entity → MemberDto 변환
     * @param entity Member Entity
     * @return MemberDto
     */
    public static MemberDto toDto(Member entity) {
        if (entity == null) {
            return null;
        }

        return MemberDto.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .build();
    }

    /**
     * Member Entity → MemberResponseDto 변환
     * @param entity Member Entity
     * @return MemberResponseDto
     */
    public static MemberResponseDto toResponseDto(Member entity) {
        if (entity == null) {
            return null;
        }

        return MemberResponseDto.builder()
                .email(entity.getEmail())
                .build();
    }

    /**
     * MemberRequestDto → Member Entity 변환
     * 비밀번호 인코딩은 Service 레이어에서 처리
     * @param dto MemberRequestDto
     * @return Member Entity (권한 정보 포함)
     */
    public static Member toEntity(MemberRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Member.builder()
                .email(dto.getEmail())
                .password(dto.getPassword()) // 주의: 인코딩은 Service에서 처리
                .nickname(dto.getNickname())
                .authority(dto.getAuthority())
                .build();
    }
}
