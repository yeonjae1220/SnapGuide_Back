package yeonjae.snapguide.entity.guide.mediaUtil.exifUtil;

public class OrientationUtil {
    public static String describeOrientation(int code) {
        return switch (code) {
            case 1 -> "Normal";
            case 3 -> "180";
            case 6 -> "90 CW";
            case 8 -> "270 CW";
            default -> "Unknown";
        };
    }
}
