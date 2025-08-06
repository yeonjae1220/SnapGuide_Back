package yeonjae.snapguide.controller.guideController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.controller.guideController.guideDto.GuideCreateTestDto;

import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.domain.guide.GuideDto;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.service.guideSerivce.GuideService;
import yeonjae.snapguide.service.mediaSerivce.MediaService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/guide")
@Slf4j
public class GuideController {
    private final GuideService guideService;
    private final MediaService mediaService;
    private final MemberRepository memberRepository;
    @PostMapping("/api/upload")
    public Long testCreateGuide(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "tip", required = false) String tip)
            throws IOException {

        // 지금 유저 찾는 코드가 너무 돌고 돔, 거기다가 memberRepository의존까지 가지게 됨
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("no member : " +  email));
        Long memberId = member.getId();
        List<Long> ids = mediaService.saveAll(Arrays.asList(files));
        /**
         * TODO : 위치 정보 , media 대표 사진에서 뽑아 와야함, 혹은 collection 써서 전체적으로 뽑아두기?
         * -> 사용자에게 공개될 장소 정보, 제한적 공개 필요
         * 프론트 측에서 사용자 현재위치로 제안 or 가능하면 사진 데이터로 제안, 후 사용자가 원하지 않을지 직접 위치 선택
         * 사진들이 여러 장소에서 찍은 케이스 처리 필요 (여러 장소를 모두 저장하거나, 바운더리로 묶을 수 있으면 묶기)
         * 현재는 우선 저장된 media를 통해 조회해서 가장 먼저 나오는 위치 데이터 사용 -> location 데이터로 요청 보내야 할듯
         */
        Long locationId = mediaService.getOneLocationId(ids);
        GuideCreateTestDto request =  GuideCreateTestDto.of(memberId, tip, locationId, ids);

        Long guideId = guideService.createGuide(request);

        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            guideService.linkMediaToGuide(guideId, request.getMediaIds());
        }

        return guideId;
    }

    @GetMapping("/api/my")
    public ResponseEntity<List<GuideResponseDto>> myGuides(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("no member : " +  email));
        Long memberId = member.getId();
        return ResponseEntity.ok(guideService.getMyGuides(memberId));
    }

    @GetMapping("/api/nearby")
    public List<GuideDto> getNearbyGuides(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "20") double radius
    ) {
        return guideService.findGuidesNear(lat, lng, radius);
    }



    /**
     * 멤버 아이디를 가지고 멤버의 가이드 리스트 쭉 가져오기
     * 가이드에 들어있는 사진들과 팁을 각각 출력 해줘야함
     */
//    @GetMapping("/api/UserGuides")
//    public ResponseEntity<?> userGuides(
//            @AuthenticationPrincipal CustomUserDetails userDetails
//    ) {
//        Long memberId = userDetails.getMember().getId();
//
//    }
}
