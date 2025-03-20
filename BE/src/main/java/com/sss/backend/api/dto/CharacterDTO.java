package com.sss.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CharacterDTO {
    @NotBlank
    private String name;

    @NotBlank
    private Integer gender;

    private String properties;
}
