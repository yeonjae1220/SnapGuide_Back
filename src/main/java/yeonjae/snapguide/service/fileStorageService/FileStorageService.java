package yeonjae.snapguide.service.fileStorageService;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

public interface FileStorageService {
    File saveFile(MultipartFile file) throws IOException;

    Resource loadFile(String filePath) throws IOException;

    String generatePublicUrl(String filePath);
}

/**
 * TODO : 추후 S3 저장용 구현체도 만들 예정, 만들때 config파일 만들어서 빈 넣어서 yml파일 기반으로 선택할 수 있게 (노션 로컬에 저장하다 s3로 전환 방법에 정리)
 */