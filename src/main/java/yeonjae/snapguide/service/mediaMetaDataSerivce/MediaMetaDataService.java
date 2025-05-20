package yeonjae.snapguide.service.mediaMetaDataSerivce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.cameraModel.CameraModel;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.CameraModelExtractor;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.ExifExtractor;
import yeonjae.snapguide.repository.cameraModelRepository.CameraModelRepository;
import yeonjae.snapguide.repository.mediaMetaDataRepository.MediaMetaDataRepository;

import java.io.File;

/**
 * 얘가 CameraModel 까지 책임
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MediaMetaDataService {
    private final MediaMetaDataRepository mediaMetaDataRepository;
    private final CameraModelRepository cameraModelRepository;

    public MediaMetaData extractAndSave(File file) {
        // EXIF 메타데이터 추출
        MediaMetaData metaData = ExifExtractor.extract(file);
        // 카메라 모델 추출 && 저장
        CameraModel cameraModel = CameraModelExtractor.extract(file);
        cameraModelRepository.save(cameraModel); // HACK : 얘도 나중에 cascade Persist로?
        // CameraModel 을 MediaMetaData에 연결
        metaData.assignCameraModel(cameraModel);
        return mediaMetaDataRepository.save(metaData);
    }
}
