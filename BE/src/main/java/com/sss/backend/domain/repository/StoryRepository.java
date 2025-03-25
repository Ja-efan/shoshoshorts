package com.sss.backend.domain.repository;


import com.sss.backend.domain.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<Story, Long> {
}
