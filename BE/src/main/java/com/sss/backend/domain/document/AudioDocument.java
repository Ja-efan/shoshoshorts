package com.sss.backend.domain.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "audios")
@CompoundIndex(name = "idx_story_scene_audio", def = "{'storyId': 1, 'sceneId': 1, 'audioId': 1}", unique = true)
public class AudioDocument {

    @Id
    private String id; // MongoDB 기본 ID

    private String storyId;
    private Integer sceneId;
    private Integer audioId;
    private Integer sequence;
    private String type;
    private String emotion;
    private String text;
    private String base_model;
    private String audio_settings;
    private String audio_url;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
}