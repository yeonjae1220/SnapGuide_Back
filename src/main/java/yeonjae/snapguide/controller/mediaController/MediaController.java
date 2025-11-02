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
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.service.fileStorageService.MediaResponseDto;
import yeonjae.snapguide.service.mediaSerivce.MediaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    private final Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");


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
        List<Media> mediaList = mediaService.getAllMedia();
        List<MediaResponseDto> response = mediaList.stream()
                .map(media -> new MediaResponseDto(media.getMediaUrl()))
                .collect(Collectors.toList());
        log.info("media/list response : " + response);
        return ResponseEntity.ok(response);
    }

    // 파일 다운로드: URL로 접근 (e.g. /media/files/uuid.jpg)
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws IOException {
        Path filePath = uploadDir.resolve(filename).normalize();
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource file = new UrlResource(filePath.toUri());

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // heic인 경우 강제 다운로드 유도
        // jpeg 변환으로 변경
//        if (filename.toLowerCase().endsWith(".heic")) {
//            return ResponseEntity.ok()
//                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                    .header("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"")
//                    .body(file);
//        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(file);
    }

    // 모든 업로드된 파일 목록 (MediaDto 리스트 반환)
    @GetMapping("/allphoto")
    public ResponseEntity<List<MediaDto>> listAllUploadedFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws IOException {
        if (!Files.exists(uploadDir)) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // 확장자 필터 먼저 적용
        List<Path> filteredFiles = Files.list(uploadDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toUpperCase();
                    return fileName.endsWith(".JPG") || fileName.endsWith(".JPEG") ||
                            fileName.endsWith(".PNG") || fileName.endsWith(".GIF") ||
                            fileName.endsWith(".WEBP");
                })
                .sorted(Comparator.comparing(Path::getFileName))
                .toList();

        // 페이지네이션 적용
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filteredFiles.size());

        if (fromIndex >= filteredFiles.size()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<MediaDto> pagedFiles = filteredFiles.subList(fromIndex, toIndex).stream()
                .map(path -> new MediaDto(
                        path.getFileName().toString(),
                        "/media/files/" + path.getFileName().toString()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(pagedFiles);
    }

    @GetMapping("/allphoto/count")
    public ResponseEntity<Long> getPhotoCount() throws IOException {
        if (!Files.exists(uploadDir)) {
            return ResponseEntity.ok(0L);
        }

        long count = Files.list(uploadDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toUpperCase();
                    return fileName.endsWith(".JPG") || fileName.endsWith(".JPEG") ||
                            fileName.endsWith(".PNG") || fileName.endsWith(".GIF") ||
                            fileName.endsWith(".WEBP"); // 필요한 확장자만 허용
                })
                .count();

        return ResponseEntity.ok(count);
    }



//    @GetMapping("/api/list")
//    public List<MediaResponseDto> list(@AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long memberId = userDetails.getMember().getId();  // 로그인한 사용자 식별
//        return mediaService.getMediaListByMemberId(memberId);
//    }


}
