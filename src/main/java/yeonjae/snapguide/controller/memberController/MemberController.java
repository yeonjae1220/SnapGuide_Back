package yeonjae.snapguide.controller.memberController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import yeonjae.snapguide.domain.member.dto.MemberDto;
import yeonjae.snapguide.service.memberSerivce.MemberService;

import java.util.List;

@RestController
//@RequiredArgsConstructor
@RequestMapping("api/members")
@Slf4j
@Tag(name = "Member", description = "Member API")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<List<MemberDto>> getMembers(@RequestHeader("Authorization") String authHeader) {
        log.info("in the controller");
        List<MemberDto> members = memberService.getAllMembers();
        for (MemberDto member : members) {
            log.info("멤버: id={}, email={}", member.getId(), member.getEmail());
        }
        // 토큰 검증은 Spring Security 필터에서 진행됨
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @DeleteMapping("delete/me")
    public ResponseEntity<Void> deleteMember(@AuthenticationPrincipal UserDetails userDetails) {
        // UserDetails.getUsername()은 실제로 email을 반환합니다 (CustomOauth2UserDetails 참고)
        String email = userDetails.getUsername();
        memberService.deleteMember(email);
        return ResponseEntity.noContent().build();
    }

}
