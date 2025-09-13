package yeonjae.snapguide.service.fileStorageService;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;


@Configuration
public class StorageConfig {
    @Value("${storage.type:local}") // local, nas, s3
    private String storageType;

    private final LocalFileStorageService local;
    private final NasFileStorageService nas;
    private final S3FileStorageService s3;

    public StorageConfig(LocalFileStorageService local, NasFileStorageService nas, S3FileStorageService s3) {
        this.local = local;
        this.nas = nas;
        this.s3 = s3;
    }

    @Bean
    public FileStorageService storageService() {
        switch (storageType) {
            case "nas": return nas;
            case "s3": return s3;
            default: return local;
        }
    }
}
