package com.sss.backend.api.dto.OAuth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sss.backend.api.dto.VoiceResponseDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class UserInfoDTO {
    private String name;
    private String email;
    private Integer token;
    // 보이스 라이브러리
    @JsonProperty("speakerLibrary")
    private List<VoiceResponseDTO> speakerLibrary;
}
