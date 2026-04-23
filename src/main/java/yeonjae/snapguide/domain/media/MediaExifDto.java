package yeonjae.snapguide.domain.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import yeonjae.snapguide.domain.cameraModel.CameraModel;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaExifDto {
    private String manufacturer;
    private String model;
    private String lens;
    private Integer iso;
    private String shutterSpeed;
    private String aperture;
    private Integer focalLength;
    private String whiteBalance;
    private String flashMode;
    private LocalDateTime time;

    public static MediaExifDto fromEntity(Media media) {
        if (media == null || media.getMediaMetaData() == null) {
            return null;
        }

        MediaMetaData metaData = media.getMediaMetaData();
        CameraModel cameraModel = metaData.getCameraModel();

        return MediaExifDto.builder()
                .manufacturer(cameraModel != null ? cameraModel.getManufacturer() : null)
                .model(cameraModel != null ? cameraModel.getModel() : null)
                .lens(cameraModel != null ? cameraModel.getLens() : null)
                .iso(metaData.getIso())
                .shutterSpeed(metaData.getShutterSpeed())
                .aperture(metaData.getAperture())
                .focalLength(metaData.getFocalLength())
                .whiteBalance(metaData.getWhiteBalance() != null ? metaData.getWhiteBalance().name() : null)
                .flashMode(metaData.getFlashMode() != null ? metaData.getFlashMode().name() : null)
                .time(metaData.getTime())
                .build();
    }
}
