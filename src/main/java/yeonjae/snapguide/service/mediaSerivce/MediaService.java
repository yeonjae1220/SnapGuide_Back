package yeonjae.snapguide.service.mediaSerivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.Media;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.domain.media.MediaMapper;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;
import yeonjae.snapguide.repository.mediaRepository.MediaRepository;
import yeonjae.snapguide.service.fileStorageService.AsyncFileProcessingService;
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
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MediaService {
    private final FileStorageService fileStorageService;
    private final AsyncFileProcessingService asyncFileProcessingService;
    private final MediaMetaDataService mediaMetaDataService;
    private final LocationServiceGeoImpl locationServiceGeoImpl;
    private final GuideService guideService;
    private final MediaRepository mediaRepository;

    /**
     * 빠른 업로드: 원본만 저장 후 즉시 응답, 썸네일/웹용은 비동기 처리
     * Guide와 함께 사용 시 내부적으로 Media 엔티티 생성 및 연결
     *
     * @return 저장된 Media 엔티티 리스트 (Guide 연결용)
     */
    public List<Media> saveAllAndGet(List<MultipartFile> files) throws IOException {
        List<Media> savedMediaList = new ArrayList<>();
        List<AsyncTask> asyncTasks = new ArrayList<>();

        for (MultipartFile file : files) {
            long startTime = System.currentTimeMillis();

            // 1. 원본만 빠르게 업로드 (동기)
            UploadFileDto savedFile = fileStorageService.uploadOriginalOnly(file);

            // 2. 메타데이터 & 위치 정보 추출
            MediaMetaData metaData = mediaMetaDataService.extractAndSave(savedFile.getOriginalFileBytes());
            Location location = locationServiceGeoImpl.extractAndResolveLocation(savedFile.getOriginalFileBytes());

            // 3. 임시 URL (원본 파일 기반) - 비동기 처리 완료 후 업데이트됨
            String tempUrl = "/media/files/" + savedFile.getBaseFileName() + ".jpg";

            // 4. Media 엔티티 저장
            Media media = Media.builder()
                    .mediaName(file.getOriginalFilename())
                    .mediaUrl(tempUrl)
                    .originalKey(savedFile.getOriginalKey())
                    .webKey(null)        // 비동기 처리 후 업데이트
                    .thumbnailKey(null)  // 비동기 처리 후 업데이트
                    .fileSize(file.getSize())
                    .build();

            media.assignMedia(metaData, location);
            mediaRepository.save(media);
            savedMediaList.add(media);

            // 5. 비동기 작업 예약
            asyncTasks.add(new AsyncTask(
                    media.getId(),
                    savedFile.getBaseFileName(),
                    savedFile.getOriginalFileBytes()
            ));

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[Upload] Media {} saved in {}ms (async derivatives pending)", media.getId(), elapsed);
        }

        // 6. 비동기 작업 시작
        for (AsyncTask task : asyncTasks) {
            asyncFileProcessingService.generateDerivativesAsync(
                    task.mediaId, task.baseFileName, task.originalBytes
            );
        }

        return savedMediaList;
    }

    /**
     * 빠른 업로드 (ID만 반환) - 테스트/레거시 호환용
     */
    public List<Long> saveAll(List<MultipartFile> files) throws IOException {
        return saveAllAndGet(files).stream()
                .map(Media::getId)
                .toList();
    }

    /**
     * 동기 업로드 (레거시 호환용)
     * @deprecated 성능 이슈로 saveAll() 사용 권장
     */
    @Deprecated
    public List<Long> saveAllSync(List<MultipartFile> files) throws IOException {
        List<Long> ids = new ArrayList<>();
        for (MultipartFile file : files) {
            @SuppressWarnings("deprecation")
            UploadFileDto savedFile = fileStorageService.uploadFile(file);
            MediaMetaData metaData = mediaMetaDataService.extractAndSave(savedFile.getOriginalFileBytes());
            Location location = locationServiceGeoImpl.extractAndResolveLocation(savedFile.getOriginalFileBytes());

            String webFileName;
            if (savedFile.getWebDir() != null && !savedFile.getWebDir().isEmpty()) {
                webFileName = Paths.get(savedFile.getWebDir()).getFileName().toString();
            } else {
                webFileName = Paths.get(savedFile.getThumbnailDir()).getFileName().toString();
            }

            String publicUrl = "/media/files/" + webFileName;

            Media media = Media.builder()
                    .mediaName(file.getOriginalFilename())
                    .mediaUrl(publicUrl)
                    .originalKey(savedFile.getOriginalDir())
                    .webKey(savedFile.getWebDir())
                    .thumbnailKey(savedFile.getThumbnailDir())
                    .fileSize(file.getSize())
                    .build();

            media.assignMedia(metaData, location);
            mediaRepository.save(media);
            ids.add(media.getId());
        }
        return ids;
    }

    private record AsyncTask(Long mediaId, String baseFileName, byte[] originalBytes) {}

    /**
     * 모든 Media를 DTO로 반환
     * Entity를 직접 반환하지 않고 DTO로 변환하여 Lazy Loading 이슈 방지
     * @return MediaDto 리스트
     */
    public List<MediaDto> getAllMedia() {
        return mediaRepository.findAll()
                .stream()
                .map(MediaMapper::toDto)
                .collect(Collectors.toList());
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
