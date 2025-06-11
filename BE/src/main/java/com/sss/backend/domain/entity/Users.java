package com.sss.backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // Unique 추가
    private String email;

    private String password;

    @Column(name = "user_name")
    private String userName;    // 유저명
    private String nickname;    // 닉네임

    @Column(name = "phone_number")
    private String phoneNumber;

    private String provider; // Kakao, naver,google

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Voice> voice = new ArrayList<>(); // 초기화 : NPE(nullpointException) 방지.

    @Column(name = "youtube_token")
    private String youtubeToken;

    @Column(name="instagram_token")
    private String instagramToken;
    
    @Column(name="tiktok_token")
    private String tiktokToken;

    private String role;

    // Java 규칙에서는 변수나 필드명을 camelCase로 쓰는 게 표준
    // 반면, DB컬럼명은 대부분 snake_case를 쓰는 게 관례임.

    public Users(String email, String userName, String role, String provider){
        this.email = email;
        this.userName = userName;
        this.role = role;
        this.provider = provider;
    }



}
