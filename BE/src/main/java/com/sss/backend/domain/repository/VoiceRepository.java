package com.sss.backend.domain.repository;

import com.sss.backend.domain.entity.Users;
import com.sss.backend.domain.entity.Voice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoiceRepository extends JpaRepository<Voice, Long> {

    // Users 객체를 기준으로 조회
    List<Voice> findAllByUser(Users user);

}
