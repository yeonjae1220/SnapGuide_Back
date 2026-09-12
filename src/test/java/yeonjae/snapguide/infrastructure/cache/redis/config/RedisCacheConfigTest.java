package yeonjae.snapguide.infrastructure.cache.redis.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import yeonjae.snapguide.controller.guideController.guideDto.GuideResponseDto;
import yeonjae.snapguide.domain.media.MediaDto;
import yeonjae.snapguide.domain.media.MediaExifDto;
import yeonjae.snapguide.domain.member.dto.MemberDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCacheConfigTest {

    @Test
    @DisplayName("nearbyGuides 캐시 값은 GuideResponseDto 리스트로 역직렬화된다")
    void shouldDeserializeNearbyGuidesAsGuideResponseDtoList() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JavaType javaType = objectMapper.getTypeFactory()
                .constructParametricType(List.class, GuideResponseDto.class);
        RedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, javaType);

        GuideResponseDto guide = GuideResponseDto.builder()
                .id(1L)
                .tip("Nearby tip")
                .author(MemberDto.builder().id(10L).email("author@example.com").build())
                .locationName("Changwon")
                .latitude(35.183)
                .longitude(128.73)
                .locationPublic(true)
                .media(List.of(new MediaDto(
                        "photo.jpg",
                        "/uploads/photo.jpg",
                        MediaExifDto.builder()
                                .model("X100")
                                .time(LocalDateTime.of(2026, 6, 8, 12, 30))
                                .build())))
                .likeCount(3)
                .userHasLiked(false)
                .build();

        byte[] serialized = serializer.serialize(List.of(guide));

        Object deserialized = serializer.deserialize(serialized);

        assertThat(deserialized).isInstanceOf(List.class);
        List<?> restored = (List<?>) deserialized;
        assertThat(restored).hasSize(1);
        assertThat(restored.get(0)).isInstanceOf(GuideResponseDto.class);

        GuideResponseDto restoredGuide = (GuideResponseDto) restored.get(0);
        assertThat(restoredGuide.getTip()).isEqualTo("Nearby tip");
        assertThat(restoredGuide.getAuthor().getEmail()).isEqualTo("author@example.com");
        assertThat(restoredGuide.getMedia()).hasSize(1);
        assertThat(restoredGuide.getMedia().get(0).getExif().getTime())
                .isEqualTo(LocalDateTime.of(2026, 6, 8, 12, 30));
    }
}
