package yeonjae.snapguide.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.member.LoginType;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.service.memberSerivce.MemberService;

import static org.junit.jupiter.api.Assertions.*;
//@ExtendWith(SpringExtension.class)
@SpringBootTest
@Transactional
class MemberServiceTest {
    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void localSignUPTest() {
        //given
        LocalSignUpRequestDto requestDto = new LocalSignUpRequestDto();
        ReflectionTestUtils.setField(requestDto, "email", "test@example.com"); // private 필드에 값을 직접 주입할 수 있도록 도와주는 유틸리티
        ReflectionTestUtils.setField(requestDto, "password", "dummyPassword");
        ReflectionTestUtils.setField(requestDto, "nickname", "dummyNickname");

        // when
        Long memberId = memberService.signUp(requestDto);

        // then
        Member savedMember = memberRepository.findById(memberId).orElseThrow();
        assertEquals("test@example.com", savedMember.getEmail());
        assertTrue(passwordEncoder.matches("dummyPassword", savedMember.getPassword()));
        assertEquals("dummyNickname", savedMember.getNickname());
        assertEquals(LoginType.LOCAL, savedMember.getLoginType());
    }

    @Test
    void duplicatedEmailSignUPException() {
        // given
        LocalSignUpRequestDto request1 = new LocalSignUpRequestDto();
        ReflectionTestUtils.setField(request1, "email", "test@example.com");
        ReflectionTestUtils.setField(request1, "password", "dummyPassword1");
        ReflectionTestUtils.setField(request1, "nickname", "user1");

        LocalSignUpRequestDto request2 = new LocalSignUpRequestDto();
        ReflectionTestUtils.setField(request2, "email", "test@example.com"); // same email
        ReflectionTestUtils.setField(request2, "password", "dummyPassword2");
        ReflectionTestUtils.setField(request2, "nickname", "user2");

        // when
        memberService.signUp(request1);

        // then
        assertThrows(IllegalArgumentException.class, () -> memberService.signUp(request2));
    }

    @Test
    void signInTest() {
        // given
        String email = "login@example.com";
        String password = "1234abcd";
        String encodedPassword = passwordEncoder.encode(password);

        Member member = Member.builder()
                .email(email)
                .password(encodedPassword)
                .nickname("dummy")
                .loginType(LoginType.LOCAL)
                .build();
        memberRepository.save(member);

        LocalSignInRequestDto loginRequest = new LocalSignInRequestDto();
        ReflectionTestUtils.setField(loginRequest, "email", email);
        ReflectionTestUtils.setField(loginRequest, "password", password);

        // when
        Member loginMember = memberService.signIn(loginRequest);

        // then
        assertNotNull(loginMember);
        assertEquals(email, loginMember.getEmail());
        assertEquals(LoginType.LOCAL, loginMember.getLoginType());
    }

    @Test
    void SignInFailedWithNoEmail() {
        // given
        LocalSignInRequestDto loginRequest = new LocalSignInRequestDto();
        ReflectionTestUtils.setField(loginRequest, "email", "notfound@example.com");
        ReflectionTestUtils.setField(loginRequest, "password", "anyPassword");

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.signIn(loginRequest);
        });
    }

    @Test
    void signInFailedWithIncorrectPassward() {
        // given
        String email = "fail@example.com";
        String realPassword = "correctpass";
        String wrongPassword = "wrongpass";

        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(realPassword))
                .nickname("wrongpassuser")
                .loginType(LoginType.LOCAL)
                .build();
        memberRepository.save(member);

        LocalSignInRequestDto loginRequest = new LocalSignInRequestDto();
        ReflectionTestUtils.setField(loginRequest, "email", email);
        ReflectionTestUtils.setField(loginRequest, "password", wrongPassword);

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.signIn(loginRequest);
        });
    }

}