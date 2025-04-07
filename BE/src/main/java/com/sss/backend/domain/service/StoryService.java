package com.sss.backend.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sss.backend.api.dto.StoryRequestDTO;
import com.sss.backend.domain.document.CharacterDocument;
import com.sss.backend.domain.document.SceneDocument;
import com.sss.backend.domain.entity.Story;
import com.sss.backend.domain.entity.Users;
import com.sss.backend.domain.repository.CharacterRepository;
import com.sss.backend.domain.repository.StoryRepository;

import com.sss.backend.domain.repository.UserRepository;
import com.sss.backend.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import com.sss.backend.domain.entity.VideoProcessingStep;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final CharacterRepository characterRepository;
    private final WebClient webClient;
    private final ScriptTransformService scriptTransformService;
    private final JWTUtil jwtUtil;
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final VideoProcessingStatusService videoProcessingStatusService;

    @Value("${api.password}")
    private String apiPassword;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // 생성자 주입
    @Transactional
    public Long saveBasicStory(StoryRequestDTO request,
                               HttpServletRequest httpRequest) {
        // 1. 유효성 검사 메서드 호출
        validateRequest(request);
        log.info("사용자 입력 인풋 :{}",request);
        // 1.5 유저 정보 추출
        String token = jwtUtil.extractTokenFromRequest(httpRequest);
        String email = jwtUtil.getEmail(token);
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저 정보 없음"));

        // 2. RDBMS에 스토리 저장.
        Story storyEntity = convertToEntity(request, user);
        storyEntity = storyRepository.save(storyEntity);
        log.info("엔티티 :{}", storyEntity);

        Long storyId = storyEntity.getId();
        log.info("저장된 storyId :{}",storyId);


        // 3. 캐릭터 정보가 있을 경우 MongoDB에 저장
        if (request.getCharacterArr() != null && !request.getCharacterArr().isEmpty()) {
            saveCharactersToMongoDB(storyId, request.getCharacterArr());
            log.info("Character 정보 몽고 디비 저장완료");
        } else {
            log.info("Character 정보가 없습니다.");
        }

        return storyId;

    }

    @Transactional
    public void saveStory(Long storyId, StoryRequestDTO request) {

        // 4. FastAPI로 보낼 JSON 데이터 생성
        Map<String, Object> jsonData = createFastAPIJson(storyId, request.getTitle(), request.getStory(), request.getNarVoiceCode());
        log.info("json변환까지 완료 {}",jsonData);

        // 5. FastAPI에 요청 및 응답 처리 // http://localhost:8000/script/convert/

        try {
            Map<String, Object> response = sendToFastAPI(jsonData).block();
            log.info("FastAPI 응답 {}",response);

            // 변환 작업 실행
            Map<String, Object> transformedJson = scriptTransformService.transformScriptJson(response);

            // 캐릭터 정보가 없을 경우 기본 캐릭터 정보 추가
            addDefaultCharactersIfNeeded(transformedJson);

            saveScenesToMongoDB(transformedJson);
        } catch (Exception e) {
            System.out.println("FastAPI 요청 실패 :"+ e.getMessage());
            log.info("fastapi 에러 {}",e.getMessage());
        }
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
        String combinedPwd = activeProfile + apiPassword;

        return webClient.post()
                .uri("/script/convert") // 엔드포인트 설정
                .header("apiPwd",combinedPwd)
                .bodyValue(jsonData) // JSON 데이터 포함
                .retrieve() // 응답 받기
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    // FastAPI로 보낼 JSON 데이터 생성 메소드
    private Map<String, Object> createFastAPIJson(Long storyId, String title, String story, String narVoiceCode) {
        System.out.println("생성하기...");
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("storyId", storyId);
        jsonData.put("storyTitle", title);
        jsonData.put("story", formatStory(story)); // " 변환 처리, 줄바꿈이 필요한가?
        if (narVoiceCode != null){
            jsonData.put("narVoiceCode", narVoiceCode);
        }
        log.info("character 넣기 전 json  : \n {}",jsonData);

        // MongoDB에서 캐릭터 정보 조회
//        CharacterDocument characterDocument = getCharacterDocument(storyId);
//        jsonData.put("characterArr", characterDocument != null ? characterDocument.getCharacterArr() : List.of());
        Optional<CharacterDocument> characterDocument = characterRepository.findByStoryId(String.valueOf(storyId));

        jsonData.put("characterArr", characterDocument.map(CharacterDocument::getCharacterArr).orElse(List.of()));

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
    private Story convertToEntity(StoryRequestDTO request, Users user) {
        Story entity = new Story();
        entity.setTitle(request.getTitle());
        entity.setStory(request.getStory());
        entity.setUser(user);
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
                .set("narVoiceCode", scriptJson.get("narVoiceCode"))
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

    /**
     * 스토리 저장 및 처리 상태 업데이트
     */
    public void saveStoryWithProcessingStatus(Long storyId, StoryRequestDTO request) {
        // 스크립트 처리 단계 설정
        videoProcessingStatusService.updateProcessingStep(storyId.toString(), VideoProcessingStep.SCRIPT_PROCESSING);

        // 기존 스토리 저장 로직 수행
        saveStory(storyId, request);
    }

    /**
     * 캐릭터 정보가 없는 경우 기본 캐릭터 정보를 생성하여 추가하는 메소드
     * audioArr에 나오는 각 character의 이름으로 기본 캐릭터를 생성한다
     * @param transformedJson FastAPI 응답 변환 후 JSON
     */
    private void addDefaultCharactersIfNeeded(Map<String, Object> transformedJson) {
        if (transformedJson == null) {
            return;
        }

        Map<String, Object> scriptJson = (Map<String, Object>) transformedJson.get("script_json");
        if (scriptJson == null) {
            return;
        }

        // 캐릭터 배열 확인
        List<Map<String, Object>> characterArr = (List<Map<String, Object>>) scriptJson.get("characterArr");

        // 캐릭터 정보가 없거나 비어있는 경우에만 기본 캐릭터 생성
        if (characterArr == null || characterArr.isEmpty()) {
            log.info("캐릭터가 비어있어 기본 캐릭터를 생성합니다.");
            List<Map<String, Object>> sceneArr = (List<Map<String, Object>>) scriptJson.get("sceneArr");
            String narVoiceCode = (String) scriptJson.get("narVoiceCode");

            if (sceneArr != null && !sceneArr.isEmpty()) {
                // 캐릭터 이름을 저장할 Set (중복 제거)
                java.util.Set<String> characterNames = new java.util.HashSet<>();

                // 모든 씬의 오디오 배열에서 캐릭터 이름 추출
                for (Map<String, Object> scene : sceneArr) {
                    List<Map<String, Object>> audioArr = (List<Map<String, Object>>) scene.get("audioArr");
                    if (audioArr != null) {
                        for (Map<String, Object> audio : audioArr) {
                            String character = (String) audio.get("character");
                            // "narration"이 아닌 실제 캐릭터 이름만 추가
                            if (character != null && !character.equals("narration")) {
                                characterNames.add(character);
                            }
                        }
                    }
                }

                // 추출된 캐릭터가 없는 경우 기본 캐릭터 "주인공" 추가
                if (characterNames.isEmpty()) {
                    characterNames.add("나");
                    log.info("대화 캐릭터가 없어 기본 '나' 캐릭터를 추가합니다.");
                }

                // 새 캐릭터 배열 생성
                List<Map<String, Object>> newCharacterArr = new java.util.ArrayList<>();

                // 추출된 캐릭터 이름으로 기본 캐릭터 정보 생성
                for (String name : characterNames) {
                    Map<String, Object> character = new java.util.HashMap<>();
                    character.put("name", name);
                    // 성별 랜덤 지정 (남자 또는 여자)
                    character.put("gender", new java.util.Random().nextBoolean() ? "남자" : "여자");
                    character.put("properties", "검은 머리, 검은 눈");
                    character.put("voiceCode", narVoiceCode);

                    newCharacterArr.add(character);
                }

                // 생성된 기본 캐릭터 정보를 JSON에 설정
                scriptJson.put("characterArr", newCharacterArr);
                log.info("캐릭터 정보가 없어 기본 캐릭터 생성: {}", newCharacterArr);
            }
        }
    }
}
