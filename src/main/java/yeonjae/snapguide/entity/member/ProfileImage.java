package yeonjae.snapguide.entity.member;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import yeonjae.snapguide.entity.FileFormat;

@Embeddable
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ProfileImage {
    @Column(nullable = false)
    private String profileImageName;

    @Column(nullable = false)
    private String profileImageUrl;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private FileFormat profileImageType;

    public static ProfileImage defaultInstance() {
        // TODO : put default image url
        return ProfileImage.builder()
                .profileImageName("default")
                // .profileImageId(UUID.randomUUID().toString())
                .profileImageType(FileFormat.PNG)
                .build();
    }
}
