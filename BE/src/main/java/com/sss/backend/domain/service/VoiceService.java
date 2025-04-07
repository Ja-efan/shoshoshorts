package com.sss.backend.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sss.backend.api.dto.VoiceCreateDTO;
import com.sss.backend.api.dto.VoiceResponseDTO;
import com.sss.backend.api.dto.VoiceZonosVectorDTO;
import com.sss.backend.config.S3Config;
import com.sss.backend.domain.entity.Users;
import com.sss.backend.domain.entity.Voice;
import com.sss.backend.domain.repository.UserRepository;
import com.sss.backend.domain.repository.VoiceRepository;
import com.sss.backend.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;


import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
//@RequiredArgsConstructor // WebClient Qualifier 쓰기 위함.
public class VoiceService {
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final VoiceRepository voiceRepository;
    private final S3Config s3Config;

//    @Qualifier("webClientVoice")
    private final WebClient webClient;

    @Value("${api.password}")
    private String apiPassword;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public VoiceService(UserRepository userRepository,
                        JWTUtil jwtUtil,
                        VoiceRepository voiceRepository,
                        S3Config s3Config,
                        @Qualifier("webClientVoice") WebClient webClient) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.voiceRepository = voiceRepository;
        this.s3Config = s3Config;
        this.webClient = webClient;
    }

    public ResponseEntity<?> VoiceCreate(String email, VoiceCreateDTO dto) {

        // 유저 조회
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다"));

        log.info("유저 {}",user);


        // FastAPI 요청 준비
        String combinedPwd = activeProfile + apiPassword;


        Map<String, String> jsonData = Map.of(
                "speaker_audio_base64",dto.getAudioBase64()
        );
//        log.info("FastAPI 요청 : {}",jsonData);
        // FastAPI 에 요청
        VoiceZonosVectorDTO body = webClient.post()
                .uri("/zonos/base64_to_tensor")
                .header("apiPwd",combinedPwd)
                .bodyValue(jsonData)
                .retrieve()
                .bodyToMono(VoiceZonosVectorDTO.class)
                .block();

//        log.info("FastAPI Response : {}",body);

        if (body == null) {
            return ResponseEntity.status(500).body(Map.of("error", "FastAPI Zonos 응답이 null입니다."));
        }

        // 응답처리  (embedding_tensor, voice_url)
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String tensorJson = objectMapper.writeValueAsString(body.getSpeaker_tensor());

            log.info("tesnor json {} ",tensorJson);

            // 텐서 직렬화 -> Json ->> byte[]
            byte[] tensorBytes = tensorJson.getBytes(StandardCharsets.UTF_8);

            log.info("tensor Bytes: {}",tensorBytes);


            // S3 키만 저장
            String s3Key = s3Config.extractS3KeyFromUrl(body.getS3_url());

            // RDBMS 저장.
            Voice voice = new Voice();
            voice.setTitle(dto.getTitle());
            voice.setVoiceSampleUrl(s3Key);
            voice.setDescription(dto.getDescription());
            voice.setEmbeddingTensor(tensorBytes);
            voice.setUser(user);

            voiceRepository.save(voice);

            String presignedUrl = s3Config.generatePresignedUrl(s3Key);

            return ResponseEntity.ok(Map.of(
                    "message","보이스 생성 및 저장이 완료되었습니다.",
                    "presignedUrl",presignedUrl,
                    "status",200
            ));

        }
        catch (JsonProcessingException e) {
            log.error("Tensor Json 직렬화 실패 : {}",e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error","서버 내부 오류 (tensro 직렬화 실패)"));
        }

    }

    @Transactional //
    public ResponseEntity<?> findMyVoices(HttpServletRequest request) {

        String token = jwtUtil.extractTokenFromRequest(request);
        String email = jwtUtil.getEmail(token);

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다"));

        log.info("유저 : {}",user);

        List<Voice> voices = voiceRepository.findAllByUser(user);

        log.info("voices : {}",voices);

        List<VoiceResponseDTO> response = new ArrayList<>();
        for (Voice voice : voices) {
            log.info("url : {}",voice.getVoiceSampleUrl());
            String presignedUrl = "http://...";
//            if (voice.getVoiceSampleUrl().isEmpty() && voice.getVoiceSampleUrl() != null){
            if (StringUtils.hasText(voice.getVoiceSampleUrl())){
                // null, "", " " 모두 확인.
//                String s3Key = s3Config.extractS3KeyFromUrl(voice.getVoiceSampleUrl());
                presignedUrl = s3Config.generatePresignedUrl(voice.getVoiceSampleUrl());
            }
            log.info("presignedURL : {}", presignedUrl);
            VoiceResponseDTO dto = new VoiceResponseDTO(
                    voice.getId(),
                    voice.getTitle(),
                    // Todo : presigned URL로 바꿔서 넣어야해 받아오기
                    voice.getDescription(),
                    presignedUrl,
                    voice.getUpdatedAt() != null ? voice.getUpdatedAt().toString() : null,
                    voice.getCreatedAt() != null ? voice.getCreatedAt().toString() : null
            );
            response.add(dto);
        }

        return ResponseEntity.ok(Map.of(
                "message","success",
                "status",200,
                "data",response));
    }
}

