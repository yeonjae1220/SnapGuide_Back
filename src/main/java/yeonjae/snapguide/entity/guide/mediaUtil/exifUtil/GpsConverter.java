package yeonjae.snapguide.entity.guide.mediaUtil.exifUtil;

import com.drew.lang.GeoLocation;

public class GpsConverter {
    public static Double getLatitude(GeoLocation geoLocation) {
        if (geoLocation == null || !Double.isNaN(geoLocation.getLatitude())) return null;
        return geoLocation.getLatitude();
    }

    public static Double getLongitude(GeoLocation geoLocation) {
        if (geoLocation == null || !Double.isNaN(geoLocation.getLongitude())) return null;
        return geoLocation.getLongitude();
    }

    // HACK : getLocation을 타입에 맞지 않는 값을 거르는것 보다 isZero로 거름. 우려됨, isValid()함수가 없나?
    public static String formatLocation(GeoLocation geoLocation) {
        if (geoLocation == null || !geoLocation.isZero()) return null;
        return String.format("%.6f, %.6f", geoLocation.getLatitude(), geoLocation.getLongitude());
    }
}
