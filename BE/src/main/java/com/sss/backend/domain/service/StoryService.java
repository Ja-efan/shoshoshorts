package com.sss.backend.domain.service;

import com.sss.backend.api.dto.StoryRequestDTO;
import com.sss.backend.domain.entity.StoryEntity;
import com.sss.backend.infrastructure.repository.StoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StoryService {

    private final StoryRepository storyRepository;

    // 생성자 주입
    public StoryService(StoryRepository storyRepository) {
        this.storyRepository = storyRepository;
    }

    @Transactional
    public StoryEntity saveStory(StoryRequestDTO request) {
        // 유효성 검사 메서드 호출
        validateRequest(request);

        // DTO -> Entity 변환 (원본 그대로 저장)
        StoryEntity storyEntity = convertToEntity(request);
        return storyRepository.save(storyEntity);

    }

    // 유효성 검사 메서드
    private void validateRequest(StoryRequestDTO request){
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 비어 있을 수 없습니다.");
        }
        if (request.getStory() == null || request.getStory().trim().isEmpty()) {
            throw new IllegalArgumentException("스토리 내용은 비어 있을 수 없습니다.");
        }
        // 더 추가할 게 있을까요???
    }

    // DTO -> Entity 변환 메소드
    private StoryEntity convertToEntity(StoryRequestDTO request) {
        StoryEntity entity = new StoryEntity();
        entity.setTitle(request.getTitle());
        entity.setStory(request.getStory());
        return entity;
    }
}
