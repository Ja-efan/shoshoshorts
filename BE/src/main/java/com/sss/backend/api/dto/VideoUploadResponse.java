package com.sss.backend.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoUploadResponse {

    private String message;
    private String videoId;
    private String videoUrl;


    public VideoUploadResponse(){


    }

    public VideoUploadResponse(String message, String videoId, String videoUrl) {
        this.message = message;
        this.videoId = videoId;
        this.videoUrl = videoUrl;
    }
}
