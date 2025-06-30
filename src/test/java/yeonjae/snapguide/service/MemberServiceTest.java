//package yeonjae.snapguide.service;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.transaction.annotation.Transactional;
//import yeonjae.snapguide.domain.member.Provider;
//import yeonjae.snapguide.domain.member.Member;
//import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
//import yeonjae.snapguide.domain.member.dto.MemberResponseDto;
//import yeonjae.snapguide.repository.memberRepository.MemberRepository;
//import yeonjae.snapguide.security.authentication.jwt.JwtToken;
//import yeonjae.snapguide.service.memberSerivce.MemberService;
//
//import static org.junit.jupiter.api.Assertions.*;
////@ExtendWith(SpringExtension.class)
//@SpringBootTest
//@Transactional
//class MemberServiceTest {
//    @Autowired
//    private MemberService memberService;
//    @Autowired
//    private AuthService authService;
//    @Autowired
//    private MemberRepository memberRepository;
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    @Test
//    void localSignUPTest() {
//        //given
//        MemberRequestDto requestDto = new MemberRequestDto();
//        ReflectionTestUtils.setField(requestDto, "email", "test@example.com"); // private 필드에 값을 직접 주입할 수 있도록 도와주는 유틸리티
//        ReflectionTestUtils.setField(requestDto, "password", "dummyPassword");
//        ReflectionTestUtils.setField(requestDto, "nickname", "dummyNickname");
//
//        // when
//        MemberResponseDto memberResponseDto = authService.signup(requestDto);
//        Long memberId = memberResponseDto.getId();
//
//        // then
//        Member savedMember = memberRepository.findById(memberId).orElseThrow();
//        assertEquals("test@example.com", savedMember.getEmail());
//        assertTrue(passwordEncoder.matches("dummyPassword", savedMember.getPassword()));
//        assertEquals("dummyNickname", savedMember.getNickname());
//        assertEquals(Provider.LOCAL, savedMember.getProvider());
//    }
//
//    @Test
//    void duplicatedEmailSignUPException() {
//        // given
//        MemberRequestDto request1 = new MemberRequestDto();
//        ReflectionTestUtils.setField(request1, "email", "test@example.com");
//        ReflectionTestUtils.setField(request1, "password", "dummyPassword1");
//        ReflectionTestUtils.setField(request1, "nickname", "user1");
//
//        MemberRequestDto request2 = new MemberRequestDto();
//        ReflectionTestUtils.setField(request2, "email", "test@example.com"); // same email
//        ReflectionTestUtils.setField(request2, "password", "dummyPassword2");
//        ReflectionTestUtils.setField(request2, "nickname", "user2");
//
//        // when
//        authService.signup(request1);
//
//        // then
//        assertThrows(IllegalArgumentException.class, () -> authService.signup(request2));
//    }
//
//    @Test
//    void signInTest() {
//        // given
//        String email = "login@example.com";
//        String password = "1234abcd";
//        String encodedPassword = passwordEncoder.encode(password);
//
//        Member member = Member.builder()
//                .email(email)
//                .password(encodedPassword)
//                .nickname("dummy")
//                .provider(Provider.LOCAL)
//                .build();
//        memberRepository.save(member);
//
//        MemberRequestDto loginRequest = new MemberRequestDto();
//        ReflectionTestUtils.setField(loginRequest, "email", email);
//        ReflectionTestUtils.setField(loginRequest, "password", password);
//
//        // when
//        JwtToken jwtToken = authService.login(loginRequest);
//
//        // then
//        assertNotNull(jwtToken);
//        assertNotNull(jwtToken.getAccessToken());
//        assertFalse(jwtToken.getAccessToken().isEmpty());
//
////        assertNotNull(loginMember);
////        assertEquals(email, loginMember.getEmail());
////        assertEquals(Provider.LOCAL, loginMember.getLoginType());
//    }
//
//    @Test
//    void SignInFailedWithNoEmail() {
//        // given
//        MemberRequestDto loginRequest = new MemberRequestDto();
//        ReflectionTestUtils.setField(loginRequest, "email", "notfound@example.com");
//        ReflectionTestUtils.setField(loginRequest, "password", "anyPassword");
//
//        // then
//        assertThrows(IllegalArgumentException.class, () -> {
//            authService.login(loginRequest);
//        });
//    }
//
//    @Test
//    void signInFailedWithIncorrectPassward() {
//        // given
//        String email = "fail@example.com";
//        String realPassword = "correctpass";
//        String wrongPassword = "wrongpass";
//
//        Member member = Member.builder()
//                .email(email)
//                .password(passwordEncoder.encode(realPassword))
//                .nickname("wrongpassuser")
//                .provider(Provider.LOCAL)
//                .build();
//        memberRepository.save(member);
//
//        MemberRequestDto loginRequest = new MemberRequestDto();
//        ReflectionTestUtils.setField(loginRequest, "email", email);
//        ReflectionTestUtils.setField(loginRequest, "password", wrongPassword);
//
//        // then
//        assertThrows(IllegalArgumentException.class, () -> {
//            authService.login(loginRequest);
//        });
//    }
//
//}