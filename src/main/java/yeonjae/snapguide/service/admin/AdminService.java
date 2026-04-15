package yeonjae.snapguide.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.controller.admin.dto.AdminMemberResponse;
import yeonjae.snapguide.controller.admin.dto.AdminRoleUpdateRequest;
import yeonjae.snapguide.controller.admin.dto.AdminStatsResponse;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {

    private final MemberRepository memberRepository;
    private final GuideRepository guideRepository;

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
}
