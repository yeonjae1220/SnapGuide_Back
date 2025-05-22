package yeonjae.snapguide.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.controller.guideController.guideDto.GuideCreateTestDto;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.repository.guideRepository.GuideRepository;
import yeonjae.snapguide.repository.locationRepository.LocationRepository;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.service.guideSerivce.GuideService;
import yeonjae.snapguide.service.mediaSerivce.MediaService;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GuideServiceTest {
    @Autowired
    GuideService guideService;
    @Autowired
    GuideRepository guideRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    LocationRepository locationRepository;
    @Autowired
    MediaRepository mediaRepository;
    @Autowired
    MediaService mediaService;

    @Test
    void testSaveGuide() {
        /*
        memberEntity를 id랑 username 두개 만 두고 함
         */
        // given
        Member member = Member.builder()
                .email("test@example.com")
                .build();
        memberRepository.save(member);

        Location location = Location.builder()
                .locationName("Gwanggyo Lake Park")
                .latitude(37.2752)
                .longitude(127.0469)
                .country("South Korea")
                .region("Gyeonggi-do")
                .subRegion("Suwon-si")
                .locality("Yeongtong-gu")
                .route("Gwanggyo-ro")
                .streetNumber("123")
                .premise("Lakeside Plaza")
                .subPremise("Cafe Blossom")
                .build();
        locationRepository.save(location);

        // when
        Long guideId = guideService.createGuide(GuideCreateTestDto.of(member.getId(), "dummy tip",location.getId()));

        // then
        assertNotNull(guideId);
        Guide guide = guideRepository.findById(guideId).get();
        assertEquals("dummy tip", guide.getTip());
        assertEquals(member.getId(), guide.getAuthor().getId());
        assertEquals(location.getId(), guide.getLocation().getId());
    }

    @Test
    void testLinkMediaTOGuide() throws IOException {
        // given
        Member member = Member.builder()
                .email("test@example.com")
                .build();
        Long memberId = memberRepository.save(member).getId();

        Guide guide = Guide.builder()
                .tip("dummy tip")
                .author(member)
                .build();
        Long guideId = guideRepository.save(guide).getId();



        List<MultipartFile> files = List.of(
                new MockMultipartFile("file", "test-image1.jpg", "image/jpeg", new FileInputStream("src/test/resources/testImage/test-image1.jpg")),
                new MockMultipartFile("file", "test-image2.jpg", "image/jpeg", new FileInputStream("src/test/resources/testImage/test-image2.jpg")),
                new MockMultipartFile("file", "test-image3.jpg", "image/jpeg", new FileInputStream("src/test/resources/testImage/test-image3.jpg")),
                new MockMultipartFile("file", "test-image4.HEIC", "image/HEIF", new FileInputStream("src/test/resources/testImage/test-image4.HEIC"))
        );

        List<Long> mediaIds = new ArrayList<>();
        for (MultipartFile file : files) {
            Long mediaId = mediaService.saveMedia(file);
            mediaIds.add(mediaId);
        }

        // when
        guideService.linkMediaToGuide(guideId, mediaIds);

        // then
        Media found1 = mediaRepository.findById(mediaIds.get(0)).orElseThrow();
        Media found2 = mediaRepository.findById(mediaIds.get(1)).orElseThrow();

        assertEquals(guide.getId(), found1.getGuide().getId());
        assertEquals(guide.getId(), found2.getGuide().getId());
    }


}