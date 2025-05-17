package yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
public class ExifCoordinateExtractor {
    public static Optional<double[]> extractCoordinate(File file) { // 왜 double이고 Double가 아닌지
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null && gpsDir.getGeoLocation() != null) {
                GeoLocation loc = gpsDir.getGeoLocation();
                return Optional.of(new double[]{loc.getLatitude(), loc.getLongitude()});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
