package yeonjae.snapguide.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.controller.admin.dto.*;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;
import yeonjae.snapguide.repository.CommentRepository;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {

    private final MemberRepository memberRepository;
    private final GuideRepository guideRepository;
    private final LocationRepository locationRepository;
    private final CommentRepository commentRepository;

    public Page<AdminMemberResponse> getMembers(Pageable pageable) {
        return memberRepository.findAll(pageable)
                .map(AdminMemberResponse::of);
    }

    @Transactional
    public AdminMemberResponse updateMemberRole(Long memberId, AdminRoleUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        member.updateAuthority(request.getAuthority());
        return AdminMemberResponse.of(member);
    }

    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .totalMembers(memberRepository.count())
                .totalGuides(guideRepository.count())
                .build();
    }

    public Page<AdminGuideResponse> getGuides(Pageable pageable) {
        return guideRepository.findAll(pageable)
                .map(AdminGuideResponse::of);
    }

    @Transactional
    public void deleteGuide(Long id) {
        if (!guideRepository.existsById(id)) {
            throw new CustomException(ErrorCode.GUIDE_NOT_FOUND);
        }
        guideRepository.deleteById(id);
    }

    public Page<AdminLocationResponse> getLocations(Pageable pageable) {
        return locationRepository.findAll(pageable)
                .map(AdminLocationResponse::of);
    }

    @Transactional
    public void deleteLocation(Long id) {
        if (!locationRepository.existsById(id)) {
            throw new CustomException(ErrorCode.LOCATION_NOT_FOUND);
        }
        locationRepository.deleteById(id);
    }

    public Page<AdminCommentResponse> getComments(Pageable pageable) {
        return commentRepository.findAll(pageable)
                .map(AdminCommentResponse::of);
    }

    @Transactional
    public void deleteComment(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        commentRepository.deleteById(id);
    }
}
