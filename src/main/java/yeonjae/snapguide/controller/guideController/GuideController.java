package yeonjae.snapguide.controller.guideController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.controller.guideController.guideDto.GuideCreateResponseDto;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.controller.guideController.guideDto.GuideUpdateRequestDto;
import yeonjae.snapguide.controller.guideController.guideDto.LikeResponse;
import yeonjae.snapguide.domain.member.Member;
import yeonjae.snapguide.service.guideSerivce.GuideLikeService;
import yeonjae.snapguide.service.guideSerivce.GuideService;
import yeonjae.snapguide.service.mediaSerivce.BatchUploadResult;
import yeonjae.snapguide.service.mediaSerivce.MediaService;
import yeonjae.snapguide.service.memberSerivce.MemberService;

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
     * 통합 API: 파일 업로드 + Guide 생성 + Media 연결을 한 번에 처리.
     *
     * 변경 사항:
     * - 반환 타입: Long → GuideCreateResponseDto (스킵된 파일 정보 포함) ⚠️ Breaking Change
     * - throws IOException 제거: 파일별 IOException 이 내부에서 catch 되어 skippedFiles 에 기록됨
     * - 업로드 후 전체 실패 + 팁 없음: 400 에러 반환
     */
    @PostMapping("/upload")
    public ResponseEntity<GuideCreateResponseDto> createGuideWithMedia(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "tip", required = false) String tip,
            @RequestParam(value = "locationPublic", defaultValue = "true") boolean locationPublic,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude) {

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

        BatchUploadResult uploadResult = hasNoFiles
                ? new BatchUploadResult(List.of(), List.of())
                : mediaService.saveAllAndGet(Arrays.asList(files), latitude, longitude);

        // 업로드 후 전체 실패 + 팁 없음: 빈 Guide가 생성되는 것을 방지
        if (uploadResult.savedMedia().isEmpty() && hasNoTip) {
            throw new IllegalArgumentException(
                    "모든 파일 업로드에 실패했습니다. 팁을 작성하거나 다른 파일로 다시 시도해주세요. " +
                    "실패 파일: " + String.join(", ", uploadResult.skippedFileNames()));
        }

        Long guideId = guideService.createGuideWithMedia(
                member, tip, uploadResult.savedMedia(), locationPublic);

        int totalFiles = hasNoFiles ? 0 : files.length;
        int savedFiles = uploadResult.savedMedia().size();
        List<String> skippedFiles = uploadResult.skippedFileNames();

        String message = skippedFiles.isEmpty()
                ? "업로드 성공"
                : skippedFiles.size() + "개 파일 업로드 실패: " + String.join(", ", skippedFiles);

        GuideCreateResponseDto response = new GuideCreateResponseDto(
                guideId, totalFiles, savedFiles, skippedFiles, message);

        URI location = URI.create("/guide/api/" + guideId);
        return ResponseEntity.created(location).body(response);
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
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new IllegalArgumentException("유효하지 않은 좌표값입니다.");
        }
        double clampedRadius = Math.min(radius, 100.0);
        return ResponseEntity.ok(guideService.findGuidesNear(lat, lng, clampedRadius));
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
