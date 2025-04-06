package com.sss.backend.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoiceCreateDTO {
    private String voice_name;

    private String audio_file; // base64 문자열


}
