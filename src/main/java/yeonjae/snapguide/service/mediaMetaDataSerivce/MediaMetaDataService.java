package yeonjae.snapguide.service.mediaMetaDataSerivce;

import com.drew.metadata.Metadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.cameraModel.CameraModel;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.ExifExtractor;
import yeonjae.snapguide.repository.mediaMetaDataRepository.MediaMetaDataRepository;
import yeonjae.snapguide.service.cameraModelService.CameraModelService;

/**
 * 얘가 CameraModel 까지 책임
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MediaMetaDataService {
    private final MediaMetaDataRepository mediaMetaDataRepository;
    private final CameraModelService cameraModelService;

    /**
     * 호출 측에서 이미지 1개당 한 번만 파싱한 Metadata를 넘겨받는다.
     * ExifExtractor / CameraModelExtractor가 각자 재파싱하지 않도록 하기 위함.
     */
    public MediaMetaData extractAndSave(Metadata metadata) {
        MediaMetaData metaData = ExifExtractor.extract(metadata);
        CameraModel cameraModel = cameraModelService.save(metadata);
        // CameraModel 을 MediaMetaData에 연결
        metaData.assignCameraModel(cameraModel);
        return mediaMetaDataRepository.save(metaData);
    }
}
