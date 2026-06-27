package yeonjae.snapguide.controller.guideController.guideDto;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.domain.media.MediaExifDto;
import yeonjae.snapguide.domain.member.dto.MemberDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuideResponseDtoTest {

    @Test
    void deserializesFromNearbyGuidesRedisCachePayload() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JavaType javaType = objectMapper.getTypeFactory()
                .constructParametricType(List.class, GuideResponseDto.class);
        Jackson2JsonRedisSerializer<List<GuideResponseDto>> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, javaType);

        GuideResponseDto guide = GuideResponseDto.builder()
                .id(12L)
                .tip("multi pic test1")
                .author(MemberDto.builder()
                        .id(3L)
                        .nickname("김연재")
                        .build())
                .locationName("Arakawa, Arakawa City")
                .latitude(35.74254722222222)
                .longitude(139.78240833333334)
                .locationPublic(true)
                .media(List.of(new MediaDto(
                        "IMG_6842.jpeg",
                        "/media/files/27d8c793-6a87-44fa-b616-37b5db6f0654.jpg",
                        MediaExifDto.builder()
                                .model("iPhone 12 mini")
                                .iso(500)
                                .shutterSpeed("1/33s")
                                .aperture("f/1.4")
                                .time(LocalDateTime.of(2026, 4, 28, 11, 34, 47))
                                .build())))
                .likeCount(0)
                .userHasLiked(false)
                .build();

        byte[] bytes = serializer.serialize(List.of(guide));
        List<GuideResponseDto> restored = serializer.deserialize(bytes);

        assertThat(restored).hasSize(1);
        GuideResponseDto restoredGuide = restored.get(0);
        assertThat(restoredGuide.getId()).isEqualTo(12L);
        assertThat(restoredGuide.getAuthor().getNickname()).isEqualTo("김연재");
        assertThat(restoredGuide.getMedia()).hasSize(1);
        assertThat(restoredGuide.getMedia().get(0).getExif().getModel()).isEqualTo("iPhone 12 mini");
    }
}
