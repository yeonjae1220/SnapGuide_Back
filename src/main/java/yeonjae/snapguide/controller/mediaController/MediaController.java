package yeonjae.snapguide.controller.mediaController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.service.fileStorageService.FileServingService;
import yeonjae.snapguide.service.fileStorageService.MediaResponseDto;
import yeonjae.snapguide.service.mediaSerivce.MediaService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
@Slf4j
public class MediaController {

    private final MediaService mediaService;
    private final FileServingService fileServingService;


    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("files") MultipartFile[] files,
                                    @RequestParam(value = "tip", required = false) String tip
                                    )throws IOException {
        log.info("tip 내용: {}", tip);
        List<Long> ids = mediaService.saveAll(Arrays.asList(files));
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/list")
    public ResponseEntity<List<MediaResponseDto>> list() {
        // Service에서 DTO로 변환된 데이터를 받아옴 (Lazy Loading 이슈 방지)
        List<MediaDto> mediaDtoList = mediaService.getAllMedia();
        List<MediaResponseDto> response = mediaDtoList.stream()
                .map(dto -> new MediaResponseDto(dto.getUrl()))
                .collect(Collectors.toList());
        log.info("media/list response : " + response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<?> serveFile(@PathVariable String filename) throws IOException {
        return fileServingService.serveFile(filename);
    }

    @GetMapping("/allphoto")
    public ResponseEntity<List<MediaDto>> listAllUploadedFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws IOException {
        return ResponseEntity.ok(fileServingService.listUploadedFiles(page, size));
    }

    @GetMapping("/allphoto/count")
    public ResponseEntity<Long> getPhotoCount() throws IOException {
        return ResponseEntity.ok(fileServingService.countUploadedFiles());
    }



//    @GetMapping("/api/list")
//    public List<MediaResponseDto> list(@AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long memberId = userDetails.getMember().getId();  // 로그인한 사용자 식별
//        return mediaService.getMediaListByMemberId(memberId);
//    }


}
