package com.sss.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

//@Data
@Getter
@NoArgsConstructor // 기본 생성자 추가
@AllArgsConstructor // 모든 필드를 매개변수로 받는 생성자 생성.
@ToString
public class CharacterDTO {
    @NotBlank
    private String name;

    @NotBlank
    private Integer gender;


    private String properties;
}
