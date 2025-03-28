package com.sss.backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;

    @Column(name = "user_name")
    private String userName;
    private String nickname;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String provider;

    @Column(name = "youtube_token")
    private String youtubeToken;

    @Column(name="instagram_token")
    private String instagramToken;
    
    @Column(name="tiktok_token")
    private String tiktokToken;

    private String role;

    // Java 규칙에서는 변수나 필드명을 camelCase로 쓰는 게 표준
    // 반면, DB컬럼명은 대부분 snake_case를 쓰는 게 관례임.

    public UserEntity(String email, String userName, String role){
        this.email = email;
        this.userName = userName;
        this.role = role;
    }



}
