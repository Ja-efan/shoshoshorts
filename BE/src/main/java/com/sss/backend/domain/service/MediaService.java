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

            // 스토리 데이터 조회
            Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
            log.info("스토리데이터 조회 완료" + sceneDocumentOpt);
            if (sceneDocumentOpt.isEmpty()) {
                return CompletableFuture.failedFuture(
                        new RuntimeException("해당 스토리를 찾을 수 없습니다: " + storyId));
            }

            log.info("여기는?");
            //Optional 처리 위한 추가 과정
            SceneDocument sceneDocument = sceneDocumentOpt.get();

            //스토리 전체 데이터에서 scene 배열 가져오기
            List<Map<String, Object>> sceneArr = sceneDocument.getSceneArr();
            log.info("scene배열 가져오기");

            // 각 씬에 대한 미디어 처리 요청을 모아놓을 리스트
            List<CompletableFuture<Void>> sceneFutures = new ArrayList<>();
            log.info("병렬처리");

            //먼저 모든 오디오를 생성(한번에 처리)
            CompletableFuture<Void> audioFuture = CompletableFuture.runAsync(() -> {
                try {
                    log.info("전체 오디오 생성 시작: storyId={}", storyId);
                    audioService.generateAllAudios(storyId);
                    log.info("전체 오디오 생성 완료: storyId={}", storyId);
                } catch (Exception e) {
                    log.error("전체 오디오 생성 중 오류: storyId={}, error={}", storyId, e.getMessage(), e);
                    throw new RuntimeException("오디오 생성 실패: " + e.getMessage(), e);
                }
            });

            //오디오 처리가 완료된 후 이미지 생성 시작
            //thenCompose: 첫번 째 CompletableFuture(오디오)가 완료된 후 다른 CompletableFuture(이미지)를 실행하는 메서드
            sceneFutures.add(audioFuture.thenCompose(v -> {

                // 각 씬에 대해 이미지 생성 작업 생성
                List<CompletableFuture<Void>> imageFutures = sceneArr.stream()
                        .map(scene -> ((Number) scene.get("sceneId")).intValue())
                        .map(sceneId -> processSceneImage(storyId, sceneId))
                        .collect(Collectors.toList());

                // 모든 이미지 생성 작업이 완료될 때까지 대기
                return CompletableFuture.allOf(
                        imageFutures.toArray(new CompletableFuture[0])
                );
            }));

            // 모든 씬 처리가 완료되면 결과 반환
            return CompletableFuture.allOf(
                    sceneFutures.toArray(new CompletableFuture[0])
            ).thenRun(() -> {
                log.info("스토리 전체 씬 미디어 처리 완료: storyId={}", storyId);
                // 추가로직 구현 한다면 -> 모든 씬 처리 후 영상 합성 요청 등
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


    //이미지 생성 요청 메서드
    @Async("mediaTaskExecutor")
    public CompletableFuture<Void> processSceneImage(String storyId, Integer sceneId) {
        log.info("씬 이미지 처리 시작: storyId={}, sceneId={}", storyId, sceneId);

        try {
            // 이미지 생성 요청
            return imageService.generateImageForScene(storyId, sceneId)
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
