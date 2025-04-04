package com.sss.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponse {
    @JsonProperty("accessToken") // 프론트 설정에 맞췄습니다.
    private String accessToken;
//    private String refreshToken;
}
