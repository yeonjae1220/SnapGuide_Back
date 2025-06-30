package yeonjae.snapguide.service.mediaMetaDataSerivce;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import yeonjae.snapguide.domain.cameraModel.CameraModel;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.CameraModelExtractor;
import yeonjae.snapguide.domain.media.mediaUtil.exifExtrator.ExifExtractor;
import yeonjae.snapguide.repository.cameraModelRepository.CameraModelRepository;
import yeonjae.snapguide.repository.mediaMetaDataRepository.MediaMetaDataRepository;
import yeonjae.snapguide.service.mediaMetaDataSerivce.MediaMetaDataService;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class MediaMetaDataServiceTest {

    @Mock
    private MediaMetaDataRepository mediaMetaDataRepository;
    @Mock private CameraModelRepository cameraModelRepository;

    @InjectMocks
    private MediaMetaDataService mediaMetaDataService;

    @Test
    void extractAndSave_shouldSaveMetaDataAndCameraModel() throws Exception {
        // given
        File dummyFile = new File("src/test/resources/sample.jpg");

        CameraModel dummyCamera = CameraModel.builder()
                .manufacturer("Apple")
                .model("iphone 12 mini")
                .lens("dummy")
                .build();
        MediaMetaData dummyMeta = new MediaMetaData();
        dummyMeta.assignCameraModel(dummyCamera);

        // mocking static methods
        try (MockedStatic<CameraModelExtractor> cameraMock = mockStatic(CameraModelExtractor.class);
             MockedStatic<ExifExtractor> exifMock = mockStatic(ExifExtractor.class)) {

            cameraMock.when(() -> CameraModelExtractor.extract(dummyFile)).thenReturn(dummyCamera);
            exifMock.when(() -> ExifExtractor.extract(dummyFile)).thenReturn(dummyMeta);

            when(cameraModelRepository.save(any())).thenReturn(dummyCamera);
            when(mediaMetaDataRepository.save(any())).thenReturn(dummyMeta);

            // when
            MediaMetaData savedMeta = mediaMetaDataService.extractAndSave(dummyFile);

            // then
            assertNotNull(savedMeta);
            verify(cameraModelRepository).save(dummyCamera);
            verify(mediaMetaDataRepository).save(dummyMeta);
        }
    }
}