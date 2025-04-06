package com.sss.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoiceCreateDTO {

    @NotBlank(message = "보이스 이름은 필수입니다.")
    private String title;

    private String description;

    @NotBlank(message = "오디오 파일은 필수입니다.")
    @JsonProperty("audioBase64")
    private String audioBase64; // base64 문자열



}
