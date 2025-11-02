package yeonjae.snapguide.service.fileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.service.fileStorageService.fileConverter.HeicConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageService implements FileStorageService{

    /**
     * System.getProperty("user.dir")는 JVM이 실행 중인 현재 디렉토리의 절대 경로
     */
//    private final String uploadPath = System.getProperty("user.dir") + "/uploads";


//    private final Path uploadDir = Paths.get("uploads");
    // 변경: 절대 경로 사용
    private final Path uploadDir = Paths.get("/Users/kim-yeonjae/Desktop/Study/snapguide/uploads");
    private final Path uploadOriginalDir = Paths.get("/Users/kim-yeonjae/Desktop/Study/snapguide/uploads/originals");
    private final HeicConverter heicConverter = new HeicConverter();

//    @PostConstruct
//    public void init() throws IOException {
//        Files.createDirectories(uploadDir);
//        Files.createDirectories(uploadOriginalDir);
//    }

    // TODO : 년, 월, 일로 파일 이름 분류해서 넣어주기
    @Override
    public File saveFile(MultipartFile multipartFile) throws IOException {
        String originalFileName = multipartFile.getOriginalFilename();
        if (originalFileName == null) throw new IOException("파일 이름이 없습니다.");

        String extension = getExtension(originalFileName); // 예: "jpg", "heic"
        String baseFileName = UUID.randomUUID().toString();

        if (extension.equals("heic")) {
            // HEIC 원본 저장
            Path heicOriginalPath = uploadOriginalDir.resolve(baseFileName + ".heic");
            Files.copy(multipartFile.getInputStream(), heicOriginalPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[saveFile] HEIC 원본 저장됨 → {}", heicOriginalPath.toAbsolutePath());

            // HEIC → JPG 변환
            File jpegFile = heicConverter.convertHeicToJpg(heicOriginalPath.toFile(), uploadDir.toString());
            log.info("[saveFile] HEIC → JPG 변환 완료 → {}", jpegFile.getAbsolutePath());
            return jpegFile;
        }

        // 일반 이미지 저장 (원래 확장자 유지)
        Path targetPath = uploadDir.resolve(baseFileName + "." + extension);
        Files.copy(multipartFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("[saveFile] 일반 파일 저장 완료 → {}", targetPath.toAbsolutePath());
        return targetPath.toFile();
    }

    /**
     * 확장자 추출 (예: "jpg", "heic", "png")
     */
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase();
    }

    @Override
    public Resource loadFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Resource resource = new UrlResource(path.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new FileNotFoundException("파일을 읽을 수 없습니다: " + filePath);
        }
    }

    @Override
    public String generatePublicUrl(String filePath) {
        return "/media/files/" + Paths.get(filePath).getFileName().toString(); // 예: uuid_img.jpg
    }

}
