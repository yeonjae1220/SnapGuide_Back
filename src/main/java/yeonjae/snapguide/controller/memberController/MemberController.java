package yeonjae.snapguide.controller.memberController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yeonjae.snapguide.domain.member.dto.LocalSignUpRequestDto;
import yeonjae.snapguide.service.memberSerivce.MemberService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/members")
@Slf4j
@Tag(name = "Member", description = "Member API")
public class MemberController {
    private final MemberService memberService;

}
