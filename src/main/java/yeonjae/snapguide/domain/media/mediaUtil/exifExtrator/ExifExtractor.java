package yeonjae.snapguide.domain.media.mediaUtil.exifExtrator;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;
import yeonjae.snapguide.domain.media.mediaUtil.exifUtil.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * https://exiftool.org/TagNames/EXIF.html
 */

public class ExifExtractor {
    public static MediaMetaData extract(byte[] imageBytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));
            ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            MediaMetaData.MediaMetaDataBuilder builder = MediaMetaData.builder();

            builder.iso(getIntegerTag(exif, ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
            builder.shutterSpeed(getFormattedExposureTime(exif));
            builder.aperture(getFormattedAperture(exif));
            builder.whiteBalance(getWhiteBalance(ifd0));
            builder.focalLength(getIntegerTag(ifd0, ExifIFD0Directory.TAG_FOCAL_LENGTH));
            builder.exposureCompensation(getFormattedExposureCompensation(ifd0));
            builder.flashMode(getFlashMode(exif));
            builder.flashCode(getIntegerTag(exif, ExifSubIFDDirectory.TAG_FLASH));
            builder.zoomLevel(getDoubleTag(exif, ExifSubIFDDirectory.TAG_DIGITAL_ZOOM_RATIO));
            builder.roll(getRoll(ifd0));
            builder.time(getDateTimeOriginal(exif));

            return builder.build();

        } catch (Exception e) {
            // TODO: SLF4J 기반으로 교체할 것
            e.printStackTrace();

        }

        return new MediaMetaData();
    }

    /**
     * method util
     */
    private static Integer getIntegerTag(Directory dir, int tag) {
        return (dir != null && dir.containsTag(tag)) ? dir.getInteger(tag) : null;
    }

    private static Double getDoubleTag(Directory dir, int tag) {
        return (dir != null && dir.containsTag(tag)) ? dir.getRational(tag).doubleValue() : null;
    }

    private static String getFormattedExposureTime(ExifSubIFDDirectory exif) {
        return (exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME))
                ? ExifRationalToStringConverter.formatExposureTime(exif.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME))
                : null;
    }

    private static String getFormattedAperture(ExifSubIFDDirectory exif) {
        return (exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_APERTURE))
                ? ExifRationalToStringConverter.formatAperture(exif.getRational(ExifSubIFDDirectory.TAG_APERTURE))
                : null;
    }

    private static String getFormattedExposureCompensation(ExifIFD0Directory ifd0) {
        return (ifd0 != null && ifd0.containsTag(ExifIFD0Directory.TAG_EXPOSURE_BIAS))
                ? ExifRationalToStringConverter.formatExposureCompensation(ifd0.getRational(ExifIFD0Directory.TAG_EXPOSURE_BIAS))
                : null;
    }

    private static WhiteBalance getWhiteBalance(ExifIFD0Directory ifd0) {
        return (ifd0 != null && ifd0.containsTag(ExifIFD0Directory.TAG_WHITE_BALANCE))
                ? WhiteBalance.fromCode(ifd0.getInteger(ExifIFD0Directory.TAG_WHITE_BALANCE))
                : null;
    }

    private static FlashMode getFlashMode(ExifSubIFDDirectory exif) {
        return (exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_FLASH))
                ? FlashMode.fromCode(exif.getInteger(ExifSubIFDDirectory.TAG_FLASH))
                : FlashMode.UNKNOWN;
    }

    private static String getRoll(ExifIFD0Directory ifd0) {
        return (ifd0 != null && ifd0.containsTag(ExifIFD0Directory.TAG_ORIENTATION))
                ? OrientationUtil.describeOrientation(ifd0.getInteger(ExifIFD0Directory.TAG_ORIENTATION))
                : null;
    }

    private static LocalDateTime getDateTimeOriginal(ExifSubIFDDirectory exif) {
        return (exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL))
                ? ExifDataTimeParser.parse(exif.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL))
                : null;
    }
}
