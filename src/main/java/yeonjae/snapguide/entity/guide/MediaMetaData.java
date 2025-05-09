package yeonjae.snapguide.entity.guide;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Embeddable
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private Long iso;

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
     * TODO : enum 으로 바꿔 놓기 Auto / Manual
     */
    private String whiteBalance;

    /**
     * 초점거리
     * 26mm
     */
    private String focalLength;

    /**
     * 밝기 조정 (+2 ~ -2EV) 보통 슬라이더 지원
     */
    private String exposureCompensation;

    /**
     * 플레쉬 사용 여부
     *  TODO : enum 으로 바꿔놓기 ON / OFF
     */
    private String flash;

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
    private String aspectRatio;

    /**
     * 해상도
     */
    private String resolution;

    /**
     * 비디오 전용
     * 30fps
     */
    private String frameRate;

    private LocalDateTime time;

    /**
     * HACK
     * enum으로?
     * 현재 위치 기반 날씨 api 가져 오거나 휴대폰 날씨 앱에서 뽑아오기
     * 개인이 추가
     */
    // private Weather weather;



}
