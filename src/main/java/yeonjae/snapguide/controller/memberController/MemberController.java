package yeonjae.snapguide.controller.memberController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
        // 토큰 검증은 Spring Security 필터에서 진행됨
        return ResponseEntity.ok(memberService.getAllMembers());
    }

}
