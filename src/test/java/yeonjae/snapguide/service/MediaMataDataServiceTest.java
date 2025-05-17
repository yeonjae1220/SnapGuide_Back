package yeonjae.snapguide.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import yeonjae.snapguide.entity.guide.CameraModel;
import yeonjae.snapguide.entity.guide.MediaMetaData;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator.CameraModelExtractor;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator.ExifExtractor;
import yeonjae.snapguide.repository.CameraModelRepository;
import yeonjae.snapguide.repository.MediaMetaDataRepository;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetaDataServiceTest {

    @Mock
    private MediaMetaDataRepository mediaMetaDataRepository;
    @Mock private CameraModelRepository cameraModelRepository;

    @InjectMocks
    private MediaMataDataService mediaMataDataService;

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
            MediaMetaData savedMeta = mediaMataDataService.extractAndSave(dummyFile);

            // then
            assertNotNull(savedMeta);
            verify(cameraModelRepository).save(dummyCamera);
            verify(mediaMetaDataRepository).save(dummyMeta);
        }
    }
}