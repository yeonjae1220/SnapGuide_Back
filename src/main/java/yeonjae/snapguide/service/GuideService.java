package yeonjae.snapguide.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.controller.GuideDto.GuideCreateTestDto;
import yeonjae.snapguide.entity.guide.Guide;
import yeonjae.snapguide.entity.guide.Location;
import yeonjae.snapguide.entity.guide.Media;
import yeonjae.snapguide.entity.member.Member;
import yeonjae.snapguide.repository.GuideRepository;
import yeonjae.snapguide.repository.LocationRepository;
import yeonjae.snapguide.repository.MediaRepository;
import yeonjae.snapguide.repository.MemberRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GuideService {
    private final GuideRepository guideRepository;
    private final MemberRepository memberRepository;
    private final LocationRepository locationRepository;
    private final MediaRepository mediaRepository;
    /*
    가이드 생성하고
    팁 넣고
    location ,member 연결해야함
    사진 정보가 없으니 Location을 뽑아서 넣는건 불가.
    1. 사용자가 guide생성시 선택해서 넣기 => 매개변수로 받기
    2. media저장할 때 location이 존재한다면 연관관계 넣어주기. => 알단 이걸로
    3. 그냥 연관관계 끊고 media통해서 획득하기
    빌더 or 생성자

    media들의 위치 정보가 다른것들이 있다면?

    TODO : 추후 위치 정보 여러개 저장할 수 있게
    list<Long>으로 Location 위치 쭉 뽑아와서 저장해두면 될듯..?

    media들은 얘 작성하고, 아이디들 받아와서 얘한테 연결 시켜 주는게 좋을듯?

     */
    public Long createGuide(GuideCreateTestDto guideCreateTestDto) {
        Member author = memberRepository.findById(guideCreateTestDto.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("member not found"));

        Location location = null;
        if (guideCreateTestDto.getLocationId() != null) {
            location = locationRepository.findById(guideCreateTestDto.getLocationId())
                    .orElseThrow(() -> new EntityNotFoundException("location not found"));
        }

        Guide guide = Guide.builder()
                .tip(guideCreateTestDto.getTip())
                .author(author)
                .location(location) // null도 가능
                .build();

        guideRepository.save(guide);
        return guide.getId();
    }

    public void linkMediaToGuide(Long guideId, List<Long> mediaIds) {
        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() -> new EntityNotFoundException("Guide not found"));
//        log.info("checkpoint_GuideService_LinkMediaToGuide");
        List<Media> mediaList = mediaRepository.findAllById(mediaIds);

        for (Media media : mediaList) {
            guide.assignGuide(media); // 양방향 관계 저장
            media.assignMedia(guide); // media ← guide 연결
        }
    }

}
