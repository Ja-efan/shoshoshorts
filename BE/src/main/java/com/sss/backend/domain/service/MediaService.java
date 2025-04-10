package com.sss.backend.domain.service;

import com.sss.backend.api.dto.SceneImageRequest;
import com.sss.backend.domain.document.SceneDocument;
import com.sss.backend.domain.entity.VideoProcessingStep;
import com.sss.backend.domain.repository.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j  //로그 기능
@Service
public class MediaService {

    private final SceneDocumentRepository sceneDocumentRepository;
    private final ImageService imageService;
    private final AudioService audioService;
    private final VideoProcessingStatusService videoProcessingStatusService;

    public MediaService(
            SceneDocumentRepository sceneDocumentRepository,
            ImageService imageService,
            AudioService audioService,
            VideoProcessingStatusService videoProcessingStatusService) {
        this.sceneDocumentRepository = sceneDocumentRepository;
        this.imageService = imageService;
        this.audioService = audioService;
        this.videoProcessingStatusService = videoProcessingStatusService;
    }


    //모든 씬에 대한 미디어(오디오, 이미지) 생성 처리
    @Async("mediaTaskExecutor")
    public CompletableFuture<Void> processAllScenes(String storyId, String audioModelName, String imageModelName) {
        log.info("스토리 전체 씬 미디어 처리 시작: storyId={}", storyId);

        try {
            // 스토리 데이터 조회
            Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
            log.info("스토리데이터 조회 완료" + sceneDocumentOpt);
            if (sceneDocumentOpt.isEmpty()) {
                return CompletableFuture.failedFuture(
                        new RuntimeException("해당 스토리를 찾을 수 없습니다: " + storyId));
            }

            //Optional에서 실제 SceneDocument 객체 추출
            SceneDocument sceneDocument = sceneDocumentOpt.get();

            //sceneDocument 데이터에 audioModelName, imageModelName 저장
            sceneDocument.setAudioModelName(audioModelName);
            sceneDocument.setImageModelName(imageModelName);

            List<Map<String, Object>> sceneArr = sceneDocument.getSceneArr();
            log.info("scene배열 가져오기");

            // 오디오 처리를 위한 필터링된 씬 배열 생성
            List<Map<String, Object>> scenesToProcessAudio = new ArrayList<>();
            
            // 각 씬별로 오디오 파일 존재 여부 확인
            for (Map<String, Object> scene : sceneArr) {
                boolean needsAudioProcessing = false;
                List<Map<String, Object>> audioArr = (List<Map<String, Object>>) scene.get("audioArr");
                
                if (audioArr != null) {
                    for (Map<String, Object> audio : audioArr) {
                        // audio_url이 없거나 비어있으면 오디오 생성 필요
                        if (audio.get("audio_url") == null || ((String) audio.get("audio_url")).isEmpty()) {
                            needsAudioProcessing = true;
                            break;
                        }
                    }
                    
                    if (needsAudioProcessing) {
                        scenesToProcessAudio.add(scene);
                    }
                }
            }
            
            log.info("오디오 생성 필요한 씬 수: {}", scenesToProcessAudio.size());
            
            // 오디오 처리 CompletableFuture
            CompletableFuture<Void> audioFuture;
            
            // 처리할 오디오가 있는 경우에만 오디오 생성 요청
            if (!scenesToProcessAudio.isEmpty()) {
                audioFuture = CompletableFuture.runAsync(() -> {
                    try {
                        // 오디오 생성 및 상태 업데이트 처리
                        audioService.generateAllAudios(storyId, audioModelName);
                        // 오디오 생성 완료 상태 업데이트
                        videoProcessingStatusService.updateProcessingStep(storyId, VideoProcessingStep.VOICE_COMPLETED);
                    } catch (Exception e) {
                        log.error("스토리 오디오 생성 중 오류 발생: storyId={}, error={}", storyId, e.getMessage(), e);
                        throw new RuntimeException("오디오 생성 실패: " + e.getMessage(), e);
                    }
                });
            } else {
                log.info("모든 오디오가 이미 생성되어 있어 오디오 생성 과정 건너뜀: storyId={}", storyId);
                audioFuture = CompletableFuture.completedFuture(null);
            }

            // 이미지 처리를 위한 CompletableFuture 리스트
            // 각 씬의 이미지 처리 결과를 담을 리스트
            List<CompletableFuture<Void>> imageFutures = new ArrayList<>();
            
            // 각 씬에 대해 이미지 생성 작업 병렬로 생성
            for (Map<String, Object> scene : sceneArr) {
                int sceneId = ((Number) scene.get("sceneId")).intValue();
                
                // 이미지가 이미 존재하는지 확인
                if (scene.get("image_url") != null && !((String) scene.get("image_url")).isEmpty()) {
                    log.info("씬 이미지가 이미 존재하여 생성 건너뜀: storyId={}, sceneId={}", storyId, sceneId);
                    continue; // 이미지가 이미 있으면 생성 건너뛰기
                }
                
                //각 씬에 대해 이미지 생성 메서드 호출
                CompletableFuture<Void> imageFuture = imageService.generateImageForScene(storyId, sceneId, imageModelName)
                        //이미지 생성 완료 시 실행됨
                        .thenAccept(response -> {
                            log.info("씬 이미지 생성 완료: storyId={}, sceneId={}, url={}",
                                    storyId, sceneId, response.getImage_url());
                        });
                // 에러 핸들링은 allOf에서 일괄적으로 처리하도록 변경
                imageFutures.add(imageFuture);
            }
            
            log.info("이미지 생성 필요한 씬 수: {}", imageFutures.size());

            // 모든 오디오와 이미지 처리가 완료될 때까지 대기할 CompletableFuture 배열
            CompletableFuture<Void>[] allFutures = new CompletableFuture[imageFutures.size() + 1];
            allFutures[0] = audioFuture; //첫번째는 오디오 처리 future
            for (int i = 0; i < imageFutures.size(); i++) {
                allFutures[i + 1] = imageFutures.get(i);
            }

            // 하나라도 실패하면 전체가 실패하도록, 성공 시 이미지 완료 상태로 업데이트
            return CompletableFuture.allOf(allFutures)
                .thenRun(() -> {
                    log.info("스토리 전체 씬 미디어 처리 완료: storyId={}", storyId);
                    // 이미지 생성 완료 상태 업데이트
                    videoProcessingStatusService.updateProcessingStep(storyId, VideoProcessingStep.IMAGE_COMPLETED);
                    
                    // 확인을 위한 로그 추가
                    VideoProcessingStep currentStep = videoProcessingStatusService.getProcessingStep(storyId);
                    log.info("미디어 처리 완료 후 상태 확인: storyId={}, step={}", 
                            storyId, currentStep != null ? currentStep.name() : "null");
                });
            // exceptionally 제거 - 오류를 상위로 전파하도록 함

        } catch (Exception e) {
            log.error("스토리 전체 씬 미디어 처리 요청 중 오류 발생: storyId={}, error={}",
                    storyId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }


    //이미지 생성 요청 메서드
    @Async("mediaTaskExecutor")
    public CompletableFuture<Void> processSceneImage(String storyId, Integer sceneId, String imageModelName) {
        log.info("씬 이미지 처리 시작: storyId={}, sceneId={}", storyId, sceneId);

        try {
            // 이미지 생성 요청
            return imageService.generateImageForScene(storyId, sceneId, imageModelName)
                    .thenAccept(response -> {
                        log.info("씬 이미지 생성 완료: storyId={}, sceneId={}, url={}",
                                storyId, sceneId, response.getImage_url());
                    })
                    .exceptionally(ex -> {
                        log.error("씬 이미지 처리 중 오류 발생: storyId={}, sceneId={}, error={}",
                                storyId, sceneId, ex.getMessage(), ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("씬 이미지 처리 요청 중 오류 발생: storyId={}, sceneId={}, error={}",
                    storyId, sceneId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    //특정 씬 찾는 메서드
    private Map<String, Object> findScene(SceneDocument sceneDocument, int sceneId) {
        return sceneDocument.getSceneArr().stream()
                .filter(scene -> ((Number) scene.get("sceneId")).intValue() == sceneId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("해당 씬을 찾을 수 없습니다: sceneId=" + sceneId));
    }
}
