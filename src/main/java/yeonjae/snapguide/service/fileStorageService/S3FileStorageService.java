package yeonjae.snapguide.service.fileStorageService;

import com.amazonaws.services.s3.AmazonS3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageService implements FileStorageService{

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;


    @Override
    public File uploadFile(MultipartFile file) throws IOException {

    }

    @Override
    public Resource downloadFile(String filePath) throws IOException {
        return null;
    }

    @Override
    public void deleteFile(String filePath) throws IOException {

    }

    @Override
    public String generatePublicUrl(String filePath) {
        return null;
    }
}
