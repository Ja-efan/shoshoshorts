package com.sss.backend.api.controller;

import com.sss.backend.api.dto.VideoMetadata;
import com.sss.backend.api.dto.VideoUploadDTO;
import com.sss.backend.api.dto.VideoUploadResponse;
import com.sss.backend.domain.entity.Users;
import com.sss.backend.domain.repository.UserRepository;
import com.sss.backend.domain.service.YoutubeAuthService;
import com.sss.backend.domain.service.YoutubeService;
import com.sss.backend.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/youtube")
@Slf4j
public class YoutubeController {

    private final YoutubeService youtubeService;
    private final YoutubeAuthService youtubeAuthService;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    public YoutubeController(YoutubeService youtubeService, YoutubeAuthService youtubeAuthService, JWTUtil jwtUtil, UserRepository userRepository){
        this.youtubeService = youtubeService;
        this.youtubeAuthService = youtubeAuthService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }


    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(@RequestBody VideoUploadDTO uploadDTO,
                                         @CookieValue(value = "youtube_access_token", required = false) String accessToken,
                                         HttpServletRequest request) {
        log.info(uploadDTO.toString());

        // storyId가 비어있는지 확인
        if (uploadDTO.getStoryId() == null || uploadDTO.getStoryId().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "스토리 ID가 누락되었습니다");
            return ResponseEntity.badRequest().body(response);
        }

        // 토큰이 없는지 확인
        if (accessToken == null || accessToken.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "유튜브 인증이 필요합니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {

            // 토큰에서 사용자 ID 추출
            String token = jwtUtil.extractTokenFromRequest(request);

            if(token == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "인증 토큰이 필요합니다."));
            }

            String email = jwtUtil.getEmail(token);
            String provider = jwtUtil.getProvider(token);

            // 사용자 정보 조회
            Users user = userRepository.findByEmailAndProvider(email, provider)
                    .orElseThrow(() -> new RuntimeException("해당하는 유저 정보가 없습니다."));

            Long userId = user.getId();

            // 추가: 토큰 유효성 검사
            if (!youtubeAuthService.validateToken(accessToken)) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "유효하지 않은 액세스 토큰입니다. 다시 로그인해주세요.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 메타데이터 준비
            VideoMetadata metadata = new VideoMetadata();
            metadata.setTitle(uploadDTO.getTitle());
            metadata.setDescription(uploadDTO.getDescription());
            metadata.setTags(uploadDTO.getTags());
            metadata.setPrivacyStatus(uploadDTO.getPrivacyStatus());
            metadata.setCategoryId(uploadDTO.getCategoryId());


            // 비디오 업로드 (비동기 메서드를 블로킹 호출)
            VideoUploadResponse response = youtubeService.uploadVideo(accessToken, uploadDTO.getStoryId(), metadata, userId).block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }




}
