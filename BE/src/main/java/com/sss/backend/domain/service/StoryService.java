package com.sss.backend.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sss.backend.api.dto.StoryRequestDTO;
import com.sss.backend.config.WebClientConfig;
import com.sss.backend.domain.document.CharacterDocument;
import com.sss.backend.domain.document.SceneDocument;
import com.sss.backend.domain.entity.StoryEntity;
import com.sss.backend.infrastructure.repository.CharacterRepository;
import com.sss.backend.infrastructure.repository.SceneRepository;
import com.sss.backend.infrastructure.repository.StoryRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
import java.util.Optional;

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
    public StoryEntity saveStory(StoryRequestDTO request) {
        // 1. 유효성 검사 메서드 호출
        validateRequest(request);

        // 2. RDBMS에 스토리 저장.
        StoryEntity storyEntity = convertToEntity(request);
        storyEntity = storyRepository.save(storyEntity);
        System.out.println("사용자 입력 인풋 :"+request);

        Long storyId = storyEntity.getId();
        System.out.println("저장된 storyId: "+storyId);

        // 3. 캐릭터 정보가 있을 경우 MongoDB에 저장
        if (request.getCharacterArr() != null && !request.getCharacterArr().isEmpty()) {
            saveCharactersToMongoDB(storyId, request.getCharacterArr());
        }
        System.out.println("몽고디비 저장완료..");

        // 4. FastAPI로 보낼 JSON 데이터 생성
        Map<String, Object> jsonData = createFastAPIJson(storyId, request.getTitle(), request.getStory());
        System.out.println("json변환까지 완료 : "+jsonData);


        // 5. FastAPI에 요청 및 응답 처리 // http://localhost:8000/script/convert/

            // 아래 reactive programming에서는 파이프라인이 구성될 뿐 실제 실행은 subscribe에서..
        sendToFastAPI(jsonData)
                // onerror : 에러가 발생하면 DummyJson 호출.
                .onErrorResume(error -> {
                    System.err.println("FastAPI 요청 실패: "+error.getMessage());
                    System.out.println("로컬 script.json 파일을 불러옵니다.");
                    return Mono.just(getDummyJson()); // Mono로 감싸줌.
                })
                    // flatMap :앞단계의 response를 받아서 새로운 Mono 반환
                    // Mono를 반환하려면 Map 대신 flatMap을 써야 함.
                .flatMap(response -> {
                    Map<String, Object> transformedJson = scriptTransformService.transformScriptJson(response);
                    return Mono.just(transformedJson);  // 변환된 JSON을 Mono로 감싸서 넘김.
                })
                .subscribe(response -> {
                    System.out.println("FastAPI 응답 :"+ response);
                    saveScenesToMongoDB(response); // MongoDB 저장하기.
                    // 정현씨 service 호출..
                    // ####### ho chuuuuu L ;
                });

        return storyEntity;
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
                      },
                      {
                        "audioArr": [
                          {
                            "text": "내가 그 상황 보다가 좀 답답해서 그냥 끼어들었어.",
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
                            "text": "현지 언어로 상황 설명 했더니 역무원이 알았다는 듯이 바로 기차표를 바꿔 주더라.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "relief",
                            "emotionParams": {
                              "happiness": 0.5,
                              "neutral": 0.5
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "땡큐 쏘 머치!",
                            "type": "dialogue",
                            "character": "아내",
                            "emotion": "gratitude",
                            "emotionParams": {
                              "happiness": 1
                            }
                          },
                          {
                            "text": "유어 웰컴!",
                            "type": "dialogue",
                            "character": "나",
                            "emotion": "neutral",
                            "emotionParams": {
                              "neutral": 1
                            }
                          },
                          {
                            "text": "이제 가려는데, 그 여자가 혼잣말로 와... 진짜 다행이다... 라고 하는거야.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "relief",
                            "emotionParams": {
                              "happiness": 0.5,
                              "neutral": 0.5
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "어? 한국분이세요?",
                            "type": "dialogue",
                            "character": "나",
                            "emotion": "surprise",
                            "emotionParams": {
                              "surprise": 1
                            }
                          },
                          {
                            "text": "헐! 한국 분이셨구나! 진짜 감사해요!",
                            "type": "dialogue",
                            "character": "아내",
                            "emotion": "surprise",
                            "emotionParams": {
                              "surprise": 0.5,
                              "happiness": 0.5
                            }
                          },
                          {
                            "text": "뭔가 급 친해진 느낌?",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "happiness",
                            "emotionParams": {
                              "happiness": 0.7,
                              "neutral": 0.3
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "그러다 어쩌다 보니 같이 기차 타고 얘기하면서 진짜 재밌는 시간을 보냈어.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "happiness",
                            "emotionParams": {
                              "happiness": 0.8,
                              "neutral": 0.2
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "내릴 때 여자가 한국 돌아가면 꼭 한번 만나요 하면서 연락처를 주더라고.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "anticipation",
                            "emotionParams": {
                              "happiness": 0.6,
                              "neutral": 0.4
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "솔직히 그냥 별 생각 없었는데, 한국 들어오는 날 카톡이 딱 온 거야.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "surprise",
                            "emotionParams": {
                              "surprise": 0.7,
                              "neutral": 0.3
                            }
                          },
                          {
                            "text": "혹시 오늘 한국 들어오시는 날 맞죠? 그때 진짜 감사했어요! 시간 괜찮으시면 밥 같이 먹을래요?",
                            "type": "dialogue",
                            "character": "아내",
                            "emotion": "gratitude",
                            "emotionParams": {
                              "happiness": 0.6,
                              "neutral": 0.4
                            }
                          },
                          {
                            "text": "뭔가 싶어서 일단 나갔지.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "curiosity",
                            "emotionParams": {
                              "neutral": 0.8,
                              "surprise": 0.2
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "근데 그게 밥 한끼가 두끼 되고, 영화 한 편이 두 편되고 결론은???",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "happiness",
                            "emotionParams": {
                              "happiness": 0.9,
                              "neutral": 0.1
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "지금 같이 살고 있고, 애도 있어.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "happiness",
                            "emotionParams": {
                              "happiness": 1
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "아내가 가끔 그때 얘기하면 그때 당신, 나한테 백마탄 왕자님이였어! 이러는데, 내가 먼저 꼬셨다고 주장하더라.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "amusement",
                            "emotionParams": {
                              "happiness": 0.7,
                              "neutral": 0.3
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "여행에서 이렇게 인생 파트너 만날 줄 누가 알았겠냐?",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "wonder",
                            "emotionParams": {
                              "surprise": 0.6,
                              "neutral": 0.4
                            }
                          }
                        ]
                      },
                      {
                        "audioArr": [
                          {
                            "text": "바뀌었나요!!?!?!",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "advice",
                            "emotionParams": {
                              "neutral": 0.8,
                              "happiness": 0.2
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

    // MongoDB에서 storyId로 characterArr 조회
//    public CharacterDocument getCharacterDocument(Long storyId){
//        return characterRepository.findByStoryId(String.valueOf(storyId))
////                .orElse(new CharacterDocument(String.valueOf(storyId), List.of()));
//                    // 항상 객체 생성. 일단 생성하고 봄.
//                .orElseGet(() -> new CharacterDocument(String.valueOf(storyId), List.of()));
//                    // Optional이 비어 있을 때만 객체 생성
////                .orElseThrow(() -> new IllegalArgumentException("해당 storyId에 해당하는 캐릭터 정보가 존재하지 않습니다."));
//    }

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
