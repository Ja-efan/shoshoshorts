package com.sss.backend.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sss.backend.api.dto.constant.DefaultTensors;
import com.sss.backend.domain.document.SceneDocument;
import com.sss.backend.domain.repository.SceneDocumentRepository;
import com.sss.backend.config.S3Config;
import com.sss.backend.domain.repository.VoiceRepository;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class AudioService {

    private final SceneDocumentRepository sceneDocumentRepository;
    private final VoiceRepository voiceRepository;
    private final WebClient webClient;
    private final S3Config s3Config;
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;

    @Value("${api.password}")
    private String apiPassword;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${audio.eleven.api.url}")
    private String elevenApiUrl;

    @Value("${audio.zonos.api.url}")
    private String zonosApiUrl;



    public AudioService(SceneDocumentRepository sceneDocumentRepository, WebClient webClient, 
                       S3Config s3Config, FFmpeg ffmpeg, FFprobe ffprobe, VoiceRepository voiceRepository) {
        this.sceneDocumentRepository = sceneDocumentRepository;
        this.webClient = webClient;
        this.s3Config = s3Config;
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;
        this.voiceRepository = voiceRepository;
    }

//    @Value("${audio.default.model-id}")
//    private String defaultModelId;
//
//    @Value("${audio.default.output-format}")
//    private String defaultOutputFormat;


    private final String defaultModelId = "eleven_multilingual_v2";
    private final String defaultOutputFormat = "mp3";



    // 스토리 전체 오디오 생성
    public SceneDocument generateAllAudios(String storyId, String audioModelName) {
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
                
                // 이미 오디오가 생성되어 있는지 확인
                if (audio.get("audio_url") != null && !((String) audio.get("audio_url")).isEmpty()) {
                    log.info("오디오가 이미 생성되어 있어 건너뜀: storyId={}, sceneId={}, audioId={}", 
                            storyId, sceneId, audioId);
                    continue;
                }

                if(audioModelName.equals("Zonos")){
                    log.info("사용된 오디오 생성 모델: " + audioModelName + "--------zonos?");

                    //zonos
                    generateZonosAudio(storyId, sceneId, audioId);

                }else{
                    log.info("사용된 오디오 생성 모델: " + audioModelName+ "--------elevenlabs?");
                    //elevenlabs
                    generateAudio(storyId, sceneId, audioId);
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


    // (elevenlabs) 오디오 생성 모델 호출
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

        //오디오의 text와 character 찾기 -> request에 넣어야함
        String text = (String) targetAudio.get("text");
        String character = (String) targetAudio.get("character");

        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("오디오 텍스트가 비어있습니다: audioId=" + audioId);
        }

        // character에 따른 voicecode 찾기
        String voiceCode = findVoiceCodeByCharacter(sceneDocument,character);


        // API 요청 데이터 준비
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("text", text);
        requestData.put("voice_code", voiceCode);
        requestData.put("model_id", defaultModelId);
        requestData.put("output_format", defaultOutputFormat);
        requestData.put("script_id", Integer.parseInt(storyId));
        requestData.put("scene_id", sceneId);
        requestData.put("audio_id", audioId);
        
        try {
            // WebClient를 사용하여 API 호출
            Map<String, Object> responseBody =
                    webClient.post()
                    .uri(elevenApiUrl)
                    .header("apiPwd", activeProfile + apiPassword)
                    .bodyValue(requestData)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // 동기 처리를 위해 block() 사용

            if (responseBody == null) {
                throw new RuntimeException("오디오 생성 API 응답이 없습니다");
            }

            // 해당 오디오 정보 업데이트 (기존에 있더라도 덮어쓰기)
            String audioUrl = (String) responseBody.get("s3_url");
            targetAudio.put("audio_url", audioUrl);
            targetAudio.put("content_type", responseBody.get("content_type"));
            targetAudio.put("file_size", responseBody.get("file_size"));
            targetAudio.put("base_model", defaultModelId);
            targetAudio.put("audio_settings", defaultOutputFormat);
            targetAudio.put("voice_code", voiceCode);

            // 오디오 길이 추출 및 저장
            double durationInSeconds = extractAudioDuration(audioUrl);
            targetAudio.put("duration", durationInSeconds);

            // SceneDocument 저장
            SceneDocument updatedDocument = sceneDocumentRepository.save(sceneDocument);
            log.info("오디오 생성 및 문서 업데이트 완료: storyId={}, sceneId={}, audioId={}", storyId, sceneId, audioId);

            return updatedDocument;
        } catch (Exception e) {
            log.error("오디오 생성 중 오류 발생", e);
            throw new RuntimeException("오디오 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // (Zonos) 오디오 생성 모델 호출
    public SceneDocument generateZonosAudio(String storyId, int sceneId, int audioId) {
        log.info("zonos 오디오 생성 시작: storyId={}, sceneId={}, audioId={}", storyId, sceneId, audioId);

        // 스토리 데이터 조회
        Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
        if (sceneDocumentOpt.isEmpty()) {
            throw new RuntimeException("해당 스토리를 찾을 수 없습니다: " + storyId);
        }

        SceneDocument sceneDocument = sceneDocumentOpt.get();

        // 해당 오디오 찾기
        Map<String, Object> targetAudio = findSceneAndAudio(sceneDocument, sceneId, audioId);

        //오디오의 text와 character 찾기 -> request에 넣어야함
        String text = (String) targetAudio.get("text");
        String character = (String) targetAudio.get("character");

        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("오디오 텍스트가 비어있습니다: audioId=" + audioId);
        }

        // character에 따른 voicecode 찾기 -> 실제로는 voiceId(voice 테이블의 pk 값)----------------------------------------------
        String voiceCode = findVoiceCodeByCharacter(sceneDocument,character);

        //voiceCode(voiceId)에 따른 tensor 값 찾기
        Long voiceId = Long.parseLong(voiceCode);

        byte[] tensorBytes = new byte[0];

        if(voiceId == -1){
            // 남자 아나운서 byte[] 데이터
            tensorBytes = DefaultTensors.MALE_VOICE_2;
        }else if(voiceId == -2){
            // 여자 아나운서 byte[] 데이터
            tensorBytes = DefaultTensors.FEMALE_VOICE_1;
        }else{
            tensorBytes = voiceRepository.findEmbeddingTensorById(voiceId);
        }

        // byte[] -> JSON 형식의 문자열 변환
        String tensorJson = new String(tensorBytes, StandardCharsets.UTF_8);

        // JSON -> List<List<List<Float>>> 역직렬화
        List<List<List<Float>>> speakerTensor;
        try {
            ObjectMapper objectMapper = new ObjectMapper(); //JSON과 Java 객체 간의 변환 담당
            speakerTensor = objectMapper.readValue(tensorJson,
                    new TypeReference<List<List<List<Float>>>>() {});
            log.info("텐서 데이터 역직렬화 완료: {}", speakerTensor.size());
        } catch (Exception e) {
            log.error("텐서 데이터 역직렬화 중 오류 발생", e);
            throw new RuntimeException("텐서 데이터 변환에 실패했습니다: " + e.getMessage(), e);
        }

        //emtion arr 찾기------------------------------------------------------------------------------------
        Map<String, Object> emotionParams = (Map<String, Object>) targetAudio.get("emotionParams");
        List<Float> emotions = new ArrayList<>();

        // 요청 형식에 맞게 emotion 값 변환
        // [0_행복, 1_슬픔, 2_혐오, 3_두려움, 4_놀람, 5_분노, 6_기타, 7_중립]
        if (emotionParams != null) {
            // 0_행복
            emotions.add(((Number) emotionParams.getOrDefault("happiness", 0)).floatValue());
            // 1_슬픔
            emotions.add(((Number) emotionParams.getOrDefault("sadness", 0)).floatValue());
            // 2_혐오
            emotions.add(((Number) emotionParams.getOrDefault("disgust", 0)).floatValue());
            // 3_두려움
            emotions.add(((Number) emotionParams.getOrDefault("fear", 0)).floatValue());
            // 4_놀람
            emotions.add(((Number) emotionParams.getOrDefault("surprise", 0)).floatValue());
            // 5_분노
            emotions.add(((Number) emotionParams.getOrDefault("anger", 0)).floatValue());
            // 6_기타
            emotions.add(0.0f); // 기타 감정은 기본값 0으로 설정
            // 7_중립 - 항상 1로 설정
            emotions.add(1.0f);
        } else {
            // emotionParams가 null인 경우 기본값으로 설정
            emotions = Arrays.asList(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        }

        log.info("---------------------------emotions:   "+emotions);


        // API 요청 데이터 준비
        Map<String, Object> requestData = new HashMap<>();
//        requestData.put("model_choice", "Zyphra/Zonos-v0.1-transformer");
        requestData.put("text", text);
//        requestData.put("language", "ko"); // 기본값 ko
        requestData.put("speaker_tensor", speakerTensor);
        requestData.put("script_id", Integer.parseInt(storyId));
        requestData.put("scene_id", sceneId);
        requestData.put("audio_id", audioId);
        requestData.put("emotion", emotions);


        try {
            // WebClient를 사용하여 API 호출
            Map<String, Object> responseBody =
                    webClient.post()
                            .uri(zonosApiUrl)
                            .header("apiPwd", activeProfile + apiPassword)
                            .bodyValue(requestData)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block(); // 동기 처리를 위해 block() 사용

            if (responseBody == null) {
                throw new RuntimeException("오디오 생성 API 응답이 없습니다");
            }

            // 해당 오디오 정보 업데이트 (기존에 있더라도 덮어쓰기)
            String audioUrl = (String) responseBody.get("s3_url");
            targetAudio.put("audio_url", audioUrl);
            targetAudio.put("base_model", "Zyphra/Zonos-v0.1-transformer");
            targetAudio.put("audio_settings", defaultOutputFormat);
            targetAudio.put("voice_code", voiceCode);

            //저장 할 지 말지 결정
            targetAudio.put("sample_rate", responseBody.get("sample_rate"));
            targetAudio.put("seed", responseBody.get("seed"));

            // 오디오 길이 추출 및 저장
            double durationInSeconds = extractAudioDuration(audioUrl);
            targetAudio.put("duration", durationInSeconds);

            // SceneDocument 저장
            SceneDocument updatedDocument = sceneDocumentRepository.save(sceneDocument);
            log.info("오디오 생성 및 문서 업데이트 완료: storyId={}, sceneId={}, audioId={}", storyId, sceneId, audioId);

            return updatedDocument;
        } catch (Exception e) {
            log.error("오디오 생성 중 오류 발생", e);
            throw new RuntimeException("오디오 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // 오디오 파일 길이 추출 메서드
    private double extractAudioDuration(String audioUrl) {
        try {
            String s3Key = s3Config.extractS3KeyFromUrl(audioUrl);
            String presignedUrl = s3Config.generatePresignedUrl(s3Key);
            
            // net.bramp.ffmpeg 라이브러리 API 사용
            FFmpegProbeResult probeResult = ffprobe.probe(presignedUrl);
            double durationInSeconds = probeResult.format.duration;
            
            log.info("오디오 길이 추출 성공: {} 초", durationInSeconds);
            return durationInSeconds;
        } catch (Exception e) {
            log.error("오디오 길이 추출 중 오류 발생: {}", e.getMessage(), e);
            return 0.0; // 추출 실패 시 기본값 0으로 설정
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

    //character로 따른 voiceCode를 찾는 새로운 메서드
    private String findVoiceCodeByCharacter(SceneDocument sceneDocument, String characterName){

        // narration이면 narvoicecode 사용
        if (characterName.equals("narration")) {
            return sceneDocument.getNarVoiceCode();
        }

        //characterArr에서 해당 이름의 캐릭터 찾기
        List<Map<String, Object>> characterArr = sceneDocument.getCharacterArr();
        if(characterArr != null){
            for(Map<String,Object> character : characterArr){
                String name = (String) character.get("name");
                if(characterName.equals(name)){
                    return (String) character.get("voiceCode");
                }
            }
        }

        // 일치하는 캐릭터가 없으면 narvoicecode 사용
        log.info("캐릭터 '{}' 에 대한 voicecode를 찾을 수 없어 narvoicecode를 사용합니다.", characterName);
        return sceneDocument.getNarVoiceCode();

    }

}
