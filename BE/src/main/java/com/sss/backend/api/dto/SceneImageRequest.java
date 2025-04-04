package com.sss.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SceneImageRequest {

    @JsonProperty("story_metadata")
    private StoryMetadata storyMetadata;

    @JsonProperty("scene_id")
    private Integer sceneId;

    private List<Audio> audios;

    private String apiPwd;

    @Data
    public static class StoryMetadata {

        @JsonProperty("story_id")
        private Integer story_id;
        private String title;
        private List<Character> characters;
//        private String original_story;
    }

    @Data
    public static class Character {
        private String name;
        private Integer gender;  // 1=남성, 2=여성 등
        private String description;
    }

    @Data
    public static class Audio {
        private String type;  // dialogue, narration, sound 등
        private String character;  // type이 dialogue인 경우만 필수
        private String text;
        private String emotion;
    }

    public String getApiPwd() {
        return apiPwd;
    }

    public void setApiPwd(String apiPwd) {
        this.apiPwd = apiPwd;
    }
}
