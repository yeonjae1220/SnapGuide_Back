package yeonjae.snapguide.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class FileStorageService {
    /**
     * System.getProperty("user.dir")는 JVM이 실행 중인 현재 디렉토리의 절대 경로
     */
    private final String uploadPath = System.getProperty("user.dir") + "/uploads";
    // TODO : 년, 월, 일로 파일 이름 분류해서 넣어주기
    public File saveFile(MultipartFile file) throws IOException {
        String uuid = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        String savedPath = uploadPath + "/" + uuid + "_" + originalFilename;
        // 로컬 파일 저장
        File dest = new File(savedPath);
        file.transferTo(dest);
        return dest;
    }
}
