package yeonjae.snapguide.controller.mediaController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.service.fileStorageService.MediaResponseDto;
import yeonjae.snapguide.service.mediaSerivce.MediaService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<List<Long>> upload(@RequestParam("files") MultipartFile[] files) throws IOException {
        List<Long> ids = mediaService.saveAll(Arrays.asList(files));
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/list")
    public ResponseEntity<List<MediaResponseDto>> list() {
        List<Media> mediaList = mediaService.getAllMedia();
        List<MediaResponseDto> response = mediaList.stream()
                .map(media -> new MediaResponseDto(media.getMediaUrl()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // URL로 접근할 수 있게 매핑 (e.g. /media/files/uuid_name.jpg)
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws IOException {
        Path filePath = Paths.get(System.getProperty("user.dir"), "uploads", filename);
        Resource file = new UrlResource(filePath.toUri());

        if (!file.exists() || !file.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // TODO :  동적으로 감지하게 해줘야함
                .body(file);
    }



//    @GetMapping("/api/list")
//    public List<MediaResponseDto> list(@AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long memberId = userDetails.getMember().getId();  // 로그인한 사용자 식별
//        return mediaService.getMediaListByMemberId(memberId);
//    }


}
