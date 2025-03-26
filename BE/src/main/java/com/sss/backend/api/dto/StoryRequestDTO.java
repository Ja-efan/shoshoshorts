package com.sss.backend.api.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 변환 시 제외
public class StoryRequestDTO {
    @NotBlank
    private String title;

    @NotBlank
    private String story;

    @JsonProperty("narVoiceCode")
    private String narVoiceCode;

    // MongoDB에 저장할 데이터 추가
    @JsonProperty("characterArr") // JSON 키와 Java 필드 매핑
    private List<Map<String, Object>> characterArr = new ArrayList<>();

}
/**
 * @Data는 다음과 같은 기능을 자동으로 생성해줌.
 * Getter, Setter, Tostring, EqualsAndHashCode,
 * RequiredArgsConstructor
 *
 */