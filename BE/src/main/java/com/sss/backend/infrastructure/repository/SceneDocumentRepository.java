package com.sss.backend.infrastructure.repository;

import com.sss.backend.domain.document.SceneDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SceneDocumentRepository extends MongoRepository<SceneDocument, String> {

    // 스토리 ID로 씬 조회
    Optional<SceneDocument> findByStoryId(String storyId);
}
