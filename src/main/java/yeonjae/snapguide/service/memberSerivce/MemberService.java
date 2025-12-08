package yeonjae.snapguide.service.memberSerivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.domain.member.dto.MemberDto;
import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
import yeonjae.snapguide.exception.CustomException;
import yeonjae.snapguide.exception.ErrorCode;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.security.authentication.jwt.TokenRequestDto;
import yeonjae.snapguide.service.AuthService;
import yeonjae.snapguide.service.fileStorageService.FileStorageService;
import yeonjae.snapguide.service.guideSerivce.GuideService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO : 	•	JWT 또는 세션 로그인 기능 추가
 * 	•	Spring Security + OAuth2 연동
 * 	•	이메일 인증, 비밀번호 변경, 닉네임 중복 체크 등 부가 기능 추가
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;
    private final GuideRepository guideRepository;
    private final MediaRepository mediaRepository;
    private final GuideService guideService;

    public List<MemberDto> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(member -> new MemberDto(member.getId(), member.getEmail()))
                .collect(Collectors.toList());
    }


    public void deleteMember(String email) {
        // 1. 사용자 조회 (JPA가 연관된 guides도 함께 불러옵니다)
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info("Starting deletion for member: {}", email);

        // 2. GuideService를 활용하여 각 Guide의 S3 파일 삭제
        // DB 삭제는 JPA cascade가 처리하므로, S3 파일 삭제에만 집중합니다.
        for (Guide guide : member.getGuides()) {
            guideService.deleteGuideMediaFiles(guide); // GuideService의 삭제 로직 재사용
        }

        // 3. Member만 삭제
        // cascade 설정에 따라 이 Member와 연관된 모든 Guide, Media, Location 등이 DB에서 자동으로 삭제됩니다.
        memberRepository.delete(member);
        log.info("Member deletion successful for: {}", email);
    }
}
