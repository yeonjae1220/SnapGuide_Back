package yeonjae.snapguide.controller.guideController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.controller.guideController.guideDto.GuideUpdateRequestDto;
import yeonjae.snapguide.controller.guideController.guideDto.LikeResponse;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.service.guideSerivce.GuideLikeService;
import yeonjae.snapguide.service.guideSerivce.GuideService;
import yeonjae.snapguide.service.mediaSerivce.MediaService;
import yeonjae.snapguide.service.memberSerivce.MemberService;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/guide/api")
@Slf4j
public class GuideController {
    private final GuideService guideService;
    private final GuideLikeService guideLikeService;
    private final MediaService mediaService;
    private final MemberService memberService;

    /**
     * 통합 API: 파일 업로드 + Guide 생성 + Media 연결을 한 번에 처리
     * - 원본만 빠르게 업로드 (동기)
     * - 썸네일/웹용은 백그라운드에서 비동기 생성
     */
    @PostMapping("/upload")
    public ResponseEntity<Long> createGuideWithMedia(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "tip", required = false) String tip,
            @RequestParam(value = "locationPublic", defaultValue = "true") boolean locationPublic,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude)
            throws IOException {

        boolean hasNoFiles = (files == null || files.length == 0);
        boolean hasNoTip = (tip == null || tip.trim().isEmpty());

        if (hasNoFiles && hasNoTip) {
            throw new IllegalArgumentException("사진 또는 팁 중 하나는 필수입니다.");
        }
        if (latitude != null && longitude != null) {
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("유효하지 않은 좌표값입니다.");
            }
        }

        Member member = memberService.getCurrentMember(userDetails.getUsername());

        List<Media> mediaList = hasNoFiles
                ? List.of()
                : mediaService.saveAllAndGet(Arrays.asList(files), latitude, longitude);

        Long guideId = guideService.createGuideWithMedia(member, tip, mediaList, locationPublic);

        URI location = URI.create("/guide/api/" + guideId);
        return ResponseEntity.created(location).body(guideId);
    }

    @GetMapping("/my")
    public ResponseEntity<List<GuideResponseDto>> myGuides(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = memberService.getCurrentMember(userDetails.getUsername()).getId();
        return ResponseEntity.ok(guideService.getMyGuides(memberId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GuideResponseDto> updateTip(
            @PathVariable Long id,
            @RequestBody @Valid GuideUpdateRequestDto req,
            @AuthenticationPrincipal UserDetails userDetails) {
        GuideResponseDto updated = guideService.updateTip(id, req.getTip(), userDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuide(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        guideService.deleteGuide(id, userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<GuideResponseDto>> getNearbyGuides(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "20") double radius) {
        return ResponseEntity.ok(guideService.findGuidesNear(lat, lng, radius));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuideResponseDto> getGuide(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(guideService.findGuideById(id, userDetails));
    }

    @PostMapping("/like/{id}")
    public ResponseEntity<LikeResponse> likeGuide(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean liked = guideLikeService.toggleLike(id, userDetails);
        GuideResponseDto updatedGuide = guideService.findGuideById(id, userDetails);
        return ResponseEntity.ok(new LikeResponse(liked, updatedGuide.getLikeCount()));
    }
}
