package yeonjae.snapguide.service.fileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "storage.type", havingValue = "nas")
public class NasFileStorageService implements FileStorageService{
    // NOTE : 일단 임시 url이랑 코드임
    private final Path nasBaseDir = Paths.get("/mnt/nas/snapguide");

    @Override
    public UploadFileDto uploadFile(MultipartFile file) throws IOException {
        Path targetPath = nasBaseDir.resolve(file.getOriginalFilename());
        Files.createDirectories(targetPath.getParent());
        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            fos.write(file.getBytes());
        }

        return UploadFileDto.builder()
                .build();
    }

    @Override
    public void deleteFile(String path) throws IOException {
        Files.deleteIfExists(Paths.get(path));
    }


    @Override
    public Resource downloadFile(String path) throws IOException {
        return new UrlResource(Paths.get(path).toUri());
    }

    @Override
    public String generatePublicUrl(String path) {
        return "/media/files/" + Paths.get(path).getFileName().toString();
    }
}
