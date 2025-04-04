package com.sss.backend.api.controller;

import com.sss.backend.api.dto.VideoMetadata;
import com.sss.backend.api.dto.VideoUploadResponse;
import com.sss.backend.domain.service.YoutubeAuthService;
import com.sss.backend.domain.service.YoutubeService;
import org.springframework.http.HttpStatus;
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
    private final YoutubeAuthService youtubeAuthService;

    public YoutubeController(YoutubeService youtubeService, YoutubeAuthService youtubeAuthService){
        this.youtubeService = youtubeService;
        this.youtubeAuthService = youtubeAuthService;
    }


    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("videoURL") String videoUrl,
            @RequestParam(value = "title", required = false, defaultValue = "Untitled") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false, defaultValue = "shorts") String tags,
            @RequestParam(value = "privacyStatus", required = false, defaultValue = "private") String privacyStatus,
            @RequestParam(value = "categoryId", required = false, defaultValue = "23") String categoryId,
            @CookieValue(value = "youtube_access_token", required = false) String accessToken){

        // URL이 비어있는지 확인
        if (videoUrl == null || videoUrl.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "비디오 URL이 누락되었습니다");
            return ResponseEntity.badRequest().body(response);
        }

        // 토큰이 없는지 확인
        if (accessToken == null || accessToken.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "유튜브 인증이 필요합니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String finalDescription = (description != null) ? description+ " #Shorts" : "#Shorts";



        try {
            // 추가: 토큰 유효성 검사
            if (!youtubeAuthService.validateToken(accessToken)) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "유효하지 않은 액세스 토큰입니다. 다시 로그인해주세요.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }


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
