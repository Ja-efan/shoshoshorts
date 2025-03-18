package com.sss.backend.domain.service;

import com.sss.backend.api.dto.StoryRequestDTO;
import com.sss.backend.config.WebClientConfig;
import com.sss.backend.domain.document.CharacterDocument;
import com.sss.backend.domain.entity.StoryEntity;
import com.sss.backend.infrastructure.repository.CharacterRepository;
import com.sss.backend.infrastructure.repository.StoryRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StoryService {

    private final StoryRepository storyRepository;
    private final CharacterRepository characterRepository;
    private final WebClient webClient;

    // 생성자 주입
    public StoryService(StoryRepository storyRepository, CharacterRepository characterRepository, WebClient webClient) {
        this.storyRepository = storyRepository;
        this.characterRepository = characterRepository;
        this.webClient = webClient;
    }

    @Transactional
    public StoryEntity saveStory(StoryRequestDTO request) {
        // 유효성 검사 메서드 호출
        validateRequest(request);

        // RDBMS에 저장.
        StoryEntity storyEntity = convertToEntity(request);
        storyEntity = storyRepository.save(storyEntity);

        System.out.println("사용자 입력 인풋 :"+request);

        Long storyId = storyEntity.getId();
        System.out.println("저장된 storyId: "+storyId);

        // 캐릭터 정보가 있을 경우 MongoDB에 저장
        if (request.getCharacterArr() != null && !request.getCharacterArr().isEmpty()) {
            saveCharactersToMongoDB(storyId, request.getCharacterArr());
        }
        System.out.println("몽고디비 저장오나료");
        // FastAPI로 보낼 JSON 데이터 생성
        Map<String, Object> jsonData = createFastAPIJson(storyId, request.getTitle(), request.getStory());
        System.out.println("json변환까지 완료 : "+jsonData);
        // FastAPI에 보내기 // http://localhost:8000/script/convert/
        sendToFastAPI(jsonData)
                .subscribe(response -> System.out.println("FastAPI 응답 :"+ response));

        // response 유효성 검사

        return storyEntity;
    }

    // FastAPI API 호출 메소드 - webClient
    private Mono<Map<String, Object>> sendToFastAPI(Map<String, Object> jsonData) {
        System.out.println("이제 FastAPI에 쏴보자잉");
        // webclient 쓰기.
        return webClient.post()
                .uri("/script/convert/") // 엔드포인트 설정
                .bodyValue(jsonData) // JSON 데이터 포함
                .retrieve() // 응답 받기
//                .bodyToMono(Map.class) // 응답을 Map<String, Object>로 변환
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnSuccess(response -> System.out.println("FastAPI 응답 :"+response))
                .doOnError(error -> System.err.println("FastAPI요청실패:"+error.getMessage()));

    }

    // FastAPI로 보낼 JSON 데이터 생성 메소드
    private Map<String, Object> createFastAPIJson(Long storyId, @NotBlank String title, @NotBlank String story) {
        System.out.println("생성하기...");
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("storyId", storyId);
        jsonData.put("storyTitle", title);
        jsonData.put("story", formatStory(story)); // " 변환 처리, 줄바꿈이 필요한가?

        // MongoDB에서 캐릭터 정보 조회
        CharacterDocument characterDocument = getCharacterDocument(storyId);
        jsonData.put("characterArr", characterDocument != null ? characterDocument.getCharacterArr() : List.of());
        return jsonData;
    }
    // 쌍따옴표 처리 메소드
    private String formatStory(@NotBlank String story) {
        return story.replace("\"","\\\"");
    }

    // MongoDB에서 storyId로 characterArr 조회
    public CharacterDocument getCharacterDocument(Long storyId){
        return characterRepository.findByStoryId(String.valueOf(storyId))
                .orElseThrow(() -> new IllegalArgumentException("해당 storyId에 해당하는 캐릭터 정보가 존재하지 않습니다."));
    }

    // 유효성 검사 메서드
    private void validateRequest(StoryRequestDTO request){
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 비어 있을 수 없습니다.");
        }
        if (request.getStory() == null || request.getStory().trim().isEmpty()) {
            throw new IllegalArgumentException("스토리 내용은 비어 있을 수 없습니다.");
        }
        // 더 추가할 게 있을까요???
    }

    // DTO -> Entity 변환 메소드
    private StoryEntity convertToEntity(StoryRequestDTO request) {
        StoryEntity entity = new StoryEntity();
        entity.setTitle(request.getTitle());
        entity.setStory(request.getStory());
        return entity;
    }
    // MongoDB에 캐릭터 정보 저장하는 메소드.
    private void saveCharactersToMongoDB(Long storyId, List<Map<String, Object>> characterArr) {
        CharacterDocument characterDocument = new CharacterDocument();
        characterDocument.setStoryId(String.valueOf(storyId));
        characterDocument.setCharacterArr(characterArr);
        characterRepository.save(characterDocument);
    }
}
