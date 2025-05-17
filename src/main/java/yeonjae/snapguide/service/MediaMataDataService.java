package yeonjae.snapguide.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.entity.guide.CameraModel;
import yeonjae.snapguide.entity.guide.MediaMetaData;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator.CameraModelExtractor;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator.ExifExtractor;
import yeonjae.snapguide.repository.CameraModelRepository;
import yeonjae.snapguide.repository.MediaMetaDataRepository;

import java.io.File;

/**
 * 얘가 CameraModel 까지 책임
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MediaMataDataService {
    private final MediaMetaDataRepository mediaMetaDataRepository;
    private final CameraModelRepository cameraModelRepository;

    public MediaMetaData extractAndSave(File file) {
        // EXIF 메타데이터 추출
        MediaMetaData metaData = ExifExtractor.extract(file);
        // 카메라 모델 추출 && 저장
        CameraModel cameraModel = CameraModelExtractor.extract(file);
        cameraModelRepository.save(cameraModel);
        // CameraModel 을 MediaMetaData에 연결
        metaData.assignCameraModel(cameraModel);
        return mediaMetaDataRepository.save(metaData);
    }
}
