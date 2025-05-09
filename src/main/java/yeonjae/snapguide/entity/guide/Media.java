package yeonjae.snapguide.entity.guide;

import jakarta.persistence.*;
import lombok.*;
import yeonjae.snapguide.entity.FileFormat;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mediaName;

    @Column(nullable = false)
    private String mediaUrl;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private MediaSubType mediaSubType;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private FileFormat fileFormat;

    @Column(nullable = false)
    private Long fileSize; // bytes

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide;

    // NOTE : 대략적으로만 나타내도 가능
    @OneToOne
    @JoinColumn(name = "local_id")
    private Location location;
}
