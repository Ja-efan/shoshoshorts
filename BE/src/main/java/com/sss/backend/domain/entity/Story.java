package com.sss.backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String story;

    @Column(nullable = false)
    private String audioModelName; // 오디오 AI 모델

    @Column(nullable = false)
    private String imageModelName; // 이미지 AI 모델

    @Column(nullable = false)
    private String narVoiceCode;

}
