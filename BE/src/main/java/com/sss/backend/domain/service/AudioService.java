package com.sss.backend.domain.service;

import com.sss.backend.domain.document.SceneDocument;
import com.sss.backend.domain.repository.SceneDocumentRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AudioService {

    private final SceneDocumentRepository sceneDocumentRepository;
    private final WebClient webClient;

    @Value("${api.password}")
    private String apiPassword;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public AudioService(SceneDocumentRepository sceneDocumentRepository, WebClient webClient){
        this.sceneDocumentRepository = sceneDocumentRepository;
        this.webClient = webClient;
    }

//    @Value("${audio.api.url}")
//    private String audioApiUrl;
//
//    @Value("${audio.default.voice-code}")
//    private String defaultVoiceCode;
//
//    @Value("${audio.default.model-id}")
//    private String defaultModelId;
//
//    @Value("${audio.default.output-format}")
//    private String defaultOutputFormat;

//    private final String audioApiUrl = ":8000/elevenlabs/tts";
private final String audioApiUrl = "http://35.216.58.38:8000/elevenlabs/tts";
//    private final String defaultVoiceCode = "uyVNoMrnUku1dZyVEXwD";
    private final String defaultModelId = "eleven_multilingual_v2";
    private final String defaultOutputFormat = "mp3";




    // 스토리 전체 오디오 생성
    public SceneDocument generateAllAudios(String storyId) {
        log.info("스토리 전체 오디오 생성 시작: storyId={}", storyId);

        // 스토리 문서 조회
        Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
        if (sceneDocumentOpt.isEmpty()) {
            throw new RuntimeException("해당 스토리를 찾을 수 없습니다: " + storyId);
        }

        //scene arr 찾기 위한 중간 작업(Optional 처리)
        SceneDocument sceneDocument = sceneDocumentOpt.get();

        List<Map<String, Object>> sceneArr = sceneDocument.getSceneArr();

        // 각 씬에 대해 반복 처리
        // scene 배열 안에 각각의 scene 안에 audio 배열 존재
        for (Map<String, Object> scene : sceneArr) {
            int sceneId = ((Number) scene.get("sceneId")).intValue();
            List<Map<String, Object>> audioArr = (List<Map<String, Object>>) scene.get("audioArr");

            // 각 씬의 오디오에 대해 반복 처리
            for (Map<String, Object> audio : audioArr) {
                int audioId = ((Number) audio.get("audioId")).intValue();

                // 오디오 생성
                try {

//                    // 각 요청 사이에 500ms 지연
//                    Thread.sleep(500);

                    generateAudio(storyId, sceneId, audioId);
                } catch (Exception e) {
                    log.error("오디오 생성 중 오류 발생: storyId={}, sceneId={}, audioId={}, error={}",
                            storyId, sceneId, audioId, e.getMessage());
                    // 오류가 있어도 다음 오디오 계속 처리
                }
            }
        }

        // 최종 문서 조회하여 반환
        Optional<SceneDocument> updatedDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
        if (updatedDocumentOpt.isEmpty()) {
            throw new RuntimeException("업데이트된 문서를 찾을 수 없습니다: storyId=" + storyId);
        }

        log.info("스토리 전체 오디오 생성 완료: storyId={}", storyId);
        return updatedDocumentOpt.get();
    }


    // (각각의) 오디오 생성 모델 호출
    public SceneDocument generateAudio(String storyId, int sceneId, int audioId) {
        log.info("오디오 생성 시작: storyId={}, sceneId={}, audioId={}", storyId, sceneId, audioId);

        // 스토리 데이터 조회
        Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
        if (sceneDocumentOpt.isEmpty()) {
            throw new RuntimeException("해당 스토리를 찾을 수 없습니다: " + storyId);
        }

        SceneDocument sceneDocument = sceneDocumentOpt.get();

        // 해당 오디오 찾기
        Map<String, Object> targetAudio = findSceneAndAudio(sceneDocument, sceneId, audioId);

        //오디오의 text만 찾기 -> request에 넣어야함
        String text = (String) targetAudio.get("text");

        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("오디오 텍스트가 비어있습니다: audioId=" + audioId);
        }

        // API 요청 데이터 준비
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("text", text);
//        requestData.put("voice_code", defaultVoiceCode);
        requestData.put("model_id", defaultModelId);
        requestData.put("output_format", defaultOutputFormat);
        requestData.put("script_id", Integer.parseInt(storyId));
        requestData.put("scene_id", sceneId);
        requestData.put("audio_id", audioId);
        requestData.put("apiPwd", activeProfile + apiPassword);
        
        try {
            // WebClient를 사용하여 API 호출
            Map<String, Object> responseBody =
                    webClient.post()
                    .uri(audioApiUrl)
                    .bodyValue(requestData)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // 동기 처리를 위해 block() 사용

            if (responseBody == null) {
                throw new RuntimeException("오디오 생성 API 응답이 없습니다");
            }

            // 해당 오디오 정보 업데이트 (기존에 있더라도 덮어쓰기)
            targetAudio.put("audio_url", responseBody.get("s3_url"));
            targetAudio.put("content_type", responseBody.get("content_type"));
            targetAudio.put("file_size", responseBody.get("file_size"));
            targetAudio.put("base_model", defaultModelId);
            targetAudio.put("audio_settings", defaultOutputFormat);

            // SceneDocument 저장
            SceneDocument updatedDocument = sceneDocumentRepository.save(sceneDocument);
            log.info("오디오 생성 및 문서 업데이트 완료: storyId={}, sceneId={}, audioId={}", storyId, sceneId, audioId);
            return updatedDocument;

        } catch (Exception e) {
            log.error("오디오 생성 중 오류 발생", e);
            throw new RuntimeException("오디오 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }


    //특정 씬과 오디오 찾는 메서드
    private Map<String, Object> findSceneAndAudio(SceneDocument sceneDocument, int sceneId, Integer audioId) {

        // 해당 씬 찾기
        Map<String, Object> targetScene = null;
        List<Map<String, Object>> sceneArr = sceneDocument.getSceneArr();

        for (Map<String, Object> scene : sceneArr) {
            if (((Number) scene.get("sceneId")).intValue() == sceneId) {
                targetScene = scene;
                break;
            }
        }

        if (targetScene == null) {
            throw new RuntimeException("해당 씬을 찾을 수 없습니다: sceneId=" + sceneId);
        }

        // audioId가 null이면 씬 자체를 반환
        if (audioId == null) {
            return targetScene;
        }

        // 해당 씬의 오디오 배열 가져오기
        List<Map<String, Object>> audioArr = (List<Map<String, Object>>) targetScene.get("audioArr");

        // 해당 audioId의 오디오 찾기
        for (Map<String, Object> audio : audioArr) {
            if (((Number) audio.get("audioId")).intValue() == audioId) {
                return audio;
            }
        }

        throw new RuntimeException("해당 오디오를 찾을 수 없습니다: audioId=" + audioId);
    }


}
