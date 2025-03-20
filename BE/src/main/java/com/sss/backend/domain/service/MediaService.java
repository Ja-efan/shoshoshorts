package com.sss.backend.domain.service;

import com.sss.backend.api.dto.SceneImageRequest;
import com.sss.backend.domain.document.SceneDocument;
import com.sss.backend.infrastructure.repository.*;
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

    public MediaService(SceneDocumentRepository sceneDocumentRepository,ImageService imageService,AudioService audioService){
        this.sceneDocumentRepository = sceneDocumentRepository;
        this.imageService = imageService;
        this.audioService = audioService;
    }


    //모든 씬에 대한 미디어(오디오, 이미지) 생성 처리
    @Async("mediaTaskExecutor")
    public CompletableFuture<Void> processAllScenes(String storyId) {
        log.info("스토리 전체 씬 미디어 처리 시작: storyId={}", storyId);

        try {
            // 스토리 문서 조회
            Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
            if (sceneDocumentOpt.isEmpty()) {
                return CompletableFuture.failedFuture(
                        new RuntimeException("해당 스토리를 찾을 수 없습니다: " + storyId));
            }

            SceneDocument sceneDocument = sceneDocumentOpt.get();
            List<Map<String, Object>> sceneArr = sceneDocument.getSceneArr();

            // 각 씬에 대한 미디어 처리 요청을 모아놓을 리스트
            List<CompletableFuture<Void>> sceneFutures = sceneArr.stream()
                    .map(scene -> ((Number) scene.get("sceneId")).intValue())
                    .map(sceneId -> processSceneMedia(storyId, sceneId))
                    .collect(Collectors.toList());

            // 모든 씬 처리가 완료되면 결과 반환
            return CompletableFuture.allOf(
                    sceneFutures.toArray(new CompletableFuture[0])
            ).thenRun(() -> {
                log.info("스토리 전체 씬 미디어 처리 완료: storyId={}", storyId);

                // 여기에 필요한 경우 추가 로직 구현
                // 예: 모든 씬 처리 후 영상 합성 요청 등
            }).exceptionally(ex -> {
                log.error("스토리 전체 씬 미디어 처리 중 오류 발생: storyId={}, error={}",
                        storyId, ex.getMessage(), ex);
                return null;
            });

        } catch (Exception e) {
            log.error("스토리 전체 씬 미디어 처리 요청 중 오류 발생: storyId={}, error={}",
                    storyId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }




    //씬의 오디오 및 이미지 생성 요청
    @Async("mediaTaskExecutor")
    public CompletableFuture<Void> processSceneMedia(String storyId, Integer sceneId) {
        log.info("씬 미디어 처리 시작: storyId={}, sceneId={}", storyId, sceneId);

        try {
            // 스토리 문서 조회
            Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
            if (sceneDocumentOpt.isEmpty()) {
                return CompletableFuture.failedFuture(
                        new RuntimeException("해당 스토리를 찾을 수 없습니다: " + storyId));
            }

            SceneDocument sceneDocument = sceneDocumentOpt.get();

            // 해당 씬 찾기
            Map<String, Object> targetScene = findScene(sceneDocument, sceneId);

//            // 씬 미디어 상태 업데이트
//            targetScene.put("mediaStatus", "MEDIA_PROCESSING");
//            sceneDocumentRepository.save(sceneDocument);

            // 1. 오디오 생성 요청 - 씬 내의 모든 오디오
            CompletableFuture<SceneDocument> audioFuture = CompletableFuture.supplyAsync(() ->
                    audioService.generateSceneAudio(storyId, sceneId)
            );

            // 2. 이미지 생성 요청
            CompletableFuture<Void> imageFuture = generateImageForScene(storyId, sceneId);

            // 3. 모든 처리가 완료되면 씬 상태 업데이트
            return CompletableFuture.allOf(audioFuture, imageFuture)
                    .thenAccept(v -> {
                        // 결과 씬 문서 다시 조회
                        Optional<SceneDocument> updatedDocOpt = sceneDocumentRepository.findByStoryId(storyId);
                        if (updatedDocOpt.isEmpty()) {
                            throw new RuntimeException("업데이트된 스토리 문서를 찾을 수 없습니다: " + storyId);
                        }

                        SceneDocument updatedDoc = updatedDocOpt.get();
                        Map<String, Object> updatedScene = findScene(updatedDoc, sceneId);

//                        // 씬 상태 업데이트
//                        updatedScene.put("mediaStatus", "MEDIA_READY");
//                        sceneDocumentRepository.save(updatedDoc);

//                        // 모든 씬의 미디어 생성이 완료되었는지 확인
//                        checkStoryCompletionAndNotify(storyId);
                    })
                    .exceptionally(ex -> {
                        log.error("씬 미디어 처리 중 오류 발생: storyId={}, sceneId={}, error={}",
                                storyId, sceneId, ex.getMessage(), ex);

                        return null;
                    });
        } catch (Exception e) {
            log.error("씬 미디어 처리 요청 중 오류 발생: storyId={}, sceneId={}, error={}",
                    storyId, sceneId, e.getMessage(), e);

            return CompletableFuture.failedFuture(e);
        }
    }


    //이미지 생성 요청
    private CompletableFuture<Void> generateImageForScene(String storyId, int sceneId) {
        log.info("씬 이미지 생성 시작: storyId={}, sceneId={}", storyId, sceneId);

        Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
        if (sceneDocumentOpt.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new RuntimeException("해당 스토리를 찾을 수 없습니다: " + storyId));
        }

        SceneDocument sceneDocument = sceneDocumentOpt.get();

        // 해당 씬 찾기
        Map<String, Object> targetScene = findScene(sceneDocument, sceneId);

        // 이미지 생성 요청 객체 생성
        SceneImageRequest imageRequest = new SceneImageRequest();
        imageRequest.setSceneId(sceneId);

        // StoryMetadata 설정
        SceneImageRequest.StoryMetadata storyMetadata = new SceneImageRequest.StoryMetadata();
        storyMetadata.setStory_id(Integer.parseInt(storyId));
        storyMetadata.setTitle(sceneDocument.getStoryTitle());

        // Characters 설정
        List<Map<String, Object>> characterMapList = sceneDocument.getCharacterArr();
        List<SceneImageRequest.Character> characters = new ArrayList<>();

        for (Map<String, Object> charMap : characterMapList) {
            SceneImageRequest.Character character = new SceneImageRequest.Character();
            character.setName((String) charMap.get("name"));

            // gender가 String이면 Integer로 변환 (예: "남자" -> 1)
            String genderStr = (String) charMap.get("gender");
            character.setGender("남자".equals(genderStr) ? 1 : 2);

            character.setDescription((String) charMap.get("properties"));
            characters.add(character);
        }

        storyMetadata.setCharacters(characters);
        imageRequest.setStoryMetadata(storyMetadata);

        // Audios 설정
        List<Map<String, Object>> audioMapList = (List<Map<String, Object>>) targetScene.get("audioArr");
        List<SceneImageRequest.Audio> audios = new ArrayList<>();

        for (Map<String, Object> audioMap : audioMapList) {
            SceneImageRequest.Audio audio = new SceneImageRequest.Audio();
            audio.setType((String) audioMap.get("type"));
            audio.setCharacter((String) audioMap.get("character"));
            audio.setText((String) audioMap.get("text"));
            audio.setEmotion((String) audioMap.get("emotion"));
            audios.add(audio);
        }

        imageRequest.setAudios(audios);

        // 이미지 서비스에 요청
        return imageService.generateImage(imageRequest, storyId)
                .thenAccept(response -> {
                    log.info("씬 이미지 생성 완료: storyId={}, sceneId={}, url={}",
                            storyId, sceneId, response.getImage_url());
                })
                .exceptionally(ex -> {
                    log.error("씬 이미지 생성 중 오류 발생: storyId={}, sceneId={}, error={}",
                            storyId, sceneId, ex.getMessage(), ex);
                    return null;
                });
    }

    /**
     * 특정 씬 찾기
     */
    private Map<String, Object> findScene(SceneDocument sceneDocument, int sceneId) {
        return sceneDocument.getSceneArr().stream()
                .filter(scene -> ((Number) scene.get("sceneId")).intValue() == sceneId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("해당 씬을 찾을 수 없습니다: sceneId=" + sceneId));
    }


//    /**
//     * 스토리 내 모든 씬의 미디어 생성 완료 여부 확인 및 영상 병합 서비스 호출
//     */
//    private void checkStoryCompletionAndNotify(String storyId) {
//        log.info("스토리 미디어 완료 상태 확인: storyId={}", storyId);
//
//        Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
//        if (sceneDocumentOpt.isEmpty()) {
//            log.error("스토리 완료 확인 중 문서를 찾을 수 없음: storyId={}", storyId);
//            return;
//        }
//
//        SceneDocument sceneDocument = sceneDocumentOpt.get();
//        List<Map<String, Object>> sceneArr = sceneDocument.getSceneArr();
//
//        // 모든 씬이 MEDIA_READY 상태인지 확인
//        boolean allReady = sceneArr.stream()
//                .allMatch(scene -> "MEDIA_READY".equals(scene.get("mediaStatus")));
//
//        // 하나라도 MEDIA_ERROR 상태인지 확인
//        boolean hasError = sceneArr.stream()
//                .anyMatch(scene -> "MEDIA_ERROR".equals(scene.get("mediaStatus")));
//
//        // 상태 업데이트
//        if (hasError) {
//            updateStoryMediaStatus(sceneDocument, "MEDIA_ERROR");
//        } else if (allReady) {
//            updateStoryMediaStatus(sceneDocument, "MEDIA_READY");
//
//            // 여기서 C(영상 병합 담당)에게 완료 신호 전달
//            // 이 부분은 실제 구현 환경에 따라 다양한 방식으로 처리할 수 있음:
//            // 1. 직접 MergeService를 주입받아 호출
//            // 2. 이벤트 발행 (Event Publishing)
//            // 3. 메시지 큐에 메시지 전송
//
//            try {
//                // 예시: mergeService.startMergeProcess(storyId);
//                log.info("영상 병합 작업 요청 전송: storyId={}", storyId);
//            } catch (Exception e) {
//                log.error("영상 병합 요청 중 오류 발생: storyId={}, error={}",
//                        storyId, e.getMessage(), e);
//                updateStoryMediaStatus(sceneDocument, "MERGE_REQUEST_ERROR");
//            }
//        }
//    }

}
