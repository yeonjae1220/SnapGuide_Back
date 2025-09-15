package yeonjae.snapguide.domain.media.mediaUtil.exifExtrator;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.springframework.stereotype.Component;
import yeonjae.snapguide.domain.cameraModel.CameraModel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

@Component
public class CameraModelExtractor {
    public static CameraModel extract(byte[] imageBytes) {
        CameraModel model = new CameraModel();

        try{
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            return CameraModel.builder()
                    .manufacturer(ifd0 != null && ifd0.containsTag(ExifIFD0Directory.TAG_MAKE)
                            ? ifd0.getString(ExifIFD0Directory.TAG_MAKE) : null)
                    .model(ifd0 != null && ifd0.containsTag(ExifIFD0Directory.TAG_MODEL)
                            ? ifd0.getString(ExifIFD0Directory.TAG_MODEL) : null)
                    .lens(exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_LENS_MODEL)
                            ? exif.getString(ExifSubIFDDirectory.TAG_LENS_MODEL) : null)
                    .build();


        } catch (Exception e) {
            // TODO : log 처리로 바꿔야 함
            e.printStackTrace();
        }
        return model;
    }
}
