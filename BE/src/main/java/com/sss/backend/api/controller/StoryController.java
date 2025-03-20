package com.sss.backend.api.controller;

import com.sss.backend.api.dto.StoryRequestDTO;
import com.sss.backend.domain.entity.StoryEntity;
import com.sss.backend.domain.service.StoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/story")
public class StoryController {

    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping("/create")
    public StoryEntity createStory(@Valid @RequestBody StoryRequestDTO request) {
        System.out.println(request);
        return storyService.saveStory(request);
    }

}
