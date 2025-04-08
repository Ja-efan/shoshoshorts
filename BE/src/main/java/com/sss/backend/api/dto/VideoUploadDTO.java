package com.sss.backend.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoUploadDTO {

    private String storyId;
    private String title;
    private String description;
    private String tags;
    private String privacyStatus;
    private String categoryId;

    public VideoUploadDTO() {
    }

    public VideoUploadDTO(String storyId, String title, String description, String tags, String privacyStatus, String categoryId) {
        this.storyId = storyId;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.privacyStatus = privacyStatus;
        this.categoryId = categoryId;
    }

}
