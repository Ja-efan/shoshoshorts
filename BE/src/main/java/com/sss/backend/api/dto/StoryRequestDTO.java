package com.sss.backend.api.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StoryRequestDTO {
    @NotBlank
    private String title;

    @NotBlank
    private String story;

    // MongoDB에 저장할 데이터 추가
    private List<Map<String, Object>> characterArr;

}
