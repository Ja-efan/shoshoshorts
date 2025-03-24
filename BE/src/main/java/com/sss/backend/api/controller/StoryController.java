package com.sss.backend.api.controller;

import com.sss.backend.api.dto.StoryRequestDTO;
import com.sss.backend.domain.entity.Story;
import com.sss.backend.domain.service.StoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/story")
public class StoryController {

    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createStory(@Valid @RequestBody StoryRequestDTO request) {

        System.out.println(request);

        CompletableFuture.runAsync(() -> {
            Long storyId = storyService.saveStory(request);
            System.out.println("스토리 생성 완료: " + storyId);

        });
        // 정현님..

        return ResponseEntity.ok("쇼츠 생성 시작합니다");
    }

}
