package com.sss.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값은 JSON 응답에서 제외
public class VideoListResponseDTO {
    private List<VideoStatusAllDTO> data;
    // list 형태의 status 데이터를 "data": [] 형태로 감싸기 위한 DTO
}
