package com.sss.backend.domain.repository;

import com.sss.backend.domain.document.SceneDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SceneRepository extends MongoRepository<SceneDocument, String> {
    Optional<SceneDocument> findByStoryId(String storyId);
}
