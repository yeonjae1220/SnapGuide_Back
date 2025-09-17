package yeonjae.snapguide.service.mediaSerivce;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.service.fileStorageService.FileStorageService;
import yeonjae.snapguide.service.fileStorageService.UploadFileDto;
import yeonjae.snapguide.service.guideSerivce.GuideService;
import yeonjae.snapguide.service.locationSerivce.LocationServiceGeoImpl;
import yeonjae.snapguide.service.mediaMetaDataSerivce.MediaMetaDataService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MediaService {
    private final FileStorageService fileStorageService;
    private final MediaMetaDataService mediaMetaDataService;
    private final LocationServiceGeoImpl locationServiceGeoImpl;
    private final GuideService guideService;
    private final MediaRepository mediaRepository;

    public List<Long> saveAll(List<MultipartFile> files) throws IOException {
            List<Long> ids = new ArrayList<>();
            for (MultipartFile file : files) {
                UploadFileDto savedFile = fileStorageService.uploadFile(file); // 로컬 파일에 저장
                MediaMetaData metaData = mediaMetaDataService.extractAndSave(savedFile.getOriginalFileBytes());
                Location location = locationServiceGeoImpl.extractAndResolveLocation(savedFile.getOriginalFileBytes());
                String publicUrl = "/media/files/" + Paths.get(savedFile.getOriginalDir()).getFileName().toString(); // NOTE : localStorage용 저장 방법

                        Media media = Media.builder()
                        .mediaName(file.getOriginalFilename())
                        .mediaUrl(publicUrl)
                        .fileSize(file.getSize())
                        .build();

                media.assignMedia(metaData, location);
                mediaRepository.save(media);
                ids.add(media.getId());
            }
            return ids;
        }

    public List<Media> getAllMedia() {
        return mediaRepository.findAll();
    }

//    public List<Media> getUserMedias() {
//
//    }

    public String getPublicUrl(File savedFile) {
        // 외부 uploads 디렉토리는 루트에 그대로 매핑되므로 `/uuid.jpg` 형태면 됨
        return "/" + savedFile.getName();
    }

    /**
     * 가이드 저장할 때 locationId 값 임시로 저장하기 위한 메서드
     */
    public Long getOneLocationId(List<Long> mediaIds) {
        List<Long> locationIds = mediaRepository.findFirstLocationIdByMediaIds(mediaIds, PageRequest.of(0, 1));
//        return locationIds.stream().findFirst()
//                .orElseThrow(() -> new EntityNotFoundException("No media with location found"));
        return locationIds.stream().findFirst().orElse(null);
    }

}
