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
import yeonjae.snapguide.service.cameraModelService.CameraModelService;

import java.io.File;

/**
 * 얘가 CameraModel 까지 책임
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MediaMetaDataService {
    private final MediaMetaDataRepository mediaMetaDataRepository;
    private final CameraModelService cameraModelService;
    public MediaMetaData extractAndSave(File file) {
        // EXIF 메타데이터 추출
        MediaMetaData metaData = ExifExtractor.extract(file);
        CameraModel cameraModel = cameraModelService.save(file);
        // CameraModel 을 MediaMetaData에 연결
        metaData.assignCameraModel(cameraModel);
        return mediaMetaDataRepository.save(metaData);
    }
}
