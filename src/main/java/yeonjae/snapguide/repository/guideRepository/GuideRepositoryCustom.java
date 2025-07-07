package yeonjae.snapguide.repository.guideRepository;

import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;

import java.util.List;

public interface GuideRepositoryCustom {
    public List<GuideResponseDto> findAllByMemberId(Long memberId);
}
