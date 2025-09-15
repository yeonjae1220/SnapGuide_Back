package yeonjae.snapguide.service.fileStorageService;

import org.apache.tika.Tika;

import java.io.IOException;
import java.io.InputStream;

public class FileTypeDetector {
    private static final Tika tika = new Tika();

    public static String detectMimeType(InputStream inputStream) throws IOException {
        return tika.detect(inputStream);
    }
}
