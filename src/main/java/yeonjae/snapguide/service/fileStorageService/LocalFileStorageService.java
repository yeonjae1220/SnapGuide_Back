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

import net.coobird.thumbnailator.Thumbnails;

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
    public File uploadFile(MultipartFile multipartFile) throws IOException {
        // 파일 기본 정보 설정
        String originalFileName = multipartFile.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) throw new IOException("파일 이름이 없습니다.");

        String extension = getExtension(originalFileName); // 예: "jpg", "heic"
        String baseFileName = UUID.randomUUID().toString();

        // 원본 파일 저장 (모든 파일 공통)
        Path originalPath = uploadOriginalDir.resolve(baseFileName + "." + extension);
        Files.createDirectories(originalPath.getParent());
        Files.copy(multipartFile.getInputStream(), originalPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("[uploadFile] 원본 파일 저장됨 → {}", originalPath.toAbsolutePath());

        // 웹용 고화질 JPG 생성
        // 이 파일이 '원본 보기'에 사용될 파일
        Path webOriginalJpgPath = uploadDir.resolve(baseFileName + ".jpg");
        Files.createDirectories(webOriginalJpgPath.getParent());

        boolean isHeic = "heic".equalsIgnoreCase(extension);

        if (isHeic) {
            // HEIC인 경우 -> 고화질 JPG로 변환하여 저장
            heicConverter.convertHeicToJpg(originalPath.toFile(), webOriginalJpgPath.toString());
            log.info("[uploadFile] HEIC -> 웹용 원본 JPG 변환 완료 → {}", webOriginalJpgPath.toAbsolutePath());
        } else {
            // JPG, PNG 등 다른 이미지인 경우 -> 원본을 그대로 복사 또는 변환 저장
            // 여기서는 Thumbnails를 이용해 품질 저하 없이 JPG로 저장하여 포맷을 통일합니다.
            Thumbnails.of(originalPath.toFile())
                    .scale(1.0) // 원본 크기 그대로
                    .outputQuality(1.0) // 원본 품질 그대로
                    .outputFormat("jpg")
                    .toFile(webOriginalJpgPath.toFile());
            log.info("[uploadFile] 일반 이미지 -> 웹용 원본 JPG 저장 완료 → {}", webOriginalJpgPath.toAbsolutePath());
        }

        // 4. 웹용 썸네일 JPG 생성 (고화질 JPG를 기반으로 생성)
        Path thumbnailPath = uploadDir.resolve(baseFileName + "_thumb.jpg");
        Thumbnails.of(webOriginalJpgPath.toFile())
                .size(1080, 1080)
                .outputQuality(0.7)
                .toFile(thumbnailPath.toFile());
        log.info("[uploadFile] 웹용 썸네일 생성 완료 → {}", thumbnailPath.toAbsolutePath());

        // 우선은 원본 jpg 파일을 반환
        return webOriginalJpgPath.toFile();

//        if (extension.equals("heic")) {
//            // HEIC 원본 저장
//            Path heicOriginalPath = uploadOriginalDir.resolve(baseFileName + ".heic");
//            Files.copy(multipartFile.getInputStream(), heicOriginalPath, StandardCopyOption.REPLACE_EXISTING);
//            log.info("[saveFile] HEIC 원본 저장됨 → {}", heicOriginalPath.toAbsolutePath());
//
//            // HEIC → JPG 변환
//            File jpegFile = heicConverter.convertHeicToJpg(heicOriginalPath.toFile(), uploadDir.toString());
//            log.info("[saveFile] HEIC → JPG 변환 완료 → {}", jpegFile.getAbsolutePath());
//            return jpegFile;
//        }
//
//        // 일반 이미지 저장 (원래 확장자 유지)
//        Path targetPath = uploadDir.resolve(baseFileName + "." + extension);
//        Files.copy(multipartFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
//        log.info("[saveFile] 일반 파일 저장 완료 → {}", targetPath.toAbsolutePath());
//        return targetPath.toFile();
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
    public Resource downloadFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Resource resource = new UrlResource(path.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new FileNotFoundException("파일을 읽을 수 없습니다: " + filePath);
        }
    }

    @Override
    public void deleteFile(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
    }

    @Override
    public String generatePublicUrl(String filePath) {
        return "/media/files/" + Paths.get(filePath).getFileName().toString(); // 예: uuid_img.jpg
    }

}


// TODO : 썸네일용 압축 파일도 처리 해야함