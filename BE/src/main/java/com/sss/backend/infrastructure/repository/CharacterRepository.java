package com.sss.backend.infrastructure.repository;

import com.sss.backend.domain.document.CharacterDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface CharacterRepository extends MongoRepository<CharacterDocument, String> {
    // MongoDB에서 storyId 기준으로 characterArr 조회
    @Query(value = "{'storyId': ?0}", sort = "{'_id': -1}") // 최신 문서 하나만 반환
    Optional<CharacterDocument> findByStoryId(String storyId);
    // 왜 Optional이 안전하게 조회하는거지?
    // Optional 없이 짠 코드는 null을 반환하여 npe 발생가능.
}
