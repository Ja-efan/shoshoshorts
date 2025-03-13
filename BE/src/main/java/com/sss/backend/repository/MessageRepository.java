package com.sss.backend.repository;

import com.sss.backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // 기본 CRUD 메서드는 JpaRepository에서 제공됩니다.
} 