package yeonjae.snapguide.entity.guide.mediaUtil.exifUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * https://exiftool.org/TagNames/EXIF.html#Flash
 * 페이지 참고
 */
@Getter
public enum FlashMode {
    NO_FLASH(0x0,"Flash did not fire"),
    FLASH_FIRED(0x1,"Flash fired"),
    FLASH_FIRED_RETURN_NOT_DETECTED(0x5,"Flash fired, return not detected"),
    FLASH_FIRED_RETURN_DETECTED(0x7,"Flash fired, return detected"),
    AUTO_NO_FLASH(0x8,"Auto mode, flash did not fire"),
    AUTO_FLASH_FIRED(0x9,"Auto mode, flash fired"),
    NO_FLASH_FUNCTION(0x10,"No flash function"),
    UNKNOWN(-1, "Unknown flash mode");

    private final int code;
    private final String description;

    FlashMode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public boolean isFlashNotFired() {
        return this == NO_FLASH || this == AUTO_NO_FLASH;
    }

    public static FlashMode fromCode(int code) {
        return Arrays.stream(values())
                .filter(mode -> mode.code == code)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
