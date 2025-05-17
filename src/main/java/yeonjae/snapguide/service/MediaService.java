package yeonjae.snapguide.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.entity.guide.*;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator.CameraModelExtractor;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator.ExifCoordinateExtractor;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator.ExifExtractor;
import yeonjae.snapguide.repository.MediaRepository;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MediaService {
    private final FileStorageService fileStorageService;
    private final MediaMataDataService mediaMataDataService;
    private final LocationService locationService;
    private final MediaRepository mediaRepository;
//    private final GuideRepository guideRepository;

    public Long saveMedia(MultipartFile file) throws IOException {

        File savedFile = fileStorageService.saveFile(file);
        MediaMetaData metaData = mediaMataDataService.extractAndSave(savedFile);
        Location location = locationService.extractAndResolveLocation(savedFile);

        // 1. 위도/경도 기반으로 주소 정보 가져오기
//        Guide guide = guideRepository.findById(guideId)
//                .orElseThrow(() -> new EntityNotFoundException("Guide not found with id: " + guideId));

        Media media = Media.builder()
                .mediaName(file.getOriginalFilename())
                .mediaUrl(savedFile.getAbsolutePath())
                .fileSize(file.getSize())
                .build();

        media.assignMedia(metaData, location);
        mediaRepository.save(media);
        return media.getId();
    }
}
