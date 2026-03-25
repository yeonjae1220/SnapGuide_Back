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
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Slf4j
@Tag(name = "Member", description = "Member API")
public class MemberController {
    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<List<MemberDto>> getMembers(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("in the controller");
        List<MemberDto> members = memberService.getAllMembers();
        members.forEach(m -> log.info("멤버: id={}, email={}", m.getId(), m.getEmail()));
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMember(@AuthenticationPrincipal UserDetails userDetails) {
        memberService.deleteMember(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
