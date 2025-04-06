package com.sss.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VoiceResponseDTO {
    private Long id;
    private String voiceName;
    private String voiceSampleUrl;
}
