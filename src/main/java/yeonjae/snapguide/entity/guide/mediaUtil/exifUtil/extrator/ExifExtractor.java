package yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import yeonjae.snapguide.entity.guide.CameraModel;
import yeonjae.snapguide.entity.guide.MediaMetaData;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.*;

import java.io.File;

/**
 * https://exiftool.org/TagNames/EXIF.html
 */

public class ExifExtractor {
    public static MediaMetaData extract(File file) {
        MediaMetaData meta = new MediaMetaData();
        // CameraModel camera = new CameraModel(); SRP원칙 위해 다른 코드로 분리

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            // TODO : SRP 단일 책임 원칙 위해 GPS 코드 분리해야함
            GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);


            return MediaMetaData.builder()
                    .iso(exif != null && exif.containsTag(ExifIFD0Directory.TAG_ISO_EQUIVALENT)
                            ? exif.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) : null)
                    .shutterSpeed(exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)
                            ? ExifRationalToStringConverter.formatExposureTime(exif.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)) : null)
                    .aperture(exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_APERTURE)
                            ? ExifRationalToStringConverter.formatAperture(exif.getRational(ExifSubIFDDirectory.TAG_APERTURE)) : null)
                    .whiteBalance(ifd0 != null && ifd0.containsTag(ExifIFD0Directory.TAG_WHITE_BALANCE)
                            ? WhiteBalance.fromCode(ifd0.getInteger(ExifIFD0Directory.TAG_WHITE_BALANCE)) : null)
                    .focalLength(ifd0 != null && ifd0.containsTag(ExifIFD0Directory.TAG_FOCAL_LENGTH)
                            ? ifd0.getInteger(ExifIFD0Directory.TAG_FOCAL_LENGTH) : null)
                    .exposureCompensation(ifd0 != null && ifd0.containsTag(ExifIFD0Directory.TAG_EXPOSURE_BIAS)
                            ? ExifRationalToStringConverter.formatExposureCompensation(ifd0.getRational(ExifIFD0Directory.TAG_EXPOSURE_BIAS)) : null)
                    .flashMode(exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_FLASH)
                            ? FlashMode.fromCode(exif.getInteger(ExifSubIFDDirectory.TAG_FLASH)) : FlashMode.UNKNOWN)
                    .flashCode(exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_FLASH)
                            ? exif.getInteger(ExifSubIFDDirectory.TAG_FLASH) : null)
                    .zoomLevel(exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_DIGITAL_ZOOM_RATIO)
                            ? exif.getRational(ExifSubIFDDirectory.TAG_DIGITAL_ZOOM_RATIO).doubleValue() : null)
                    .roll(ifd0 != null && ifd0.containsTag(ExifIFD0Directory.TAG_ORIENTATION)
                            ? OrientationUtil.describeOrientation(ifd0.getInteger(ExifIFD0Directory.TAG_ORIENTATION)) : null)
                    .time(exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
                            ? ExifDataTimeParser.parse(exif.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) : null)
                    .build();


//            if (gps != null && gps.getGeoLocation() != null) {
//                meta.setLatitude(gps.getGeoLocation().getLatitude());
//                meta.setLongitude(gps.getGeoLocation().getLongitude());
//            }


        } catch (Exception e) {
            // TODO : log 처리로 바꿔야 함
            e.printStackTrace();
        }

        return meta;
    }
}
