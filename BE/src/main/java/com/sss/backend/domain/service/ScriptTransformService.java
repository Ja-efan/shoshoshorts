package com.sss.backend.domain.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ScriptTransformService
 * - 원본 JSON 데이터를 특정 형식으로 변환하는 서비스
 * - storyId, storyTitle, characterArr, sceneArr를 변환하여 반환
 */
@Service
public class ScriptTransformService {

    /**
     * 원본 JSON 데이터를 변환하여 새로운 JSON 형식으로 반환
     *
     * @param originalJson 변환할 원본 JSON 데이터 (Map 형태)
     * @return 변환된 JSON 데이터 (Map 형태)
     */

    public Map<String, Object> transformScriptJson(Map<String, Object> originalJson) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> scriptJson = (Map<String, Object>) originalJson.get("script_json");

        if (scriptJson != null) {
            Map<String, Object> newScriptJson = new HashMap<>();
            newScriptJson.put("storyId", scriptJson.get("storyId")); // 스토리ID 저장
            newScriptJson.put("storyTitle", scriptJson.get("storyTitle")); // 스토리 제목 저장
            newScriptJson.put("characterArr", scriptJson.get("characterArr")); // 캐릭터 배열 저장

            // sceneArr 변환 및 저장
            List<Map<String, Object>> newSceneArr = transformSceneArr((List<Map<String, Object>>)scriptJson.get("sceneArr"));
            newScriptJson.put("sceneArr", newSceneArr);

            result.put("script_json", newScriptJson);
        }

        return result;
    }


    /**
     * sceneArr 변환 메서드
     * - 기존 sceneArr을 변환하여 새로운 리스트 생성
     * - sceneId를 추가하고, audioArr을 변환
     *
     * @param sceneArr 변환할 원본 sceneArr 리스트
     * @return 변환된 sceneArr 리스트
     */
    private List<Map<String, Object>> transformSceneArr(List<Map<String, Object>> sceneArr) {
        List<Map<String, Object>> newSceneArr = new ArrayList<>();

        for (int i = 0; i < sceneArr.size(); i++) {
            Map<String, Object> scene = sceneArr.get(i);
            Map<String, Object> newScene = new HashMap<>(scene);

            // sceneId 추가 ( 1부터 )
            newScene.put("sceneId", i + 1);

            // audioArr 변환 및 저장
            List<Map<String, Object>> newAudioArr = transformAudioArr((List<Map<String, Object>>) scene.get("audioArr"));
            newScene.put("audioArr", newAudioArr);

            newSceneArr.add(newScene);
        }

        return newSceneArr;
    }

    /**
     * audioArr 변환 메서드
     * - 기존 audioArr을 변환하여 새로운 리스트 생성
     * - emotionParams 값을 변환하여 포함
     *
     * @param audioArr 변환할 원본 audioArr 리스트
     * @return 변환된 audioArr 리스트
     */
    private List<Map<String, Object>> transformAudioArr(List<Map<String, Object>> audioArr) {
        List<Map<String, Object>> newAudioArr = new ArrayList<>();

//        for (Map<String, Object> audio : audioArr) {
//            Map<String, Object> newAudio = new HashMap<>(audio);

        for (int i=0; i< audioArr.size(); i++) {
            Map<String, Object> audio = audioArr.get(i); // 기존 audio 데이터
            Map<String, Object> newAudio = new HashMap<>(audio); // 원본 데이터 복사 (깊은 복사)

            // auidoId 추가 해주기
            newAudio.put("audioId", i+1);

            // emotionParams 변환 (누락된 감정 채워주기)
            Map<String, Object> emotionParams = (Map<String, Object>) audio.get("emotionParams");
            Map<String, Object> fullEmotionParams = fillEmotionParams(emotionParams);
            newAudio.put("emotionParams", fullEmotionParams);

            newAudioArr.add(newAudio);
        }

        return newAudioArr;
    }

    private Map<String, Object> fillEmotionParams(Map<String, Object> emotionParams) {
        String[] emotions = {"happiness", "sadness", "disgust", "fear", "surprise", "anger", "neutral"};
        Map<String, Object> fullParams = new HashMap<>();

        // 모든 감정값을 0으로 초기화
        for (String emotion : emotions) {
            fullParams.put(emotion, 0.0);
        }

        // 기존 값 복사
        if (emotionParams != null) {
            for (Map.Entry<String, Object> entry : emotionParams.entrySet()) {
                fullParams.put(entry.getKey(), entry.getValue());
            }
        }

        return fullParams;
    }
}