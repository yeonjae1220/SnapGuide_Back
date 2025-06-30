package yeonjae.snapguide.service.fileStorageService;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService{

    /**
     * System.getProperty("user.dir")는 JVM이 실행 중인 현재 디렉토리의 절대 경로
     */
    private final String uploadPath = System.getProperty("user.dir") + "/uploads";

    // TODO : 년, 월, 일로 파일 이름 분류해서 넣어주기
    @Override
    public File saveFile(MultipartFile file) throws IOException {
        String uuid = UUID.randomUUID().toString();
        String savedPath = uploadPath + "/" + uuid + "_" + file.getOriginalFilename();
        // 로컬 파일 저장
        File dest = new File(savedPath);
        file.transferTo(dest);
        return dest;
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
