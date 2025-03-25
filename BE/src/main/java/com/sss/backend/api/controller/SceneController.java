package com.sss.backend.api.controller;

import com.sss.backend.domain.document.SceneDocument;
import com.sss.backend.domain.repository.SceneRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mongo/")
public class SceneController {
    // 생성자 주입.
    private final SceneRepository sceneRepository;

    public SceneController(SceneRepository sceneRepository){
        this.sceneRepository = sceneRepository;
    }

    @GetMapping("/all")
    public List<SceneDocument> getAllScene() {
        return sceneRepository.findAll();
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<SceneDocument> getSceneByStoryId(@PathVariable String storyId){
        return sceneRepository.findByStoryId(storyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
