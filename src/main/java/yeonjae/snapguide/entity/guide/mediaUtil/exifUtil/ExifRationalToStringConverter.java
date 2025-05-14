package yeonjae.snapguide.entity.guide.mediaUtil.exifUtil;

import com.drew.lang.Rational;

public class ExifRationalToStringConverter {
    /**
     * Rational 객체를 "1/125" 형태의 문자열로 변환
     */
    public static String rationalToString(Rational rational) {
        if (rational == null) return null;
        return rational.getNumerator() + "/" + rational.getDenominator();
    }
    /**
     * Rational 객체를 소수(double)로 변환해서 문자열로 반환 (예: 0.008)
     */
    public static String rationalToDecimalString(Rational rational, int precision) {
        if (rational == null) return null;
        double value = rational.doubleValue();
        return String.format("%." + precision + "f", value);
    }

    /**
     * Rational → 노출시간(shutter speed) 표기 (예: "1/125s")
     * type : rational64s
     */
    public static String formatExposureTime(Rational rational) {
        if (rational == null) return null;
        return rational.getNumerator() + "/" + rational.getDenominator() + "s";
    }

    /**
     * Rational -> aperture(조리개 값) 표기 (예 : "f/1.6")
     * type : rational64u
     */
    public static String formatAperture(Rational rational) {
        if (rational == null) return null;
        return String.format("f/%.1f", rational.doubleValue());
    }

    /**
     * 소수값으로만 반환하고 싶을 때
     */
    public static String apertureToDecimal(Rational rational, int precision) {
        if (rational == null) return null;
        return String.format("%." + precision + "f", rational.doubleValue());
    }

    /**
     * Rational → 노출 보정 exposure bias 표기 (예: "+2 ~ -2EV")
     * type : rational64s
     * 소숫점 두번째 자리에서 반올림
     */
    public static String formatExposureCompensation(Rational rational) {
        if (rational == null) return null;
        return String.format("%.1fEV", rational.doubleValue());
    }

    /**
     * zoomlevel
     * DigitalZoomRatio
     * type : rational64u
     */


}
