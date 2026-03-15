package yeonjae.snapguide.service.fileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import yeonjae.snapguide.domain.media.MediaDto;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServingService {

    private final FileStorageService fileStorageService;

    @Value("${storage.local.base-dir}")
    private String uploadBasePath;

    /**
     * 파일명으로 파일을 서빙.
     * S3: Presigned URL로 302 리다이렉트.
     * 로컬: Resource로 직접 응답.
     */
    public ResponseEntity<?> serveFile(String filename) throws IOException {
        Optional<String> presignedUrl = fileStorageService.generatePresignedUrl(filename);
        if (presignedUrl.isPresent()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(presignedUrl.get()));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        return serveLocalFile(filename);
    }

    /**
     * 로컬 스토리지에서 파일 목록 조회 (페이지네이션).
     */
    public List<MediaDto> listUploadedFiles(int page, int size) throws IOException {
        Path baseDir = Paths.get(uploadBasePath);

        if (!Files.exists(baseDir)) {
            return Collections.emptyList();
        }

        List<Path> finalFileList = collectUniqueImageFiles(baseDir);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, finalFileList.size());

        if (fromIndex >= finalFileList.size()) {
            return Collections.emptyList();
        }

        return finalFileList.subList(fromIndex, toIndex).stream()
                .map(path -> new MediaDto(
                        path.getFileName().toString(),
                        "/media/files/" + path.getFileName().toString()
                ))
                .toList();
    }

    /**
     * 로컬 스토리지 이미지 파일 수 조회.
     */
    public long countUploadedFiles() throws IOException {
        Path baseDir = Paths.get(uploadBasePath);

        if (!Files.exists(baseDir)) {
            return 0L;
        }

        return collectUniqueImageFiles(baseDir).size();
    }

    private ResponseEntity<?> serveLocalFile(String filename) throws IOException {
        try {
            Path baseDir = Paths.get(uploadBasePath).toAbsolutePath().normalize();
            Path primaryDir = baseDir.resolve("originals");

            Path filePath = primaryDir.resolve(filename).normalize();
            if (!Files.exists(filePath)) {
                filePath = baseDir.resolve(filename).normalize();
            }

            // Path traversal 방지
            if (!filePath.startsWith(baseDir)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * originals/ 우선으로 중복 제거된 이미지 파일 목록 수집.
     */
    private List<Path> collectUniqueImageFiles(Path baseDir) throws IOException {
        Path primaryDir = baseDir.resolve("originals");
        Map<String, Path> uniqueFiles = new HashMap<>();

        try (Stream<Path> stream = Files.list(baseDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> uniqueFiles.put(path.getFileName().toString(), path));
        }

        if (Files.exists(primaryDir)) {
            try (Stream<Path> stream = Files.list(primaryDir)) {
                stream.filter(Files::isRegularFile)
                        .forEach(path -> uniqueFiles.put(path.getFileName().toString(), path));
            }
        }

        return uniqueFiles.values().stream()
                .filter(path -> isImageFile(path.getFileName().toString()))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
    }

    private boolean isImageFile(String fileName) {
        String upper = fileName.toUpperCase();
        return upper.endsWith(".JPG") || upper.endsWith(".JPEG") ||
                upper.endsWith(".PNG") || upper.endsWith(".GIF") || upper.endsWith(".WEBP");
    }
}
