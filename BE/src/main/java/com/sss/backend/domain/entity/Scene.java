package com.sss.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Scene {
    @Id
    private Integer sceneId;
    private String imageUrl;       // S3에 저장된 이미지 경로
    private List<Audio> audioArr;  // 여러 오디오 파일들의 경로

    // ID를 제외한 필드 초기화 생성자 추가
    public Scene(String imageUrl, List<Audio> audioArr) {
        this.imageUrl = imageUrl;
        this.audioArr = audioArr;
    }
} 