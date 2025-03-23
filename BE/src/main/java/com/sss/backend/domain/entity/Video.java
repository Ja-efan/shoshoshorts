package com.sss.backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false, unique = true)
    private Story story;

    @Column(nullable = true)
    private String video_url;

    @Column(nullable = true)
    private String youtube_url;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VideoStatus status = VideoStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    public enum VideoStatus {
        PENDING,       // 대기 중
        PROCESSING,    // 처리 중
        COMPLETED,     // 완료됨
        FAILED         // 실패
    } 
}
