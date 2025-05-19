package yeonjae.snapguide.entity.guide;

import jakarta.persistence.*;
import lombok.*;
import yeonjae.snapguide.entity.member.Member;
import yeonjae.snapguide.infrastructure.persistence.jpa.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "tip"})
public class Guide extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000) // 최대 길이 지정
    private String tip;

    // private Long voteCount;
    // private Long viewCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member author;

    // FIXME : 셀피같은 경우 등을 고려해 비공개 선택이나 나라만 나타낼 수 있게 변경
    // NOTE : 대략적으로만 나타내도 가능, null로 뺼지, 아니면 저장은 하되, 프론트에 DTO로 빼고 보낼지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;


    // NOTE : 화면에서 Guide 객체 하나로 그 안에 있는 Photo 목록을 자주 순회 할 듯 하여 양방향 설정
    // TODO : 관계 유지 코드 (동기화) 작성 필요
    @OneToMany(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default //@Builder는 필드 초기화를 무시하므로, @Builder.Default가 없으면 null이 들어감
    private List<Media> mediaList = new ArrayList<>();

    // 일단 고려
//    // NOTE : 댓글이 많을 경우 N+1이나 지연 로딩 이슈가 발생할 수 있지만, 앱 특성상 댓글이 거의 없을 것이라 생각하여 양방향 설정
//    // TODO : 관계 유지 코드 (동기화) 작성 필요
//    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Comment> commentList = new ArrayList<>();

    public void assignGuide(Location location) {
        this.location = location;
    }

    public void assignGuide(Media media) {
        if (this.mediaList == null) {
            this.mediaList = new ArrayList<>();
        }
        this.mediaList.add(media); // 이것도 Null 때문에 방어 코드 추가
        /**
        * null 방어 코드를 추가하면, 추후 다른 코드에서도 안전하게 사용할 수 있습니다. 다만 이건 임시 해결일 뿐, 엔티티 자체를 null-safe하게 만드는 게 최선입니다.
         */
    }
}
