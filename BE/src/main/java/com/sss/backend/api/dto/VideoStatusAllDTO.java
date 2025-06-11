package com.sss.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sss.backend.domain.entity.Video;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VideoStatusAllDTO {
    private String title;
    private Video.VideoStatus status;
    private String completedAt;
    private String thumbnailUrl;
    private String videoUrl;
    private String storyId;
    private String processingStep;

    public VideoStatusAllDTO(String title, Video.VideoStatus status, String completedAt, 
                          String thumbnailUrl, String videoUrl, String storyId) {
        this.title = title;
        this.status = status;
        this.completedAt = completedAt;
        this.thumbnailUrl = thumbnailUrl;
        this.videoUrl = videoUrl;
        this.storyId = storyId;
    }
    
    public VideoStatusAllDTO(String title, Video.VideoStatus status, String completedAt, 
                          String thumbnailUrl, String videoUrl, String storyId, String processingStep) {
        this(title, status, completedAt, thumbnailUrl, videoUrl, storyId);
        this.processingStep = processingStep;
    }
}
