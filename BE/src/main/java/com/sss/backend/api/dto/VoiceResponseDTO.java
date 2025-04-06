package com.sss.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VoiceResponseDTO {
    private Long id;
    private String title;
    private String description;

    @JsonProperty("voiceSampleUrl")
    private String voiceSampleUrl;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    @JsonProperty("updatedAt")
    private LocalDateTime  updatedAt;
}
