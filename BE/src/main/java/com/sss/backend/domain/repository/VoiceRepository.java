package com.sss.backend.domain.repository;

import com.sss.backend.domain.entity.Users;
import com.sss.backend.domain.entity.Voice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface VoiceRepository extends JpaRepository<Voice, Long> {

    // Users 객체를 기준으로 조회
    List<Voice> findAllByUser(Users user);

    // 임베딩 벡터 값 가져오기
    @Query("SELECT v.embeddingTensor FROM Voice v WHERE v.id = :voiceId")
    @Transactional
    byte[] findEmbeddingTensorById(@Param("voiceId") Long voiceId);
}
