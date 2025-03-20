package com.sss.backend.domain.repository;

import com.sss.backend.domain.entity.Story;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryRepository extends MongoRepository<Story, String> {
    Story findByStoryId(String storyId);
} 