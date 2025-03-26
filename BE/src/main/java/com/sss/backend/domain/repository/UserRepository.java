package com.sss.backend.domain.repository;

import com.sss.backend.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

//    UserEntity findByUsername(String username);
    Optional<UserEntity> findByUserName(String userName);
    // UserEntity find... 를 하면 값이 없을 때 null이 리턴 됨.
    // 이걸 Service나 Controller에서 사용할 때 NullPointerException이 날 수 있음.
}
