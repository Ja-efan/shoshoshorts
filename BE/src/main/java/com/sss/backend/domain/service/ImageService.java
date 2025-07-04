package com.sss.backend.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import com.sss.backend.api.dto.SceneImageRequest;
import com.sss.backend.api.dto.SceneImageResponse;
import com.sss.backend.config.AppProperties;
import com.sss.backend.config.S3Config;
import com.sss.backend.domain.document.SceneDocument;
import com.sss.backend.domain.entity.Story;
import com.sss.backend.domain.repository.SceneDocumentRepository;

import com.sss.backend.domain.repository.StoryRepository;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;

@Service
@Slf4j
public class ImageService {

    private final WebClient webClient;
    private final AppProperties appProperties;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper; //JSON 데이터 변환에 사용
    private final SceneDocumentRepository sceneDocumentRepository;
    private final StoryRepository storyRepository;
    private final S3Config s3Config;

    @Value("${api.password}")
    private String apiPassword;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${image.cling.api.url}")
    private String clingApiUrl;

    @Value("${image.stable.api.url}")
    private String stableApiUrl;



    public ImageService(WebClient webClient, AppProperties appProperties,
                        MongoTemplate mongoTemplate, ObjectMapper objectMapper,
                        SceneDocumentRepository sceneDocumentRepository, StoryRepository storyRepository,
                        S3Config s3Config) {
        this.webClient = webClient;
        this.appProperties = appProperties;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.sceneDocumentRepository = sceneDocumentRepository;
        this.storyRepository = storyRepository;
        this.s3Config = s3Config;
    }


    //씬 이미지 생성 요청
    @Async("imageTaskExecutor")
    public CompletableFuture<SceneImageResponse> generateImageForScene(String storyId, Integer sceneId, String imageModelName) {
        log.info("씬 이미지 생성 시작: storyId={}, sceneId={}", storyId, sceneId);

        try {

//            try {
//                Thread.sleep(500); // 500ms 지연
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                throw new RuntimeException("이미지 생성 지연 중 중단됨", e);
//            }


            // 스토리 문서 조회
            Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
            if (sceneDocumentOpt.isEmpty()) {
                return CompletableFuture.failedFuture(
                        new RuntimeException("해당 스토리를 찾을 수 없습니다: " + storyId));
            }

            SceneDocument sceneDocument = sceneDocumentOpt.get();

            // 해당 씬 찾기
            Map<String, Object> targetScene = findScene(sceneDocument, sceneId);

            //요청 객체 준비
            SceneImageRequest imageRequest = prepareImageRequest(sceneDocument, targetScene, sceneId, storyId);

            // 이미지 생성 API 호출
            return generateImage(imageRequest, storyId, imageModelName);
        } catch (Exception e) {
            log.error("씬 이미지 생성 준비 중 오류 발생: storyId={}, sceneId={}, error={}",
                    storyId, sceneId, e.getMessage(), e);
            return CompletableFuture.failedFuture(
                new RuntimeException("씬 이미지 생성 실패(storyId=" + storyId + ", sceneId=" + sceneId + "): " + e.getMessage(), e)
            );
        }
    }

    //해당하는 씬 찾기
    private Map<String, Object> findScene(SceneDocument sceneDocument, Integer sceneId) {
        return sceneDocument.getSceneArr().stream()
                .filter(scene -> ((Number) scene.get("sceneId")).intValue() == sceneId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("해당 씬을 찾을 수 없습니다: sceneId=" + sceneId));
    }



    //request에 storyId 추가된 버전
    //이미지 생성 요청 객체 준비
    private SceneImageRequest prepareImageRequest(SceneDocument sceneDocument, Map<String, Object> targetScene, Integer sceneId, String storyId) {

        // 이미지 생성 요청 객체 생성
        SceneImageRequest imageRequest = new SceneImageRequest();
        imageRequest.setSceneId(sceneId);

        // StoryMetadata 설정
        SceneImageRequest.StoryMetadata storyMetadata = new SceneImageRequest.StoryMetadata();
        storyMetadata.setStory_id(Integer.parseInt(storyId));
        storyMetadata.setTitle(sceneDocument.getStoryTitle());

        //사용자가 입력한 이야기 요청에 추가
        String story = storyRepository.findStoryById(Long.parseLong(storyId));
        storyMetadata.setOriginal_story(story);


        // Characters 설정
        List<Map<String, Object>> characterMapList = sceneDocument.getCharacterArr();
        List<SceneImageRequest.Character> characters = new ArrayList<>();

        for (Map<String, Object> charMap : characterMapList) {
            SceneImageRequest.Character character = new SceneImageRequest.Character();
            character.setName((String) charMap.get("name"));

            // gender가 String이면 Integer로 변환 (예: "남자" :0, "여자":1)
            String genderStr = (String) charMap.get("gender");
            if(genderStr.equals("남성") || genderStr.equals("남자") || genderStr.equals("1")){
                character.setGender(0);
            }else if(genderStr.equals("여성") || genderStr.equals("여자") || genderStr.equals("2")){
                character.setGender(1);
            }

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

        return imageRequest;
    }




    //이미지 생성 모델 API 호출
    @Async("imageTaskExecutor")
    public CompletableFuture<SceneImageResponse> generateImage(SceneImageRequest sceneRequest, String storyId, String imageModelName) {
        log.info("이미지 생성 API 호출 - 씬 ID: {}", sceneRequest.getSceneId());

        try {

            String apiUrl = "";
            if(imageModelName.equals("Kling")){
                apiUrl = clingApiUrl;
                log.info(imageModelName + "---------------------clingAI 모델 사용?");
            }else{
                apiUrl = stableApiUrl;
                log.info(imageModelName + "------------------------stableDiffusion 모델 사용?");
            }

            // API 호출 (오류 응답 로깅 추가)
            return webClient
                    .post()
                    .uri(apiUrl)
                    .header("apiPwd", activeProfile + apiPassword)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(sceneRequest)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                            return response.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("API 오류 응답: 씬 ID={}, 응답={}", sceneRequest.getSceneId(), errorBody);
                                        return Mono.error(new RuntimeException("이미지 API 요청 실패(씬 ID=" +
                                                sceneRequest.getSceneId() + "): " + errorBody));
                                    });
                        } else {
                            return response.bodyToMono(SceneImageResponse.class);
                        }
                    })
                    .doOnNext(response -> {
                        log.info("이미지 생성 완료 - 씬 ID: {}, URL: {}",
                                sceneRequest.getSceneId(), response.getImage_url());

                        // 이미지 정보를 MongoDB에 저장
                        saveImageToMongoDB(storyId, response);
                    })
                    // .doOnError(e ->
                    //         log.error("이미지 생성 API 호출 중 오류 발생 - 씬 ID: {}, 오류: {}",
                    //                 sceneRequest.getSceneId(), e.getMessage(), e)
                    // )
                    .toFuture();

        } catch (Exception e) {
            log.error("이미지 생성 요청 처리 중 예외 발생 - 씬 ID: {}, 오류: {}",
                    sceneRequest.getSceneId(), e.getMessage(), e);
            return CompletableFuture.failedFuture(
                new RuntimeException("이미지 생성 요청 실패(씬 ID=" + sceneRequest.getSceneId() + "): " + e.getMessage(), e)
            );
        }
    }

    //이미지 MongoDB에 저장
    private void saveImageToMongoDB(String storyId, SceneImageResponse response) {
        try {
            log.info("이미지 MongoDB 저장 시작 - 스토리 ID: {}, 씬 ID: {}, 이미지 URL: {}",
                    storyId, response.getScene_id(), response.getImage_url());

            Query query = Query.query(Criteria.where("storyId").is(storyId));
            query.addCriteria(Criteria.where("sceneArr.sceneId").is(response.getScene_id()));

            String newUrl = response.getImage_url();

            // sceneId가 1이면, response에서 url 받아와서 사이즈 줄이고, s3 업로드 후에 해당 url 저장.
            if ("1".equals(String.valueOf(response.getScene_id()))){
                // S3 key 추출
                String s3Key = s3Config.extractS3KeyFromUrl(response.getImage_url());
                // presignedUrl 생성
                String presignedURL = s3Config.generatePresignedUrl(s3Key);
                // 이미지 다운로드
                BufferedImage originalImage = ImageIO.read(new URL(presignedURL));

                // 임시 파일 경로 지정
                File compressedImageFile = File.createTempFile("compressed_",".jpg");

                // 4. Thumbnails 사용해 리사이징 or 압축 (품질 낮추기만 함)
                Thumbnails.of(originalImage)
                        .scale(1.0) // 사이즈는 그대로
                        .outputQuality(0.3) // 30% 품질 (용량 1/3 수준)
                        .outputFormat("jpg")
                        .toFile(compressedImageFile);


                // 5. 다시 S3 업로드 (경로는 기존과 동일하거나 새 키로)
                String newS3Key = s3Key.replace(".jpg", "_compressed.jpg");
                s3Config.uploadToS3(compressedImageFile.getAbsolutePath(), newS3Key);

                // 6. 새로운 URL 만들기 (선택)
                newUrl = "https://" + s3Config.getBucketName() +
                        ".s3." + s3Config.getRegion() +
                        ".amazonaws.com/" + newS3Key;


            }

            // MongoDB에 이미지 정보 저장 (해당 씬에 이미지 정보 추가)
            Update update = new Update(); //특정 필드를 업데이트할 객체 생성

            // $를 사용하여 배열 내 조건에 맞는 요소 업데이트
            update.set("sceneArr.$.image_prompt", response.getImage_prompt());
            update.set("sceneArr.$.image_url", newUrl);

            mongoTemplate.updateFirst(query, update, "scenes");


            log.info("이미지 정보 MongoDB 저장 완료 - 스토리 ID: {}, 씬 ID: {}, 저장값 : {}",
                    storyId, response.getScene_id(), update);
        } catch (Exception e) {
            log.error("이미지 정보 MongoDB 저장 중 오류 발생 - 씬 ID: {}, 오류: {}",
                    response.getScene_id(), e.getMessage(), e);
        }
    }

}
