package yeonjae.snapguide.controller.guideController.guideDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Slice;

import java.util.List;

/**
 * 커서 기반 페이징 응답 DTO
 * @param <T> 컨텐츠 타입
 */
@Data
@AllArgsConstructor
@Builder
public class SliceResponse<T> {
    /**
     * 현재 페이지의 컨텐츠 목록
     */
    private List<T> content;

    /**
     * 다음 페이지 존재 여부
     */
    private boolean hasNext;

    /**
     * 다음 페이지를 요청할 때 사용할 커서 (마지막 아이템의 ID)
     * hasNext가 false면 null
     */
    private Long nextCursor;

    /**
     * 현재 페이지의 아이템 개수
     */
    private int size;

    /**
     * 첫 페이지 여부
     */
    private boolean first;

    /**
     * Spring Data Slice를 SliceResponse로 변환하는 정적 팩토리 메서드
     * @param slice Spring Data Slice
     * @param getCursor 커서 추출 함수 (예: GuideResponseDto::getId)
     * @param <T> 컨텐츠 타입
     * @return SliceResponse
     */
    public static <T> SliceResponse<T> from(Slice<T> slice, java.util.function.Function<T, Long> getCursor) {
        List<T> content = slice.getContent();
        Long nextCursor = slice.hasNext() && !content.isEmpty()
                ? getCursor.apply(content.get(content.size() - 1))
                : null;

        return SliceResponse.<T>builder()
                .content(content)
                .hasNext(slice.hasNext())
                .nextCursor(nextCursor)
                .size(content.size())
                .first(slice.isFirst())
                .build();
    }
}
