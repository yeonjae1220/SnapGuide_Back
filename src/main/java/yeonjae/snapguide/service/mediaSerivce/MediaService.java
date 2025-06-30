package yeonjae.snapguide.service.mediaSerivce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.service.fileStorageService.FileStorageService;
import yeonjae.snapguide.service.fileStorageService.LocalFileStorageService;
import yeonjae.snapguide.service.guideSerivce.GuideService;
import yeonjae.snapguide.service.locationSerivce.LocationService;
import yeonjae.snapguide.service.mediaMetaDataSerivce.MediaMetaDataService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MediaService {
    private final FileStorageService fileStorageService;
    private final MediaMetaDataService mediaMetaDataService;
    private final LocationService locationService;
    private final GuideService guideService;
    private final MediaRepository mediaRepository;

    public List<Long> saveAll(List<MultipartFile> files) throws IOException {
            List<Long> ids = new ArrayList<>();

            for (MultipartFile file : files) {
                File savedFile = fileStorageService.saveFile(file);
                MediaMetaData metaData = mediaMetaDataService.extractAndSave(savedFile);
                Location location = locationService.extractAndResolveLocation(savedFile);
                String filePath = savedFile.getAbsolutePath();

                Media media = Media.builder()
                        .mediaName(file.getOriginalFilename())
                        .mediaUrl(filePath)
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

}
