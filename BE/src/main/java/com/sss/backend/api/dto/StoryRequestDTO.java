package com.sss.backend.api.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StoryRequestDTO {
    @NotBlank
    private String title;

    @NotBlank
    private String story;
}
