package com.sss.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoiceCreateDTO {

    @NotBlank(message = "보이스 이름은 필수입니다.")
    private String voice_name;

    @NotBlank(message = "오디오 파일은 필수입니다.")
    private String audio_file; // base64 문자열


}
