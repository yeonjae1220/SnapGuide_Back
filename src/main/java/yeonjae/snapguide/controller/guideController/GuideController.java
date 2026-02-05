package yeonjae.snapguide.controller.guideController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.controller.guideController.guideDto.GuideUpdateRequestDto;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.repository.memberRepository.MemberRepository;
import yeonjae.snapguide.service.guideSerivce.GuideService;
import yeonjae.snapguide.service.mediaSerivce.MediaService;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/guide/api")
@Slf4j
public class GuideController {
    private final GuideService guideService;
    private final MediaService mediaService;
    private final MemberRepository memberRepository;
    /**
     * 통합 API: 파일 업로드 + Guide 생성 + Media 연결을 한 번에 처리
     * - 원본만 빠르게 업로드 (동기)
     * - 썸네일/웹용은 백그라운드에서 비동기 생성
     * - Guide ID만 반환 (Media ID 불필요)
     */
    @PostMapping("/upload")
    public Long createGuideWithMedia(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "tip", required = false) String tip)
            throws IOException {

        // 1. 검증
        boolean hasNoFiles = (files == null || files.length == 0);
        boolean hasNoTip = (tip == null || tip.trim().isEmpty());

        if (hasNoFiles && hasNoTip) {
            throw new IllegalArgumentException("사진 또는 팁 중 하나는 필수입니다.");
        }

        // 2. 사용자 조회
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("no member : " + email));

        // 3. 파일 업로드 + Media 엔티티 생성 (비동기 썸네일 생성 시작)
        List<Media> mediaList = hasNoFiles
                ? List.of()
                : mediaService.saveAllAndGet(Arrays.asList(files));

        // 4. Guide 생성 + Media 연결 (한 번에 처리)
        Long guideId = guideService.createGuideWithMedia(member, tip, mediaList);

        return guideId;
    }

    @GetMapping("/my")
    public ResponseEntity<List<GuideResponseDto>> myGuides(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("no member : " +  email));
        Long memberId = member.getId();
        return ResponseEntity.ok(guideService.getMyGuides(memberId));
    }


    @PutMapping("/update")
    public ResponseEntity<GuideResponseDto> updateTip(@RequestBody GuideUpdateRequestDto req, @AuthenticationPrincipal UserDetails userDetails) {
        GuideResponseDto updated =  guideService.updateTip(req.getId(), req.getTip(), userDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteGuide(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        guideService.deleteGuide(id, userDetails);
    }


    @GetMapping("/nearby")
    public List<GuideResponseDto> getNearbyGuides(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "20") double radius
    ) {
        return guideService.findGuidesNear(lat, lng, radius);
    }
    // 게시글 상세 조회 API
    @GetMapping("/{id}")
    public ResponseEntity<GuideResponseDto> getGuide(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        GuideResponseDto guideDto = guideService.findGuideById(id, userDetails);
        return ResponseEntity.ok(guideDto);
    }

    @PostMapping("/like/{id}")
    public ResponseEntity<Map<String, Object>> likeGuide(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        boolean liked = guideService.toggleLike(id, userDetails);
        GuideResponseDto updatedGuide = guideService.findGuideById(id, userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("liked", liked);
        response.put("likeCount", updatedGuide.getLikeCount());

        return ResponseEntity.ok(response);
    }


//    @GetMapping("/api/distance")
//    public List<GuideDto> getGuidesDistance() {
//
//    }


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
