package com.sss.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sss.backend.domain.entity.Video.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값은 JSON 응답에서 제외
public class VideoStatusResponseDto {
    private String storyId;
    private VideoStatus status;
    private String videoUrl;
    private String errorMessage;
    private String createdAt;
    private String completedAt;
    
    // 진행 중인 경우 초기 응답용 생성자 (storyId, status, createdAt 포함)
    public VideoStatusResponseDto(String storyId, VideoStatus status, String createdAt) {
        this.storyId = storyId;
        this.status = status;
        this.createdAt = createdAt;
    }
} 