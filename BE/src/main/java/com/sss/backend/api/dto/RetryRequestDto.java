package com.sss.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetryRequestDto {
    @JsonProperty("StoryId")
    private Long StoryId;
}
