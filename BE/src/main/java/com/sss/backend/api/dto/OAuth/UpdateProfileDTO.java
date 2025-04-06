package com.sss.backend.api.dto.OAuth;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileDTO {

    @Size(min=2, max=15, message = "닉네임은 2~15자 사이여야 합니다.")
    private String nickname;

    @Size()
    private String phoneNumber;


    // 사용하려나?
    private String youtubeToken;
    private String instagramToken;
    private String tiktokToken;}

