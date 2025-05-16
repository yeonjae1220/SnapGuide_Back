package yeonjae.snapguide.service.util.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeometryDto {
    private LocationPointDto location;
    // private String location_type;
}

/**
 * 위 주석 처리한 필드를 없애고 ignore 애노테이션이 없을 때
 * Jackson이 JSON을 Java 객체로 역직렬화(deserialize)할 때, JSON에 포함된 필드가 Java 클래스에 정의되어 있지 않아서 발생합니다. 구체적으로는:
 * 🔴 오류 요약
 * 	•	GeometryDto 클래스에는 "location_type"이라는 필드가 정의되어 있지 않음.
 * 	•	하지만 실제 응답 JSON에 "location_type" 필드가 포함되어 있음.
 * 	•	Jackson은 이를 인식하지 못하고 예외(UnrecognizedPropertyException)를 발생시킴.
 */