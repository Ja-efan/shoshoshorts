package com.sss.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "stories")
public class Story {
    @Id
    private String id;
    private String storyId;
    private String storyTitle;
    private List<Scene> sceneArr;
} 