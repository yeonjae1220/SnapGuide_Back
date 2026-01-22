package yeonjae.snapguide.controller.guideController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.controller.guideController.guideDto.GuideCreateTestDto;

import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;

import yeonjae.snapguide.controller.guideController.guideDto.GuideUpdateRequestDto;
import yeonjae.snapguide.controller.guideController.guideDto.SliceResponse;

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
    @PostMapping("/upload")
    public Long testCreateGuide(
            @AuthenticationPrincipal UserDetails userDetails,

            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "tip", required = false) String tip)
            throws IOException {

        // 둘 다 비었을 경우 업로드 차단
        boolean hasNoFiles = (files == null || files.length == 0);
        boolean hasNoTip = (tip == null || tip.trim().isEmpty());

        if (hasNoFiles && hasNoTip) {
            throw new IllegalArgumentException("사진 또는 팁 중 하나는 필수입니다.");
        }

        // 지금 유저 찾는 코드가 너무 돌고 돔, 거기다가 memberRepository의존까지 가지게 됨
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("no member : " +  email));
        Long memberId = member.getId();

        // 사진 저장
        List<Long> ids = hasNoFiles ? List.of() : mediaService.saveAll(Arrays.asList(files));

        /**
         * TODO : 위치 정보 , media 대표 사진에서 뽑아 와야함, 혹은 collection 써서 전체적으로 뽑아두기?
         * -> 사용자에게 공개될 장소 정보, 제한적 공개 필요
         * 프론트 측에서 사용자 현재위치로 제안 or 가능하면 사진 데이터로 제안, 후 사용자가 원하지 않을지 직접 위치 선택
         * 사진들이 여러 장소에서 찍은 케이스 처리 필요 (여러 장소를 모두 저장하거나, 바운더리로 묶을 수 있으면 묶기)
         * 현재는 우선 저장된 media를 통해 조회해서 가장 먼저 나오는 위치 데이터 사용 -> location 데이터로 요청 보내야 할듯
         */

        // 위치 ID 추정 (없으면 null)
        Long locationId = ids.isEmpty() ? null : mediaService.getOneLocationId(ids);

        GuideCreateTestDto request =  GuideCreateTestDto.of(memberId, tip, locationId, ids);

        Long guideId = guideService.createGuide(request);

        // 사진이 있다면 가이드에 연결
        if (!ids.isEmpty()) {
            guideService.linkMediaToGuide(guideId, ids);
        }

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

    /**
     * 커서 기반 페이징으로 주변 가이드 조회
     * @param lat 위도
     * @param lng 경도
     * @param radius 검색 반경 (km)
     * @param cursor 마지막으로 조회한 가이드 ID (다음 페이지 커서)
     * @param size 페이지 크기 (기본값: 20)
     * @param userDetails 현재 로그인한 사용자 정보 (선택)
     * @return 페이징된 가이드 목록 (SliceResponse)
     */
    @GetMapping("/nearby/paged")
    public ResponseEntity<SliceResponse<GuideResponseDto>> getNearbyGuidesPaged(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "20") double radius,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("🌍 [getNearbyGuidesPaged] 요청 - lat: {}, lng: {}, radius: {}, cursor: {}, size: {}",
                lat, lng, radius, cursor, size);

        // 현재 사용자 ID 조회 (좋아요 정보 조회용)
        Long currentMemberId = null;
        if (userDetails != null) {
            Member member = memberRepository.findByEmail(userDetails.getUsername())
                    .orElse(null);
            if (member != null) {
                currentMemberId = member.getId();
            }
        }

        // 페이징된 가이드 조회
        Slice<GuideResponseDto> slice = guideService.findGuidesNearPaged(
                lat, lng, radius, cursor, size, currentMemberId
        );

        // SliceResponse로 변환
        SliceResponse<GuideResponseDto> response = SliceResponse.from(
                slice,
                GuideResponseDto::getId  // 커서 추출 함수
        );

        log.info("✅ [getNearbyGuidesPaged] 응답 - content: {}, hasNext: {}, nextCursor: {}",
                response.getSize(), response.isHasNext(), response.getNextCursor());

        return ResponseEntity.ok(response);
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
