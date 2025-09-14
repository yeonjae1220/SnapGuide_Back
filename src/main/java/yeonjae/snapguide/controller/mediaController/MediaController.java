package yeonjae.snapguide.controller.mediaController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.service.fileStorageService.MediaResponseDto;
import yeonjae.snapguide.service.mediaSerivce.MediaService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
@Slf4j
public class MediaController {

    private final MediaService mediaService;

//    private final Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
    @Value("${storage.local.base-dir}")
    private String uploadBasePath;


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
        try {
            Path baseDir = Paths.get(uploadBasePath).toAbsolutePath().normalize();
            Path primaryDir = baseDir.resolve("originals");

            // 2. 우선순위 1: 'uploads/originals' 디렉토리에서 파일을 찾습니다.
            Path filePath = primaryDir.resolve(filename).normalize();

            // 3. 우선순위 1 경로에 파일이 없으면, 우선순위 2: 'uploads' 디렉토리에서 다시 찾습니다.
            if (!Files.exists(filePath)) {
                filePath = baseDir.resolve(filename).normalize();
            }

            // 4. 보안 체크: 최종 경로가 허용된 기본 디렉토리(uploads)를 벗어나는지 확인합니다.
            if (!filePath.startsWith(baseDir)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 5. 최종적으로 결정된 경로에 파일이 존재하고 읽을 수 있는지 확인합니다.
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // 6. 파일의 Content-Type을 결정하고 클라이언트에게 파일을 전송합니다.
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 모든 업로드된 파일 목록 (MediaDto 리스트 반환)
    @GetMapping("/allphoto")
    public ResponseEntity<List<MediaDto>> listAllUploadedFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws IOException {

        Path baseDir = Paths.get(uploadBasePath);
        Path primaryDir = baseDir.resolve("originals");

        if (!Files.exists(baseDir)) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // 1. Map을 사용해 중복을 제거 (originals 우선)
        // Key: 파일 이름, Value: 파일의 전체 Path
        Map<String, Path> uniqueFilesMap = new HashMap<>();

        // 2. 우선순위 2: 'uploads' 디렉토리의 파일들을 먼저 Map에 추가
        try (Stream<Path> stream = Files.list(baseDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> uniqueFilesMap.put(path.getFileName().toString(), path));
        }

        // 3. 우선순위 1: 'uploads/originals' 디렉토리의 파일들을 추가 (중복 시 덮어쓰기)
        if (Files.exists(primaryDir)) {
            try (Stream<Path> stream = Files.list(primaryDir)) {
                stream.filter(Files::isRegularFile)
                        .forEach(path -> uniqueFilesMap.put(path.getFileName().toString(), path));
            }
        }

        // 4. Map의 값들(Path)을 리스트로 변환 후, 기존 로직 적용
        List<Path> finalFileList = uniqueFilesMap.values().stream()
                .filter(path -> {
                    String fileName = path.getFileName().toString().toUpperCase();
                    return fileName.endsWith(".JPG") || fileName.endsWith(".JPEG") ||
                            fileName.endsWith(".PNG") || fileName.endsWith(".GIF") ||
                            fileName.endsWith(".WEBP");
                })
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();

        // 페이지네이션 적용
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, finalFileList.size());

        if (fromIndex >= finalFileList.size()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<MediaDto> pagedFiles = finalFileList.subList(fromIndex, toIndex).stream()
                .map(path -> new MediaDto(
                        path.getFileName().toString(),
                        "/media/files/" + path.getFileName().toString() // URL 경로는 동일
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(pagedFiles);
    }

    @GetMapping("/allphoto/count")
    public ResponseEntity<Long> getPhotoCount() throws IOException {
        Path baseDir = Paths.get(uploadBasePath);
        Path primaryDir = baseDir.resolve("originals");

        if (!Files.exists(baseDir)) {
            return ResponseEntity.ok(0L);
        }

        // 1. Set을 사용해 중복된 파일 이름을 제거
        Set<String> uniqueFileNames = new HashSet<>();

        // 2. 두 디렉토리의 파일 이름을 모두 Set에 추가 (Set이 자동으로 중복 제거)
        try (Stream<Path> stream = Files.list(baseDir)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .forEach(uniqueFileNames::add);
        }
        if (Files.exists(primaryDir)) {
            try (Stream<Path> stream = Files.list(primaryDir)) {
                stream.filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .forEach(uniqueFileNames::add);
            }
        }

        // 3. Set에 담긴 고유한 파일 이름들 중 확장자 필터링 후 개수 계산
        long count = uniqueFileNames.stream()
                .filter(fileName -> {
                    String upperCaseName = fileName.toUpperCase();
                    return upperCaseName.endsWith(".JPG") || upperCaseName.endsWith(".JPEG") ||
                            upperCaseName.endsWith(".PNG") || upperCaseName.endsWith(".GIF") ||
                            upperCaseName.endsWith(".WEBP");
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
