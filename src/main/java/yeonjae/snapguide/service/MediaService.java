package yeonjae.snapguide.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.entity.guide.CameraModel;
import yeonjae.snapguide.entity.guide.Media;
import yeonjae.snapguide.entity.guide.MediaMetaData;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator.CameraModelExtractor;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator.ExifExtractor;
import yeonjae.snapguide.repository.CameraModelRepository;
import yeonjae.snapguide.repository.MediaMetaDataRepository;
import yeonjae.snapguide.repository.MediaRepository;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MediaService {
    private final MediaRepository mediaRepository;
    private final MediaMetaDataRepository mediaMetaDataRepository;
    private final CameraModelRepository cameraModelRepository;

    /**
     * System.getProperty("user.dir")는 JVM이 실행 중인 현재 디렉토리의 절대 경로
     */
    private final String uploadPath = System.getProperty("user.dir") + "/uploads";

    public String saveMedia(MultipartFile file) throws IOException {
        // TODO : 년, 월, 일로 파일 이름 분류해서 넣어주기
        String uuid = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        String savedPath = uploadPath + "/" + uuid + "_" + originalFilename;

        // 1. 로컬 파일 저장
        File dest = new File(savedPath);
        file.transferTo(dest);

        // 2. EXIF 메타데이터 추출
        MediaMetaData metaData = ExifExtractor.extract(dest);
        // 카메라 모델 추출 && 저장
        CameraModel cameraModel = CameraModelExtractor.extract(dest);
        cameraModelRepository.save(cameraModel);  // 먼저 저장
        // CameraModel 을 MediaMetaData에 연결
        metaData.assignCameraModel(cameraModel);
        mediaMetaDataRepository.save(metaData); // NOTE : 이렇게 해도 되나? media랑 연관관계 제대로 걸리는지?

        Media media = Media.builder()
                .mediaName(originalFilename)
                .mediaUrl(savedPath)  // 추후 외부 저장소 대응 가능
                .fileSize(file.getSize())
                .build();
        // 4. MetaData 저장
        media.assignMedia(metaData);
        mediaRepository.save(media);


        return media.getId().toString();
    }
}
