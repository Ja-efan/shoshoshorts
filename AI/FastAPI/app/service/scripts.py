import os
import json
from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field
from openai import OpenAI
from dotenv import load_dotenv

# .env 파일에서 환경 변수 로드
load_dotenv()

# OpenAI API 키 가져오기
api_key = os.getenv("OPENAI_API_KEY")
if not api_key:
    raise ValueError("OPENAI_API_KEY가 .env 파일에 설정되어 있지 않습니다.")

# OpenAI 클라이언트 초기화
client = OpenAI(api_key=api_key)

# 요청 모델 정의
class Character(BaseModel):
    name: str = Field(description="캐릭터 이름")
    gender: str = Field(description="캐릭터 성별")
    properties: str = Field(description="캐릭터 특성 정보")

class ScriptRequest(BaseModel):
    scriptId: str = Field(description="스크립트 고유 식별자")
    scriptTitle: str = Field(description="스크립트 제목")
    character_arr: List[Character] = Field(description="스크립트에 등장하는 캐릭터 목록")
    contents: str = Field(description="스크립트 내용 (텍스트)")

# 응답 모델 정의
class ScriptResponse(BaseModel):
    script_json: Dict[str, Any] = Field(description="변환된 스크립트 JSON")

async def generate_script_json(request: ScriptRequest) -> ScriptResponse:
    """
    스크립트 내용을 JSON 형식으로 변환하는 함수
    
    Args:
        request: 스크립트 요청 객체
        
    Returns:
        변환된 스크립트 JSON
    """
    try:
        # JSON 스키마 정의
        json_schema = {
            "id": {
                "type": "string",
                "description": "스크립트 고유 식별자 (AutoSet)"
            },
            "title": {
                "type": "string",
                "description": "스크립트 제목"
            },
            "characters": {
                "type": "array",
                "description": "스크립트에 등장하는 캐릭터 목록",
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
            "contents": {
                "type": "array",
                "description": "스크립트 블록 목록",
                "items": {
                    "type": {
                        "type": "string",
                        "enum": ["narration", "dialogue", "sound", "effect"],
                        "description": "장면 요소의 유형 (나레이션, 대사, 사운드, 기타 효과 등)"
                    },
                    "text": {
                        "type": ["string", "null"],
                        "description": "해당 장면의 텍스트 내용. 대사/나레이션일 때 읽어줄 텍스트, 사운드일 경우 소리를 설명하는 텍스트 등"
                    },
                    "character": {
                        "type": ["string", "null"],
                        "description": "대사('dialogue') 유형일 때 누가 말하는지 나타내는 필드"
                    },
                    "emotion": {
                        "type": ["string", "null"],
                        "description": "발화자의 감정(예: '기쁨', '슬픔', '후회' 등). 'dialogue'나 'narration'에서 사용 가능"
                    },
                    "audio": {
                        "type": ["object", "null"],
                        "description": "TTS나 사운드 재생 등에 필요한 정보를 담는 필드",
                        "properties": {
                            "voiceType": {
                                "type": "string",
                                "description": "음성 유형(예: '남성 목소리', '여성 목소리', '로봇 음성' 등)"
                            },
                            "emotion": {
                                "type": "object",
                                "description": "다양한 감정에 대한 수치 정보를 담는 필드",
                                "properties": {
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
                                        "description": "중립 수치"
                                    }
                                }
                            },
                            "speakingRate": {
                                "type": "number",
                                "description": "발화 속도 (1.0은 기본 속도)"
                            },
                            "seed": {
                                "type": "number",
                                "description": "발화 시드 값"
                            },
                            "languageCode": {
                                "type": "string",
                                "description": "발화 언어"
                            }
                        }
                    }
                }
            }
        }

        # 캐릭터 정보 변환
        characters = []
        for character in request.character_arr:
            characters.append({
                "name": character.name,
                "gender": character.gender,
                "properties": character.properties
            })

        # 예시 JSON 제공
        example_json = {
            "id": "script-001",
            "title": "아반떼 N 전손 썰",
            "characters": [
                {
                    "name": "종훈",
                    "gender": "male",
                    "properties": "무언가에 쉽게 열광하고 즉흥적인 성격"
                },
                {
                    "name": "보험사 직원",
                    "gender": "male",
                    "properties": "차분하고 책임감 있는 성격"
                }
            ],
            "contents": [
                {
                    "type": "narration",
                    "text": "N 모드를 켠 뒤, 도산대로를 달리기 시작했다.",
                    "character": None,
                    "emotion": None,
                    "audio": None
                },
                {
                    "type": "dialogue",
                    "text": "N은 렌트해서 타는 게 아니었다...",
                    "character": "종훈",
                    "emotion": "후회",
                    "audio": {
                        "voiceType": "male_youth",
                        "emotion": {
                            "happiness": 0.1,
                            "sadness": 0.6,
                            "disgust": 0,
                            "fear": 0,
                            "surprise": 0,
                            "anger": 0,
                            "neutral": 0.3
                        },
                        "speakingRate": 1.0,
                        "seed": 12345,
                        "languageCode": "ko"
                    }
                }
            ]
        }

        # OpenAI API 요청 메시지 구성
        messages = [
            {
                "role": "system",
                "content": f"""당신은 스크립트를 JSON 형식으로 변환하는 전문가입니다. 
                주어진 스크립트 내용을 분석하여 다음 JSON 스키마에 맞게 변환해주세요:
                
                {json.dumps(json_schema, indent=2, ensure_ascii=False)}
                
                다음은 변환 예시입니다:
                
                {json.dumps(example_json, indent=2, ensure_ascii=False)}
                
                응답은 반드시 유효한 JSON 형식이어야 합니다. 
                대사(dialogue)와 나레이션(narration)을 구분하고, 
                캐릭터의 감정과 음성 특성을 적절히 추론하여 채워주세요.
                
                audio 필드의 emotion 값들 각각 0.0 ~ 1.0이 되도록 설정해주세요.
                speakingRate는 기본값으로 1.0을 사용하고, 
                seed는 임의의 5자리 숫자를 사용하세요.
                languageCode는 기본적으로 'ko'를 사용하세요."""
            },
            {
                "role": "user",
                "content": f"""다음 스크립트를 JSON으로 변환해주세요:
                
                스크립트 ID: {request.scriptId}
                스크립트 제목: {request.scriptTitle}
                
                등장인물:
                {json.dumps([character.dict() for character in request.character_arr], indent=2, ensure_ascii=False)}
                
                스크립트 내용:
                {request.contents}"""
            }
        ]

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
        script_json["id"] = request.scriptId
        script_json["title"] = request.scriptTitle
        
        return ScriptResponse(script_json=script_json)
    
    except Exception as e:
        # 오류 발생 시 로깅 및 예외 처리
        print(f"스크립트 JSON 생성 중 오류 발생: {str(e)}")
        raise
