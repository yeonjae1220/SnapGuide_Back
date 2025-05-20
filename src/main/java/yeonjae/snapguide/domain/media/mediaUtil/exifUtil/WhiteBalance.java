package yeonjae.snapguide.domain.media.mediaUtil.exifUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum WhiteBalance {
    AUTO(0),
    MANUAL(1);

    private final int code;

    public static WhiteBalance fromCode(int code) {
        return Arrays.stream(values())
                .filter(wb -> wb.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid white balance code: " + code));
    }
}
