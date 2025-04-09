package com.sss.backend.domain.repository;


import com.sss.backend.domain.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StoryRepository extends JpaRepository<Story, Long> {


    @Query("SELECT s.story FROM Story s WHERE s.id = :id")
    String findStoryById(Long id);

    @Query("SELECT s.title FROM Story s WHERE s.id = :id")
    String findTitleById(Long id);


}
