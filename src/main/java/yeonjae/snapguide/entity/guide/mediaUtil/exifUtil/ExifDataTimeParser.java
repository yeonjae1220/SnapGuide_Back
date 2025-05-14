package yeonjae.snapguide.entity.guide.mediaUtil.exifUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExifDataTimeParser {
    public static LocalDateTime parse(String dateStr) {
        if (dateStr == null) return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            return null;
        }
    }
}
