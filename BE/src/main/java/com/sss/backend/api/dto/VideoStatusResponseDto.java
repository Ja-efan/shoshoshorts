package com.sss.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sss.backend.domain.entity.Video.VideoStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값은 JSON 응답에서 제외
public class VideoStatusResponseDto {
    private String storyId;
    private VideoStatus status;
    private String videoUrl;
    private String errorMessage;
    private String createdAt;
    private String completedAt;
    private String processingStep; // 처리 단계에 대한 상세 정보
    private String thumbnailUrl;
    
    // 진행 중인 경우 초기 응답용 생성자 (storyId, status, createdAt 포함)
    public VideoStatusResponseDto(String storyId, VideoStatus status, String createdAt) {
        this.storyId = storyId;
        this.status = status;
        this.createdAt = createdAt;
    }
    
    // 처리 단계 포함 생성자
    public VideoStatusResponseDto(String storyId, VideoStatus status, String videoUrl, 
                                String errorMessage, String createdAt, String completedAt, String processingStep) {
        this.storyId = storyId;
        this.status = status;
        this.videoUrl = videoUrl;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.processingStep = processingStep;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}