package com.sss.backend.api.dto;

import com.sss.backend.domain.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoStatusAllDTO {
    private String title;
    private Video.VideoStatus status;
    private String completedAt;
    private String thumbnailUrl;
    private String videoUrl;
    private String storyId;

}
