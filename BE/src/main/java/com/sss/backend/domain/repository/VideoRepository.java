package com.sss.backend.domain.repository;

import com.sss.backend.domain.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {
    Optional<Video> findByStoryId(Long storyId);

} 