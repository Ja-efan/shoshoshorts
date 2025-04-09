package com.sss.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoUploadDTO {

    @JsonProperty("storyId")
    private String storyId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("tags")
    private String tags;

    @JsonProperty("privacyStatus")
    private String privacyStatus;

    @JsonProperty("categoryId")
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

    @Override
    public String toString() {
        return "VideoUploadDTO{" +
                "storyId='" + storyId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", tags='" + tags + '\'' +
                ", privacyStatus='" + privacyStatus + '\'' +
                ", categoryId='" + categoryId + '\'' +
                '}';
    }
}
