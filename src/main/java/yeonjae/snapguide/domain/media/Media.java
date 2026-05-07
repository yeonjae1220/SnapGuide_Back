package yeonjae.snapguide.domain.media;

import jakarta.persistence.*;
import lombok.*;
import yeonjae.snapguide.domain.guide.Guide;
import yeonjae.snapguide.domain.mediaMetaData.MediaMetaData;
import yeonjae.snapguide.domain.location.Location;
import yeonjae.snapguide.domain.media.ProcessingStatus;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mediaName;

    @Column(nullable = false)
    private String mediaUrl;

    private String originalKey;
    private String webKey;
    private String thumbnailKey;

//    @Enumerated(value = EnumType.STRING)
//    @Column(nullable = false)
//    private MediaType mediaType;
//
//    @Enumerated(value = EnumType.STRING)
//    @Column(nullable = false)
//    private MediaSubType mediaSubType;
//
//    @Enumerated(value = EnumType.STRING)
//    @Column(nullable = false)
//    private FileFormat fileFormat;

    @Column(nullable = false)
    private Long fileSize; // bytes

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'PENDING'")
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "member_id", nullable = false)
//    private Member member;
//
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = true) // HACK : Guide보다 media를 빨리 저장해야해서 얘가 Nullable이 들어감
    private Guide guide;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_metadata_id", nullable = false)
    private MediaMetaData mediaMetaData;

    // NOTE : 대략적으로만 나타내도 가능, 디테일한 주소를 따로 빼고 ManytoOne으로 바꾼 다음 하면 최적화 가능 할 듯
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST) // PERSIST: 새 Location일 경우 자동 저장
    @JoinColumn(name = "location_id")
    private Location location;

    public void assignMedia(MediaMetaData mediaMetaData, Location location) {
        this.mediaMetaData = mediaMetaData;
        this.location = location;
    }

    public void assignMedia(Guide guide) {
        this.guide = guide;
    }

    /**
     * 비동기 파생 파일 생성 완료 (S3: webKey+thumbnailKey, Local: thumbnailKey만)
     */
    public void markProcessingCompleted(String webKey, String thumbnailKey, String mediaUrl) {
        this.webKey = webKey;
        this.thumbnailKey = thumbnailKey;
        this.mediaUrl = mediaUrl;
        this.processingStatus = ProcessingStatus.COMPLETED;
    }

    /**
     * 비동기 파생 파일 생성 실패 — webKey/thumbnailKey는 null 유지
     */
    public void markProcessingFailed() {
        this.processingStatus = ProcessingStatus.FAILED;
    }
}
