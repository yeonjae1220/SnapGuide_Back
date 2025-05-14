package yeonjae.snapguide.entity.guide.mediaUtil.exifUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExifDateTimeConverter {
    private static final SimpleDateFormat exifFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    public static Date parseExifDate(String exifDateStr) {
        if (exifDateStr == null) return null;
        try {
            return exifFormat.parse(exifDateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String formatToDisplay(Date date) {
        if (date == null) return null;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
