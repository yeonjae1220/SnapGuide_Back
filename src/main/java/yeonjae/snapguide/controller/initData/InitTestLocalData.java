package yeonjae.snapguide.controller.initData;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.cameraModel.CameraModel;
import yeonjae.snapguide.domain.member.dto.LocalSignUpRequestDto;
import yeonjae.snapguide.service.FileStorageService;
import yeonjae.snapguide.service.locationSerivce.LocationService;
import yeonjae.snapguide.service.mediaMetaDataSerivce.MediaMetaDataService;
import yeonjae.snapguide.service.mediaSerivce.MediaService;
import yeonjae.snapguide.service.memberSerivce.MemberService;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitTestLocalData {
    private final InitTestDataService initTestDataService;

    @PostConstruct
    public void init() {
        initTestDataService.init();
    }

    @Component
    static class InitTestDataService {
        @PersistenceContext
        EntityManager em;
        @Autowired
        private MediaService mediaService;
        @Autowired
        private FileStorageService fileStorageService;
        @Autowired
        private MediaMetaDataService mediaMetaDataService;
        @Autowired
        private LocationService locationService;
        @Autowired
        private MemberService memberService;


        @Transactional
        public void init() {
            // testMember 입력
            LocalSignUpRequestDto requestDto = new LocalSignUpRequestDto("test@example.com", "testPassword", "testNickname");
            Long memberId = memberService.signUp(requestDto);

        }

    }

}
