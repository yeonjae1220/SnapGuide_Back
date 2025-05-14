package yeonjae.snapguide.entity.guide;

import jakarta.persistence.*;
import lombok.*;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.FlashMode;
import yeonjae.snapguide.entity.guide.mediaUtil.exifUtil.WhiteBalance;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MediaMetaData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camera_model_id", nullable = false)
    private CameraModel cameraModel;

    // HACK : 추후 중복되는 데이터가 많으면 정규화 할 예정
    // HACK : 숫자 타입들 Long 으로 저장?
    /**
     * 빛에 대한 감도
     * 50, 100, 150 ..
     */
    private Integer iso;

    /**
     * exposure time
     * 1/1000s
     */
    private String shutterSpeed;

    /**
     * 조리개 값 (밝기 조절)
     * f/1.6
     */
    private String aperture;

    /**
     * 자동 수동
     * */
    @Enumerated(EnumType.STRING)
    private WhiteBalance whiteBalance;

    /**
     * 초점거리
     * 26mm
     */
    private Integer focalLength;

    /**
     * 밝기 조정 (노출 보정) (+2 ~ -2EV) 보통 슬라이더 지원
     */
    private String exposureCompensation;

    /**
     * 플레쉬 사용 여부
     *  TODO : enum 으로 바꿔놓기 ON / OFF
     */
    @Enumerated(EnumType.STRING)
    private FlashMode flashMode;
    private Integer flashCode;  // 0x7 같은 원시값 저장

    /**
     * 줌
     * 0.x ~ 100
     */
    private Double zoomLevel;

    /**
     * 회전 방향
     * Rotate 90 CW or 한쪽 방향으로 정수형으로?
     */
    private String roll;

    /**
     * 비율
     */
    // private String aspectRatio;

    /**
     * 해상도
     */
    // private String resolution;

    /**
     * 비디오 전용
     * 30fps
     */
    // private String frameRate;

    private LocalDateTime time;

    /**
     * HACK
     * enum으로?
     * 현재 위치 기반 날씨 api 가져 오거나 휴대폰 날씨 앱에서 뽑아오기
     * 개인이 추가
     */
    // private Weather weather;

    public void assignCameraModel(CameraModel cameraModel) {
        this.cameraModel = cameraModel;
    }

}
