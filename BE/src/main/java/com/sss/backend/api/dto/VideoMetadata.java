package com.sss.backend.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoMetadata {

    private String title;
    private String description;
    private String tags;
    private String privacyStatus;
    private String categoryId;

    public VideoMetadata(){

    }

    public VideoMetadata(String title, String description, String tags, String privacyStatus, String categoryId) {
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.privacyStatus = privacyStatus;
        this.categoryId = categoryId;
    }
}
