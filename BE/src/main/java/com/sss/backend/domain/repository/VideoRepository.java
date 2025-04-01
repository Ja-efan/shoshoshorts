package com.sss.backend.domain.repository;

import com.sss.backend.domain.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {
    Optional<Video> findByStoryId(Long storyId);

//    List<Video> findByStoryUser_Id(Long userId);
    List<Video> findByStory_User_Id(Long userId);
    /**
     * Spring Data JPA 의 메서드 파싱 규칙.
     * Video -> Story -> User -> Id 처럼 객체 그래프를 따라가는 방식
     *
     * findByStoryUser_Id : Video 클래스 안의 storyUser 를 뜻함.
     */

}
