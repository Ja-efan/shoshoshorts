package com.sss.backend.api.controller;

import com.sss.backend.api.dto.VideoResponseDto;
import com.sss.backend.domain.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoService videoService;
    
    @Value("${temp.directory}")
    private String tempDirectory;

    @PostMapping("/generate-sync")
    public ResponseEntity<VideoResponseDto> generateVideoSync(){
        // TODO: 입력 데이터 json 파싱
        /** Request Body
         * title	String	필수	쇼츠 제목 (최대 n자)
         * story	String	필수	쇼츠 스크립트 생성을 위한 스토리 (최대 3000자)
         * characters	Array	선택	등장인물 목록.
         */

        // TODO: 이미지, 음성 생성

        // 비디오 생성 및 S3 업로드
        String outputPath = tempDirectory + "/" + UUID.randomUUID() + "_final.mp4";  // 임시 출력 파일 경로
        String videoUrl = videoService.createAndUploadVideo(storyId, outputPath);

        VideoResponseDto response = new VideoResponseDto(storyId, videoUrl);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{storyId}")
    public ResponseEntity<VideoResponseDto> generateVideo(@PathVariable String storyId) {
        // 임시 출력 파일 경로 생성
        String outputPath = tempDirectory + "/" + UUID.randomUUID() + "_final.mp4";
        
        // 비디오 생성 및 S3 업로드
        String videoUrl = videoService.createAndUploadVideo(storyId, outputPath);
        
        // DTO 응답 생성
        VideoResponseDto response = new VideoResponseDto(storyId, videoUrl);
        
        return ResponseEntity.ok(response);
    }
} 