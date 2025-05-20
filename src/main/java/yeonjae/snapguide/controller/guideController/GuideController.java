package yeonjae.snapguide.controller.guideController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yeonjae.snapguide.controller.guideController.guideDto.GuideCreateTestDto;
import yeonjae.snapguide.service.guideSerivce.GuideService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/guide")
@Slf4j
public class GuideController {
    private final GuideService guideService;
    @PostMapping("/api/upload")
    public Long testCreateGuide(@RequestBody GuideCreateTestDto request) {
        Long guideId = guideService.createGuide(request);

        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            guideService.linkMediaToGuide(guideId, request.getMediaIds());
        }

        return guideId;
    }
}
