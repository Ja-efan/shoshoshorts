import os
import json
from openai import OpenAI
from dotenv import load_dotenv
from app.schema.script import ScriptRequest, ScriptResponse

# json 구조 유효성 검사
# https://jsonlint.com/
# .env 파일에서 환경 변수 로드
load_dotenv()

# OpenAI API 키 가져오기
api_key = os.getenv("OPENAI_API_KEY")
if not api_key:
    raise ValueError("OPENAI_API_KEY가 .env 파일에 설정되어 있지 않습니다.")

# OpenAI 클라이언트 초기화
client = OpenAI(api_key=api_key)

class ScriptService:
    async def generate_script_json(self, request: ScriptRequest) -> ScriptResponse:
        """
        스토리 내용을 JSON 형식으로 변환하는 함수
        """
        
        # request에 담긴 characterArr 중 name 값을 모아서 배열로 만들기
        character_names = [character.name for character in request.characterArr]

        try:
            # JSON 스키마 정의
            json_schema = {
                "storyId": {
                    "type": "integer",
                    "description": "스토리 고유 식별자 (AutoSet)"
                },
                "storyTitle": {
                    "type": "string",
                    "description": "스토리 제목"
                },
                "characterArr": {
                    "type": "array",
                    "description": "스토리에 등장하는 캐릭터 목록",
                    "items": {
                        "name": {
                            "type": "string",
                            "description": "캐릭터 이름"
                        },
                        "gender": {
                            "type": "string",
                            "description": "캐릭터 성별"
                        },
                        "properties": {
                            "type": "string",
                            "description": "캐릭터 특성 정보"
                        }
                    }   
                },
                "sceneArr": {
                    "type": "array",
                    "description": "씬 목록",
                    "items": {
                        "audioArr": {
                            "type": "array",
                            "description": "한 장면에 나오는 구분되는 TTS나 사운드 재생에 필요한 정보를 담는 필드",
                            "items": {
                                "text": {
                                    "type": "string",
                                    "description": "Scene 텍스트를 분할하였을 때 각 장면의 텍스트를 담는 필드"
                                },
                                "type": {
                                    "type": "string",
                                    "enum": ["narration", "dialogue", "sound"],
                                    "description": "장면 요소의 유형 (나레이션, 대사, 효과음)"
                                },
                                "character": {
                                    "type": "string",
                                    "enum": [character_names, "etc", "narration"],
                                    "description": "대사('dialogue') 유형일 때 누가 말하는지 나타내는 필드. character_names 배열에 없는 이름일 경우 'etc'로 표시. 나레이션일 경우 narration으로 표시."
                                },
                                "emotion": {
                                    "type": "string",
                                    "description": "발화자의 감정(예: '기쁨', '슬픔', '후회' 등). 'dialogue'나 'narration'에서 사용 가능"
                                },
                                "emotionParams": {
                                    "type": "object",
                                    "description": "emotion을 참고하여 다양한 감정에 대한 수치 정보를 담는 필드. 0에서 1.0 사이의 값으로 표시.",
                                    "items": {
                                        "happiness": {
                                            "type": "number",
                                            "description": "기쁨 수치"
                                        },
                                        "sadness": {
                                            "type": "number",
                                            "description": "슬픔 수치"
                                        },
                                        "disgust": {
                                            "type": "number",
                                            "description": "역겨움 수치"
                                        },
                                        "fear": {
                                            "type": "number",
                                            "description": "두려움 수치"
                                        },
                                        "surprise": {
                                            "type": "number",
                                            "description": "놀람 수치"
                                        },
                                        "anger": {
                                            "type": "number",
                                            "description": "분노 수치"
                                        },
                                        "neutral": {
                                            "type": "number",
                                            "description": "중립 수치. 기본적으로 1.0으로 표시."
                                        }
                                    }
                                },
                            }
                        }
                    }
                }
            }

            # 캐릭터 정보 변환
            characters = []
            for character in request.characterArr:
                characters.append({
                    "name": character.name,
                    "gender": character.gender,
                    "properties": character.properties
                })

            # 예시 스토리 제공
            example_story = """
            외국 여행 갔을 때 기차역에서 있었던 일이야. 내 앞에 어떤 여자가 역무원이랑 얘기하다가 진짜 멘붕 온 표정으로 서 있는 거야. 듣다 보니까 기차표를 잘못 사서 지금 기차를 못 탄다는 거였는데 문제는 역무원이 영어를 아예 못 한다는 거지. 내가 그 상황 보다가 좀 답답해서 그냥 끼어들었어. 현지 언어로 상황 설명 했더니 역무원이 알았다는 듯이 바로 기차표를 바꿔 주더라.
    여자가 "땡큐 쏘 머치!" 라고 해서 나도 "유어 웰컴" 이라고 대답했고, 이제 가려는데, 그 여자가 혼잣말로 "와... 진짜 다행이다..." 라고 하는거야.
            """
            # 예시 JSON 제공
            example_json = {
                "storyId": 1,
                "storyTitle": "기차역에서 만난 인연",
                "characterArr": [
                    {
                        "name": "나",
                        "gender": "남성",
                        "properties": "한국인, 여행자"
                    },
                    {
                        "name": "여자",
                        "gender": "여성",
                        "properties": "한국인, 여행객"
                    },
                    {
                        "name": "역무원",
                        "gender": "남성",
                        "properties": "현지인, 영어를 못 함"
                    }
                ],
                "sceneArr": [
                    {
                        "audioArr": [{
                            "text": "외국 여행 갔을 때 기차역에서 있었던 일이야.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "neutral",
                            "emotionParams": {"neutral": 1.0}
                        }]
                    },
                    {
                        "audioArr": [{
                            "text": "내 앞에 어떤 여자가 역무원이랑 얘기하다가 진짜 멘붕 온 표정으로 서 있는 거야.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "surprise",
                            "emotionParams": {"surprise": 1.0}
                        }]
                    },
                    {
                        "audioArr": [{
                            "text": "듣다 보니까 기차표를 잘못 사서 지금 기차를 못 탄다는 거였는데 문제는 역무원이 영어를 아예 못 한다는 거지.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "worry",
                            "emotionParams": {"fear": 0.5, "neutral": 0.5}
                        }]
                    },
                    {
                        "audioArr": [{
                            "text": "내가 그 상황 보다가 좀 답답해서 그냥 끼어들었어.",
                            "type": "narration",
                            "character": "나",
                            "emotion": "neutral",
                            "emotionParams": {"neutral": 1.0}
                        }]
                    },
                    {
                        "audioArr": [{
                            "text": "현지 언어로 상황 설명 했더니 역무원이 알았다는 듯이 바로 기차표를 바꿔 주더라.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "relief",
                            "emotionParams": {"happiness": 0.5, "neutral": 0.5}
                        }]
                    },
                    {
                        "audioArr": [{
                            "text": "땡큐 쏘 머치!",
                            "type": "dialogue",
                            "character": "여자",
                            "emotion": "gratitude",
                            "emotionParams": {"happiness": 1.0}
                        },
                        {
                            "text": "유어 웰컴!",
                            "type": "dialogue",
                            "character": "나",
                            "emotion": "neutral",
                            "emotionParams": {"neutral": 1.0}
                        },
                        {
                            "text": "그렇게 대화하고 이제 가려고 했거든? 그런데 그 여자가 혼잣말을 하는거야.",
                            "type": "narration",
                            "character": "narration",
                            "emotion": "relief",
                            "emotionParams": {"neutral": 1.0}
                        }]
                    },
                    {
                        "audioArr": [{
                            "text": "와... 진짜 다행이다...",
                            "type": "dialogue",
                            "character": "여자",
                            "emotion": "relief",
                            "emotionParams": {"happiness": 0.5, "neutral": 0.5}
                        }]
                    },
                ]
            }

            # skeleton_json 작성
            skeleton_json = {
                "storyId": request.storyId,
                "storyTitle": request.storyTitle,
                "characterArr": characters,
                "sceneArr": [{
                    "audioArr": [{
                        "text": "",
                        "type": "",
                        "character": "",
                        "emotion": "",
                        "emotionParams": {"": 0.0}
                    }]
                }]
            }

            # OpenAI API 요청 메시지 구성
            messages = [
                {
                    "role": "system",
                    "content": f"""당신은 영상 제작에 사용될 스토리를 JSON 형식으로 변환하는 전문가입니다. 
                    주어진 스토리 내용을 분석하여 다음 JSON 스키마에서 sceneArr만 알맞게 변환해주세요:
                    
                    {json.dumps(json_schema, indent=2, ensure_ascii=False)}
                    
                    다음은 변환 예시입니다.

                    예시 스토리:
                    {example_story}
                    
                    예시 변환 JSON:
                    {json.dumps(example_json, indent=2, ensure_ascii=False)}
                    
                    응답은 반드시 유효한 JSON 형식이어야 합니다. 
                    대사(dialogue)와 나레이션(narration)을 구분하고, 
                    캐릭터의 감정과 음성 특성을 적절히 추론하여 채워주세요.
                    캐릭터 정보가 없는 경우 모두 나래이션으로 처리해주세요.
                    
                    audioArr 필드의 emotion 값들 각각 0.0 ~ 1.0이 되도록 설정해주세요.
                    audioArr는 한 scene 화면에 적절한 오디오들을 담는 배열입니다.
                    character 필드의 값은 characterArr 배열에 있는 이름이어야 합니다.
                    type이 narration일 경우 character 필드는 narration으로 설정해주세요.

                    중요한 건 대사와 나레이션을 분리할 때
                    **story 내용이 중복거나 생략되지 않도록 해주세요.**
                    """
                },
                {
                    "role": "user",
                    "content": f"""다음 스토리를 주어진 Json 스켈레톤의 sceneArr 필드에 맞게 변환해주세요:
                    
                    Json 스켈레톤:
                    {json.dumps(skeleton_json, indent=2, ensure_ascii=False)}
                    
                    스토리 내용:
                    {request.story}"""
                }
            ]

            # print(messages)

            # OpenAI API 호출
            response = client.chat.completions.create(
                model="gpt-4o-mini",
                messages=messages,
                temperature=0.7,
                max_tokens=4000,
                top_p=1.0,
                frequency_penalty=0.0,
                presence_penalty=0.0
            )
            print("response")
            print(response)
            # API 응답에서 JSON 추출
            response_content = response.choices[0].message.content
            
            # JSON 부분만 추출 (마크다운 코드 블록이 있을 경우 처리)
            if "```json" in response_content:
                json_str = response_content.split("```json")[1].split("```")[0].strip()
            elif "```" in response_content:
                json_str = response_content.split("```")[1].split("```")[0].strip()
            else:
                json_str = response_content.strip()
            
            # JSON 파싱
            script_json = json.loads(json_str)
            
            # ID와 제목은 요청에서 받은 값으로 설정
            script_json["storyId"] = request.storyId
            script_json["storyTitle"] = request.storyTitle
            script_json["characterArr"] = characters
            
            return ScriptResponse(script_json=script_json)
        
        except Exception as e:
            # 오류 발생 시 로깅 및 예외 처리
            print(f"스크립트트 JSON 생성 중 오류 발생: {str(e)}")
            raise
