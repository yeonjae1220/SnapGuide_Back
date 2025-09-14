package yeonjae.snapguide.service.cameraModelService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.cameraModel.CameraModel;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.CameraModelExtractor;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.ExifExtractor;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;
import yeonjae.snapguide.repository.cameraModelRepository.CameraModelRepository;

import java.io.File;
import java.io.InputStream;

@Service
@Transactional
@RequiredArgsConstructor
public class CameraModelService {
    private final CameraModelRepository cameraModelRepository;
    public CameraModel save(InputStream inputStream) {
        // EXIF 메타데이터 추출
        MediaMetaData metaData = ExifExtractor.extract(inputStream);
        // 카메라 모델 추출 && 저장
        CameraModel cameraModel = CameraModelExtractor.extract(inputStream);
        return cameraModelRepository.save(cameraModel); // HACK : 얘도 나중에 cascade Persist로?
    }
}
