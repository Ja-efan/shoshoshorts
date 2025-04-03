package com.sss.backend.api.dto.OAuth;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UserInfoDTO {
    private String email;
    private String nickname;
    private String phoneNumber;

    // 보이스 라이브러리
//    private List<VoiceDTO> voices;
}
