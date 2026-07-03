package yeonjae.snapguide.service.cameraModelService;

import com.drew.metadata.Metadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeonjae.snapguide.domain.cameraModel.CameraModel;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.CameraModelExtractor;
import yeonjae.snapguide.repository.cameraModelRepository.CameraModelRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class CameraModelService {
    private final CameraModelRepository cameraModelRepository;
    public CameraModel save(Metadata metadata) {
        CameraModel cameraModel = CameraModelExtractor.extract(metadata);
        return cameraModelRepository.save(cameraModel); // HACK : 얘도 나중에 cascade Persist로?
    }
}
