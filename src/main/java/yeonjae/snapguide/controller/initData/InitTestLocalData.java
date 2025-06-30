//package yeonjae.snapguide.controller.initData;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//import yeonjae.snapguide.domain.member.Authority;
//import yeonjae.snapguide.domain.member.dto.MemberRequestDto;
//import yeonjae.snapguide.service.AuthService;
//import yeonjae.snapguide.service.fileStorageService.FileStorageService;
//import yeonjae.snapguide.service.locationSerivce.LocationService;
//import yeonjae.snapguide.service.mediaMetaDataSerivce.MediaMetaDataService;
//import yeonjae.snapguide.service.mediaSerivce.MediaService;
//import yeonjae.snapguide.service.memberSerivce.MemberService;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//
//@Profile("local")
//@Component
//@RequiredArgsConstructor
//public class InitTestLocalData {
//    private final InitTestDataService initTestDataService;
//
//    @PostConstruct
//    public void init() {
//        initTestDataService.init();
//    }
//
//    @Component
//    static class InitTestDataService {
//        @PersistenceContext
//        EntityManager em;
//
//        private MediaService mediaService;
//
//        private FileStorageService fileStorageService;
//
//        private MediaMetaDataService mediaMetaDataService;
//
//        private LocationService locationService;
//
//        private MemberService memberService;
//
//        private AuthService authService;
//
//
//        @Transactional
//        public void init() {
//            // testMember 입력
//            MemberRequestDto requestDto = new MemberRequestDto("test@example.com", "testPassword", "testNickname", new ArrayList<>(Arrays.asList(Authority.MEMBER)));
//            authService.signup(requestDto);
//            authService.login(requestDto);
//        }
//
//    }
//
//}
