package com.sss.backend.infrastructure.repository;


import com.sss.backend.domain.entity.StoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<StoryEntity, Long> {
}
