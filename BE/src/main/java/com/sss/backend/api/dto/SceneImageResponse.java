package com.sss.backend.api.dto;

import lombok.Data;

@Data
public class SceneImageResponse {

    private Integer scene_id;
    private String image_prompt;
    private String image_url;

}
