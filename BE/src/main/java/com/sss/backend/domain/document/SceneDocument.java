package com.sss.backend.domain.document;

import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

//@Data
@Getter
@Setter
@Document(collection = "scenes")
public class SceneDocument {

    @Id
    private String id; // mongoDB 기본 id 필드

    private String storyId;
    private String storyTitle;
    private String narVoiceCode;
    private String audioModelName;
    private String imageModelName;
    private List<Map<String,Object>> characterArr;
    private List<Map<String,Object>> sceneArr;
}
