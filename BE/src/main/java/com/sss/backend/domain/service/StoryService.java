package com.sss.backend.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sss.backend.api.dto.StoryRequestDTO;
import com.sss.backend.domain.document.CharacterDocument;
import com.sss.backend.domain.document.SceneDocument;
import com.sss.backend.domain.entity.Story;
import com.sss.backend.domain.repository.CharacterRepository;
import com.sss.backend.domain.repository.SceneRepository;
import com.sss.backend.domain.repository.StoryRepository;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class StoryService {

    private final StoryRepository storyRepository;
    private final CharacterRepository characterRepository;
    private final WebClient webClient;
    private final ScriptTransformService scriptTransformService;
    private final SceneRepository sceneRepository;
    private final MongoTemplate mongoTemplate;

    // 생성자 주입
    public StoryService(StoryRepository storyRepository,
                        CharacterRepository characterRepository,
                        WebClient webClient,
                        ScriptTransformService scriptTransformService,
                        SceneRepository sceneRepository,
                        MongoTemplate mongoTemplate) {
        this.storyRepository = storyRepository;
        this.characterRepository = characterRepository;
        this.webClient = webClient;
        this.scriptTransformService = scriptTransformService;
        this.sceneRepository = sceneRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Transactional
    public Long saveStory(StoryRequestDTO request) {
        // 1. 유효성 검사 메서드 호출
        validateRequest(request);

        // 2. RDBMS에 스토리 저장.
        Story storyEntity = convertToEntity(request);
        storyEntity = storyRepository.save(storyEntity);
        System.out.println("사용자 입력 인풋 :"+request);
        log.info("사용자 입력 인풋 :{}",request);

        Long storyId = storyEntity.getId();
        System.out.println("저장된 storyId: "+storyId);

        // 3. 캐릭터 정보가 있을 경우 MongoDB에 저장
        if (request.getCharacterArr() != null && !request.getCharacterArr().isEmpty()) {
            saveCharactersToMongoDB(storyId, request.getCharacterArr());
        }
        System.out.println("몽고디비 저장완료..");
        log.info("몽고 디비 저장완료");

        // 4. FastAPI로 보낼 JSON 데이터 생성
        Map<String, Object> jsonData = createFastAPIJson(storyId, request.getTitle(), request.getStory());
        System.out.println("json변환까지 완료 : "+jsonData);
        log.info("json변환까지 완료 {}",jsonData);

        // 5. FastAPI에 요청 및 응답 처리 // http://localhost:8000/script/convert/

            // 아래 reactive programming에서는 파이프라인이 구성될 뿐 실제 실행은 subscribe에서..
        try {
            Map<String, Object> response = sendToFastAPI(jsonData).block();
            System.out.println("FastAPI 응답 : "+response);
            log.info("FastAPI 응답 {}",response);

            // 변환 작업 실행
            Map<String, Object> transformedJson = scriptTransformService.transformScriptJson(response);


            saveScenesToMongoDB(transformedJson);
        } catch (Exception e) {
            System.out.println("FastAPI 요청 실패 :"+ e.getMessage());
            log.info("fastapi 에러 {}",e.getMessage());
        }

        return storyId;
    }

    // Dummy json getter
    private Map<String, Object> getDummyJson() {
        String jsonString = """
                {
                  "script_json": {
                    "storyId": 1,
                    "storyTitle": "운명을 믿으시나요?",
                    "characterArr": [
                      {
                        "name": "나",
                        "gender": "남자",
                        "properties": "흑발에 검은 눈. 한국인. 여자를 도와주고 결혼까지 한다."
                      },
                      {
                        "name": "아내",
                        "gender": "여자",
                        "properties": "갈색 머리에 긴 장발. 한국인. 외국에서 기차표를 잘못 샀다가 내가 도와주었다."
                      }
                    ],
                    "sceneArr": [
                      {
                        "audioArr": [
                          {
                            "text": "외국 여행 갔을 때 기차역에서 있었던 일이야.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "neutral",
                            "emotionParams": {
                              "neutral": 1
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "내 앞에 어떤 여자가 역무원이랑 얘기하다가 진짜 멘붕 온 표정으로 서 있는 거야.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "surprise",
                            "emotionParams": {
                              "surprise": 1
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "듣다 보니까 기차표를 잘못 사서 지금 기차를 못 탄다는 거였는데 문제는 역무원이 영어를 아예 못 한다는 거지.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "worry",
                            "emotionParams": {
                              "fear": 0.5,
                              "neutral": 0.5
                            }
                          }
                        ]
                      }                   
                    ]
                  }
                }
            """;
        try {
            // JSON 문자열을 Map<String, Object>로 변환
            ObjectMapper objectMapper = new ObjectMapper(); // Java에서 JSON을 다룰 때 사용하는 라이브러리.
            return objectMapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(); // 변환 실패 시 빈 Map 반환
        }
    }

    // FastAPI API 호출 메소드 - webClient
    private Mono<Map<String, Object>> sendToFastAPI(Map<String, Object> jsonData) {
        System.out.println("이제 FastAPI에 쏴보자잉");
        return webClient.post()
                .uri("/script/convert") // 엔드포인트 설정
                .bodyValue(jsonData) // JSON 데이터 포함
                .retrieve() // 응답 받기
//                .bodyToMono(Map.class) // 응답을 Map<String, Object>로 변환
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    // FastAPI로 보낼 JSON 데이터 생성 메소드
    private Map<String, Object> createFastAPIJson(Long storyId, String title, String story) {
        System.out.println("생성하기...");
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("storyId", storyId);
        jsonData.put("storyTitle", title);
        jsonData.put("story", formatStory(story)); // " 변환 처리, 줄바꿈이 필요한가?
        System.out.println("좀 안되나??"+jsonData);

        // MongoDB에서 캐릭터 정보 조회
//        CharacterDocument characterDocument = getCharacterDocument(storyId);
//        jsonData.put("characterArr", characterDocument != null ? characterDocument.getCharacterArr() : List.of());
        Optional<CharacterDocument> characterDocument = characterRepository.findByStoryId(String.valueOf(storyId));

        jsonData.put("characterArr", characterDocument.map(CharacterDocument::getCharacterArr).orElse(List.of()));
        System.out.println("여기가 문제인가");

        return jsonData;
    }
    // 쌍따옴표 처리 메소드
    private String formatStory(@NotBlank String story) {
        return story.replace("\"","\\\"");
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
    private Story convertToEntity(StoryRequestDTO request) {
        Story entity = new Story();
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

    // MongoDB에 Scenes 정보 저장하는 메소드`

    /**
     * upsert 메소드 사용 : query id가 있다면, 새로 만들고, 없으면 해당 필드만 업데이트.
     *
     * @param response
     */
    private void saveScenesToMongoDB(Map<String,Object> response) {
        Map<String, Object> scriptJson = (Map<String, Object>) response.get("script_json");
        if (scriptJson == null) {
            System.err.println("MongoDB 저장 실패: script_json이 존재하지 않습니다.");
            return;
        }

        String storyId = scriptJson.get("storyId").toString();

        // MongoDB Upsert 처리
        Query query = new Query(Criteria.where("storyId").is(storyId));
        Update update = new Update()
                .set("storyTitle", scriptJson.get("storyTitle"))
                .set("characterArr", scriptJson.get("characterArr"))
                .set("sceneArr", scriptJson.get("sceneArr"));

        mongoTemplate.upsert(query, update, SceneDocument.class);

//        SceneDocument sceneDocument = new SceneDocument();
//        sceneDocument.setStoryId((scriptJson.get("storyId").toString()));
//        sceneDocument.setStoryTitle((String) scriptJson.get("storyTitle"));
//        sceneDocument.setCharacterArr((List<Map<String, Object>>) scriptJson.get("characterArr")); // 🔹 캐릭터 배열 추가
//        sceneDocument.setSceneArr((List<Map<String, Object>>) scriptJson.get("sceneArr"));

//        sceneRepository.save(sceneDocument);
        System.out.println("MongoDB scenes 컬렉션 저장 완료! (characterArr 포함)");

    }
}
