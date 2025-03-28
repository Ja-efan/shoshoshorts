package com.sss.backend.api.controller;

import com.sss.backend.api.dto.VideoMetadata;
import com.sss.backend.api.dto.VideoUploadResponse;
import com.sss.backend.domain.service.YoutubeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/youtube")
public class YoutubeController {


    private final YoutubeService youtubeService;

    public YoutubeController(YoutubeService youtubeService){
        this.youtubeService = youtubeService;
    }


    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("videoURL") String videoUrl,
            @RequestParam(value = "title", required = false, defaultValue = "Untitled") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false, defaultValue = "shorts") String tags,
            @RequestParam(value = "privacyStatus", required = false, defaultValue = "private") String privacyStatus,
            @RequestParam(value = "categoryId", required = false, defaultValue = "23") String categoryId,
            @RequestHeader("Authorization") String authorizationHeader) {
        //요청 헤더에서 인증 토큰 추출

        // URL이 비어있는지 확인
        if (videoUrl == null || videoUrl.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "비디오 URL이 누락되었습니다");
            return ResponseEntity.badRequest().body(response);
        }

        String finalDescription = (description != null) ? description+ " #Shorts" : "#Shorts";



        try {
            // Authorization 헤더에서 토큰 추출 (Bearer 토큰 형식 가정)
            String accessToken = authorizationHeader.replace("Bearer ", "");

            // 메타데이터 준비
            VideoMetadata metadata = new VideoMetadata();
            metadata.setTitle(title);
            metadata.setDescription(finalDescription);
            metadata.setTags(tags);
            metadata.setPrivacyStatus(privacyStatus);
            metadata.setCategoryId(categoryId);


            // 비디오 업로드 (비동기 메서드를 블로킹 호출)
            VideoUploadResponse response = youtubeService.uploadVideo(accessToken,videoUrl,metadata).block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }




}
