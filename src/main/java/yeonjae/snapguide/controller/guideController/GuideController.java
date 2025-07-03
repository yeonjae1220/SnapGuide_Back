package yeonjae.snapguide.controller.guideController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.controller.guideController.guideDto.GuideCreateTestDto;
import yeonjae.snapguide.domain.member.CustomUserDetails;
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
    @PostMapping("/api/upload")
    public Long testCreateGuide(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "tip", required = false) String tip)
            throws IOException {

        Long memberId = userDetails.getMember().getId();
        List<Long> ids = mediaService.saveAll(Arrays.asList(files));
        // TODO : 위치 정보 , media 대표 사진에서 뽑아 와야함, 혹은 collection 써서 전체적으로 뽑아두기?
        GuideCreateTestDto request =  GuideCreateTestDto.of(memberId, tip, null, ids);

        Long guideId = guideService.createGuide(request);

        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            guideService.linkMediaToGuide(guideId, request.getMediaIds());
        }

        return guideId;
    }

    /**
     * 멤버 아이디를 가지고 멤버의 가이드 리스트 쭉 가져오기
     * 가이드에 들어있는 사진들과 팁을 각각 출력 해줘야함
     */
    @GetMapping("/api/UserGuides")
    public ResponseEntity<?> userGuides(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails.getMember().getId();

    }
}
