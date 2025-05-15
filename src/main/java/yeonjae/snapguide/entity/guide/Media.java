package yeonjae.snapguide.entity.guide;

import jakarta.persistence.*;
import lombok.*;
import yeonjae.snapguide.entity.FileFormat;
import yeonjae.snapguide.entity.guide.mediaUtil.MediaSubType;
import yeonjae.snapguide.entity.guide.mediaUtil.MediaType;
import yeonjae.snapguide.entity.member.Member;

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

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "member_id", nullable = false)
//    private Member member;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "guide_id", nullable = false)
//    private Guide guide;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_metadata_id", nullable = false)
    private MediaMetaData mediaMetaData;

    // NOTE : 대략적으로만 나타내도 가능, 디테일한 주소를 따로 빼고 ManytoOne으로 바꾼 다음 하면 최적화 가능 할 듯
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST) // PERSIST: 새 Location일 경우 자동 저장
    @JoinColumn(name = "location_id")
    private Location location;

    public void assignMedia(MediaMetaData mediaMetaData) {
        this.mediaMetaData = mediaMetaData;
    }
}
