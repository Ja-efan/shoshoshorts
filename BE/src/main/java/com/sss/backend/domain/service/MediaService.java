package com.sss.backend.domain.service;

import com.sss.backend.api.dto.SceneImageRequest;
import com.sss.backend.domain.document.SceneDocument;
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

    public MediaService(SceneDocumentRepository sceneDocumentRepository,ImageService imageService,AudioService audioService){
        this.sceneDocumentRepository = sceneDocumentRepository;
        this.imageService = imageService;
        this.audioService = audioService;
    }


    //모든 씬에 대한 미디어(오디오, 이미지) 생성 처리
    @Async("mediaTaskExecutor")
    public CompletableFuture<Void> processAllScenes(String storyId, String audioModelName,String imageModelName) {
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

            List<Map<String, Object>> sceneArr = sceneDocument.getSceneArr();
            log.info("scene배열 가져오기");

            // 오디오 처리 CompletableFuture
            CompletableFuture<Void> audioFuture = CompletableFuture.runAsync(() -> {
                //별도 스레드에서 실행될 오디오 처리 작업 정의
                try {
                    log.info("전체 오디오 생성 시작: storyId={}", storyId);
                    audioService.generateAllAudios(storyId,audioModelName);
                    log.info("전체 오디오 생성 완료: storyId={}", storyId);
                } catch (Exception e) {
                    log.error("전체 오디오 생성 중 오류: storyId={}, error={}", storyId, e.getMessage(), e);
                    throw new RuntimeException("오디오 생성 실패: " + e.getMessage(), e);
                }
            });

            // 이미지 처리를 위한 CompletableFuture 리스트
            // 각 씬의 이미지 처리 결과를 담을 리스트
            List<CompletableFuture<Void>> imageFutures = new ArrayList<>();

            // 각 씬에 대해 이미지 생성 작업 병렬로 생성
            for (Map<String, Object> scene : sceneArr) {
                int sceneId = ((Number) scene.get("sceneId")).intValue();
                //각 씬에 대해 이미지 생성 메서드 호출
                CompletableFuture<Void> imageFuture = imageService.generateImageForScene(storyId, sceneId, imageModelName)
                        //이미지 생성 완료 시 실행됨
                        .thenAccept(response -> {
                            log.info("씬 이미지 생성 완료: storyId={}, sceneId={}, url={}",
                                    storyId, sceneId, response.getImage_url());
                        // })
                        // .exceptionally(ex -> {
                        //     log.error("씬 이미지 처리 중 오류 발생: storyId={}, sceneId={}, error={}",
                        //             storyId, sceneId, ex.getMessage(), ex);
                        //     return null; //null반환 -> 실패해도 다른 이미지 처리는 계속 진행되도록 함
                });
                // 에러 핸들링은 allOf에서 일괄적으로 처리하도록 변경
                imageFutures.add(imageFuture);
            }

            // 모든 오디오와 이미지 처리가 완료될 때까지 대기할 CompletableFuture 배열
            CompletableFuture<Void>[] allFutures = new CompletableFuture[imageFutures.size() + 1];
            allFutures[0] = audioFuture; //첫번째는 오디오 처리 future
            for (int i = 0; i < imageFutures.size(); i++) {
                allFutures[i + 1] = imageFutures.get(i);
            }

            // 모든 퓨처를 하나로 결합
            // //allof: 모든 future가 완료될 때까지 대기하는 CompletableFuture
            // CompletableFuture<Void> allMediaFuture = CompletableFuture.allOf(allFutures);

            // // 이 CompletableFuture를 반환 - 이렇게 하면 future.get()을 호출할 때
            // // 모든 미디어 처리가 완료될 때까지 대기함
            // return allMediaFuture.thenRun(() -> {
            //     log.info("스토리 전체 씬 미디어 처리 완료: storyId={}", storyId);
            // }).exceptionally(ex -> {
            //     log.error("스토리 전체 씬 미디어 처리 중 오류 발생: storyId={}, error={}",
            //             storyId, ex.getMessage(), ex);
            //     return null;
            // });
            // 하나라도 실패하면 전체가 실패하도록 설정
            return CompletableFuture.allOf(allFutures)
                .thenRun(() -> {
                    log.info("스토리 전체 씬 미디어 처리 완료: storyId={}", storyId);
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
