package yeonjae.snapguide.domain.media.mediaUtil.exifExtrator;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

@Component
public class ExifCoordinateExtractor {
    public static Optional<double[]> extractCoordinate(InputStream inputStream) { // 왜 double이고 Double가 아닌지
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null && gpsDir.getGeoLocation() != null) {
                GeoLocation loc = gpsDir.getGeoLocation();
                return Optional.of(new double[]{loc.getLatitude(), loc.getLongitude()});
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO : 로그로 바꿔야함
        }
        return Optional.empty();
    }
}
