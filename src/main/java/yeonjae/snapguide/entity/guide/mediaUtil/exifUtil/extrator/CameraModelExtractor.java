package yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.extrator;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import yeonjae.snapguide.entity.guide.CameraModel;

import java.io.File;

public class CameraModelExtractor {
    public static CameraModel extract(File file) {
        CameraModel model = new CameraModel();

        try{
            Metadata metadata = ImageMetadataReader.readMetadata(file);
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
