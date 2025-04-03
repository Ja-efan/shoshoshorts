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
        
        # 먼저 스크립트 정리하는 함수 수행
        try:
            original_story = request.story
            preprocessed_story = ""
            original_story_ex1 = """윗집에 애가 셋인데 시간을 안가리고 층간소음 장난 아니었음.
몇번 가서 말해도 예예. 알겠습니다 하고 바뀌는 건 없었음.
한참 참다가 누나가 빡친 얼굴로 해결한다고 올라감.
그러고 평온한 얼굴로 내려와서는 해결했다고 말했는데
그 이후 며칠 지났는데도 없지는 않지만 엄청 조용해짐.
야식 먹으면서 누나한테 뭐라고 했길래 조용해진건지 물어봄.
근데 이 도라이가 내 동생 조현병 환자인데 소음 듣고 자꾸 오락가락해요. 제발 부탁드린다고, 자기도 동생 무섭다고 했다함.
졸지에 미친놈 되버림...
엘베 타는데 어째 사람들이 같이 안타려고하더라."""
            preprocessed_story_ex1 = """윗집에 애가 셋인데 시간을 안가리고 층간소음 장난 아니었음.
몇번 가서 말해도 성의 없이 대답하더라.
"예예. 알겠습니다."
바뀌는 건 없었음.
한참 참다가 누나가 빡친 얼굴로 해결한다고 올라감.
그러고 평온한 얼굴로 내려와서는
해결했다고 말했는데
그 이후 며칠 지났는데도 없지는 않지만 엄청 조용해짐
야식 먹으면서 누나한테 물어봤음.
"뭐라고 했길래 윗집 조용해졌음?"
근데 이 도라이가 이랬다는 거임.
"내 동생 조현병 환자인데 소음 듣고 자꾸 오락가락해요. 저도 제 동생 무서워요. 제발 부탁드립니다."
졸지에 미친놈 되버림...
엘베 타는데 어째 사람들이 같이 안타려고하더라.
"""
            original_story_ex2 = """외국 여행 갔을 때 기차역에서 있었던 일이야. 내 앞에 어떤 여자가 역무원이랑 얘기하다가 진짜 멘붕 온 표정으로 서 있는 거야. 듣다 보니까 기차표를 잘못 사서 지금 기차를 못 탄다는 거였는데 문제는 역무원이 영어를 아예 못 한다는 거지. 내가 그 상황 보다가 좀 답답해서 그냥 끼어들었어. 현지 언어로 상황 설명 했더니 역무원이 알았다는 듯이 바로 기차표를 바꿔 주더라.\n여자가 \"땡큐 쏘 머치!\" 라고 해서 나도 \"유어 웰컴\" 이라고 대답했고, 이제 가려는데, 그 여자가 혼잣말로 \"와... 진짜 다행이다...\" 라고 하는거야.\n나 순간 너무 놀라서 \"어? 한국분이세요?\" 했더니, 그 여자도 놀라서 \"헐! 한국 분이셨구나! 진짜 감사해요!\" 이러는데 뭔가 급 친해진 느낌? 그러다 어쩌다 보니 같이 기차 타고 얘기하면서 진짜 재밌는 시간을 보냈어.\n내릴 때 여자가 \"한국 돌아가면 꼭 한번 만나요\" 하면서 연락처를 주더라고. 솔직히 그냥 별 생각 없었는데, 한국 들어오는 날 카톡이 딱 온 거야. \"혹시 오늘 한국 들어오시는 날 맞죠? 그때 진짜 감사했어요! 시간 괜찮으시면 밥 같이 먹을래요?\" 이러는데, 뭔가 싶어서 일단 나갔지. 근데 그게 밥 한끼가 두끼 되고, 영화 한 편이 두 편되고 결론은???\n지금 같이 살고 있고, 애도 있어. 아내가 가끔 그때 얘기하면 \"그때 당신, 나한테 백마탄 왕자님이였어!\" 이러는데, 내가 먼저 꼬셨다고 주장하더라. 여행에서 이렇게 인생 파트너 만날 줄 누가 알았겠냐?\n인생 모르니까 누구에게나 잘해줘!"""
            preprocessed_story_ex2 = """외국 여행 갔을 때 기차역에서 있었던 일이야. 내 앞에 어떤 여자가 역무원이랑 얘기하다가 진짜 멘붕 온 표정으로 서 있는 거야. 듣다 보니까 기차표를 잘못 사서 지금 기차를 못 탄다는 거였는데 문제는 역무원이 영어를 아예 못 한다는 거지. 내가 그 상황 보다가 좀 답답해서 그냥 끼어들었어. 현지 언어로 상황 설명 했더니 역무원이 알았다는 듯이 바로 기차표를 바꿔 주더라. 여자가 고맙다고. "땡큐 쏘 머치!". 그래서 나도 짧게 대답했어. "유어 웰컴!". 그렇게 대화하고 이제 가려고 했거든? 그런데 그 여자가 혼잣말을 하는거야. "와... 진짜 다행이다..." 나 순간 너무 놀라서. "어? 한국분이세요?". 했더니, 그 여자도 놀라더라. "헐! 한국 분이셨구나! 진짜 감사해요!" 이러는데 뭔가 급 친해진 느낌? 그러다 어쩌다 보니 같이 기차 타고 얘기하면서 진짜 재밌는 시간을 보냈어.\n내릴 때 여자가 연락처를 주면서 그러더라고. "한국 돌아가면 꼭 한번 만나요". 솔직히 그냥 별 생각 없었는데, 한국 들어오는 날 카톡이 딱 온 거야. "혹시 오늘 한국 들어오시는 날 맞죠? 그때 진짜 감사했어요! 시간 괜찮으시면 밥 같이 먹을래요?". 이러는데, 뭔가 싶어서 일단 나갔지. 근데 그게 밥 한끼가 두끼 되고, 영화 한 편이 두 편되고 결론은???\n지금 같이 살고 있고, 애도 있어. 아내가 가끔 그때 얘기하면 \"그때 당신, 나한테 백마탄 왕자님이였어!\" 이러는데, 내가 먼저 꼬셨다고 주장하더라. 여행에서 이렇게 인생 파트너 만날 줄 누가 알았겠냐?\n인생 모르니까 누구에게나 잘해줘!"""
            
            # OpenAI API 요청 메시지 구성
            sys_messages = [
                {
                    "role": "system",
                    "content": f"""당신은 텍스트를 TTS(Text-to-Speech)에 적합한 대본으로 변환하는 전문가입니다.
                    다음 변환 조건에 따라 입력된 스토리를 전처리해주세요:
                    
                    1. 숫자나 영어는 한국어 발음에 따른 기호로 바꿉니다.
                    - 예시: 1234 → 천이백삼십사
                    - 예시: Thank you → 땡큐
                    - 예시: 1. → 첫째

                    2. 서술 중간에 대사가 들어가 있는 경우, 서술과 대사를 자연스럽게 분리합니다. 문장을 자주 쪼개서 대사를 분리합니다.
                    - '~라고 말했다.', '~이라고 대답했다.' 등의 구조를 '대답했다. "~".' 형식으로 분리합니다.
                    - 서술 사이에 "", '' 로 감싸여진 대화가 나오면 분리합니다.

                    3. 데이터를 json 양식에서 불러오기 때문에 \' 또는 \, 와 같은 escape sequence를 ' 또는 , 의 문자로 다시 변환해줍니다.
                    
                    예시:
                    원문: '윗집에 몇번 가서 말해도 "예예. 조용히 할게요."하고 바뀌는 건 없었음.'
                    변환: '윗집에 몇번 가서 말했거든? "예예. 조용하 할게요.". 그렇게 말하고 바뀌는 건 없었어.'
                    
                    자연스러운 음성 읽기를 위해 위 조건을 적용하여 텍스트를 변환해주세요.

                    다음은 변환 예시입니다.

                    예시 스토리1:
                    {original_story_ex1}

                    예시 변환1:
                    {preprocessed_story_ex1}

                    예시 스토리2:
                    {original_story_ex2}

                    예시 변환2:
                    {preprocessed_story_ex2}
                    """
                },
                {
                    "role": "user",
                    "content": f"다음 스토리를 TTS에 적합한 형태로 전처리해주세요:\n\n{original_story}"
                }
            ]

            print("시스템 프롬프트1 확인:")
            print(sys_messages)

            # OpenAI API 호출
            response = client.chat.completions.create(
                model="gpt-4o",
                messages=sys_messages,
                temperature=0.5,  # 창의성보다 일관성 있는 변환을 위해 낮은 temperature 설정
                max_tokens=4000,
                top_p=1.0,
                frequency_penalty=0.0,
                presence_penalty=0.0
            )

            # API 응답에서 변환된 텍스트 추출
            preprocessed_story = response.choices[0].message.content.strip()
            print("preprocessed_story")
            print(preprocessed_story)

        except Exception as e:
            # 오류 발생 시 로깅 및 예외 처리
            print(f"스크립트 정제본 생성 시 오류 발생: {str(e)}")
            raise


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
                "narVoiceCode": {
                    "type": "string",
                    "description": "나레이션 보이스 코드"
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
                            "description": "캐릭터 성별. 남자 또는 여자."
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
                                    "enum": ["narration", "dialogue"],
                                    "description": "장면 요소의 유형 (나레이션, 대사)"
                                },
                                "character": {
                                    "type": "string",
                                    "enum": [character_names, "etc", "narration"],
                                    "description": "대사('dialogue') 유형일 때 누가 말하는지 나타내는 필드. character_names 배열에 없는 이름일 경우 'etc'로 표시. type이 나레이션일 경우 narration으로 표시."
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
            additional_prompt=":"

            characters = []
            temp_characters = []
            for character in request.characterArr:
                    characters.append({
                        "name": character.name,
                        "gender": character.gender,
                        "properties": character.properties,
                    })
                    temp_characters.append({
                        "name": character.name,
                        "gender": character.gender,
                        "properties": character.properties,
                        "voiceCode": character.voiceCode
                    })

            if request.characterArr == []:
                additional_prompt = ". 현재 json에 캐릭터 정보가 주어지지 않았기 때문에 스토리에서 인물 정보를 추출하여 characterArr를 완성해주세요. 머리 색, 머리 모양, 눈동자 색, 복장을 상세하게 입력하세요. 스토리에서 알 수 없다면 상상해서 쓰세요."
                

            # 예시 스토리 제공
            example_story = """
            외국 여행 갔을 때 기차역에서 있었던 일이야. 내 앞에 어떤 여자가 역무원이랑 얘기하다가 진짜 멘붕 온 표정으로 서 있는 거야. 듣다 보니까 기차표를 잘못 사서 지금 기차를 못 탄다는 거였는데, 문제는 역무원이 영어를 아예 못 한다는 거지. 내가 그 상황 보다가 좀 답답해서 그냥 끼어들었어. 현지 언어로 상황 설명 했더니, 역무원이 알았다는 듯이 바로 기차표를  바꿔 주더라. 여자가 고맙다고 했어. "땡큐 쏘 머치!". 그래서 나도 대답했지. "유어 웰컴!". 이제 가려는데, 그 여자가 혼잣말로 그러는 거야. "와... 진짜 다행이다...". 나 순간 너무 놀라서 물어봤어. "어? 한국분이세요?". 했더니, 그 여자도 놀라서 대답했어. "헐! 한국 분이셨구나! 진짜 감사해요!". 이러는데, 뭔가 급 친해진 느낌? 그러다 어쩌다 보니 같이 기차 타고 얘기하면서 진짜 재밌는 시간을 보냈어. 내릴 때 여자가 연락처를 주면서 그러더라고. "한국 돌아가면 꼭 한번 만나요". 솔직히 그냥 별 생각 없었는데, 한국 들어오는 날 카톡이 딱 온 거야. "혹시 오늘 한국 들어오시는 날 맞죠? 그때 진짜 감사했어요! 시간 괜찮으시면 밥 같이 먹을래요?". 이러는데, 뭔가 싶어서 일단 나갔지. 근데 그게 밥 한끼가 두끼 되고, 영화 한 편이 두 편 되고, 결론은??? 지금 같이 살고 있고, 애도 있어. 아내가 가끔 그때 얘기하면 이렇게 말해. "그때 당신, 나한테 백마탄 왕자님이였어!". 이러는데, 내가 먼저 꼬셨다고 주장하더라. 여행에서 이렇게 인생 파트 너 만날 줄 누가 알았겠냐? 인생 모르니까 누구에게나 잘해줘!
            """
            # 예시 JSON 제공
            example_json = {
        "storyId": 1,
        "storyTitle": "운명을 믿으시나요?",
        "characterArr": [
            {
                "name": "나",
                "gender": "남자",
                "properties": "흑발에 검은 눈. 한국인. 여자를 도와주고 결혼까지 한다.",
            },
            {
                "name": "아내",
                "gender": "여자",
                "properties": "갈색 머리에 긴 장발. 한국인. 외국에서 기차표를 잘못 샀다가 내가 도와주었다.",
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
                            "neutral": 1.0
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
                            "surprise": 1.0
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "듣다 보니까 기차표를 잘못 사서 지금 기차를 못 탄다는 거였는데, 문제는 역무원이 영어를 아예 못 한다는 거지.",
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
                        "character": "나",
                        "emotion": "neutral",
                        "emotionParams": {
                            "neutral": 1.0
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "현지 언어로 상황 설명 했더니, 역무원이 알았다는 듯이 바로 기차표를 바꿔 주더라.",
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
                        "text": "여자가 고맙다고 했어.",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "gratitude",
                        "emotionParams": {
                            "happiness": 0.5
                        }
                    },
                    {
                        "text": "땡큐 쏘 머치!",
                        "type": "dialogue",
                        "character": "아내",
                        "emotion": "gratitude",
                        "emotionParams": {
                            "happiness": 1.0
                        }
                    },
                    {
                        "text": "그래서 나도 대답했지.",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "neutral",
                        "emotionParams": {
                            "neutral": 1.0
                        }
                    },
                    {
                        "text": "유어 웰컴!",
                        "type": "dialogue",
                        "character": "나",
                        "emotion": "neutral",
                        "emotionParams": {
                            "neutral": 1.0
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "이제 가려는데, 그 여자가 혼잣말로 그러는 거야.",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "neutral",
                        "emotionParams": {
                            "neutral": 1.0
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "와... 진짜 다행이다...",
                        "type": "dialogue",
                        "character": "아내",
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
                        "text": "나 순간 너무 놀라서 물어봤어.",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "surprise",
                        "emotionParams": {
                            "surprise": 1.0
                        }
                    },
                    {
                        "text": "어? 한국분이세요?",
                        "type": "dialogue",
                        "character": "나",
                        "emotion": "surprise",
                        "emotionParams": {
                            "surprise": 1.0
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "했더니, 그 여자도 놀라서 대답했어.",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "surprise",
                        "emotionParams": {
                            "surprise": 1.0
                        }
                    },
                    {
                        "text": "헐! 한국 분이셨구나! 진짜 감사해요!",
                        "type": "dialogue",
                        "character": "아내",
                        "emotion": "gratitude",
                        "emotionParams": {
                            "happiness": 1.0
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "이러는데, 뭔가 급 친해진 느낌?",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "neutral",
                        "emotionParams": {
                            "neutral": 1.0
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
                            "happiness": 1.0
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "내릴 때 여자가 연락처를 주면서 그러더라고.",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "neutral",
                        "emotionParams": {
                            "neutral": 1.0
                        }
                    },
                    {
                        "text": "한국 돌아가면 꼭 한번 만나요.",
                        "type": "dialogue",
                        "character": "아내",
                        "emotion": "hope",
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
                        "text": "솔직히 그냥 별 생각 없었는데, 한국 들어오는 날 카톡이 딱 온 거야.",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "surprise",
                        "emotionParams": {
                            "surprise": 1.0
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "혹시 오늘 한국 들어오시는 날 맞죠? 그때 진짜 감사했어요! 시간 괜찮으시면 밥 같이 먹을래요?",
                        "type": "dialogue",
                        "character": "아내",
                        "emotion": "hope",
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
                        "text": "이러는데, 뭔가 싶어서 일단 나갔지.",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "curiosity",
                        "emotionParams": {
                            "surprise": 0.5,
                            "neutral": 0.5
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "근데 그게 밥 한끼가 두끼 되고, 영화 한 편이 두 편 되고, 결론은??? 지금 같이 살고 있고, 애도 있어.",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "happiness",
                        "emotionParams": {
                            "happiness": 1.0
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "아내가 가끔 그때 얘기하면 이렇게 말해.",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "neutral",
                        "emotionParams": {
                            "neutral": 1.0
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "그때 당신, 나한테 백마탄 왕자님이였어!",
                        "type": "dialogue",
                        "character": "아내",
                        "emotion": "happiness",
                        "emotionParams": {
                            "happiness": 1.0
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "이러는데, 내가 먼저 꼬셨다고 주장하더라.",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "neutral",
                        "emotionParams": {
                            "neutral": 1.0
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
                        "emotion": "reflection",
                        "emotionParams": {
                            "neutral": 0.5,
                            "surprise": 0.5
                        }
                    }
                ]
            },
            {
                "audioArr": [
                    {
                        "text": "인생 모르니까 누구에게나 잘해줘!",
                        "type": "narration",
                        "character": "narration",
                        "emotion": "advice",
                        "emotionParams": {
                            "neutral": 1.0
                        }
                    }
                ]
            }
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
                    나래이션으로 시작했다면, '.' 로 문장을 마무리하고 다음 문장에서 대사를 출력하세요.
                    """
                },
                {
                    "role": "user",
                    "content": f"""다음 스토리를 주어진 Json 스켈레톤의 sceneArr 필드에 맞게 변환해주세요{additional_prompt}
                    
                    Json 스켈레톤:
                    {json.dumps(skeleton_json, indent=2, ensure_ascii=False)}
                    
                    스토리 내용:
                    {preprocessed_story}"""
                }
            ]

            print("시스템 프롬프트2 확인:")
            print(messages)

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
            script_json["narVoiceCode"] = request.narVoiceCode

            if request.characterArr == []:#기존에 받은 정보가 없어서 임의로 생성한 경우. voice code만 추가하면 됨.
                add_voicecode_arr = []
                for character in script_json["characterArr"]:
                        new_voice_code = ""
                        if character["gender"] == "남자":
                            new_voice_code = "4JJwo477JUAx3HV0T7n7"
                        elif character["gender"] == "여자":
                            new_voice_code = "uyVNoMrnUku1dZyVEXwD"
                            add_voicecode_arr.append({
                                "name": character["name"],
                                "gender": character["gender"],
                                "properties": character["properties"],
                                "voiceCode": new_voice_code
                            })
                        else:
                            add_voicecode_arr.append({
                                "name": character["name"],
                                "gender": character["gender"],
                                "properties": character["properties"],
                                "voiceCode": request.narVoiceCode
                            })
                script_json["characterArr"] = add_voicecode_arr
                
            else: 
                script_json["characterArr"] = temp_characters
            
            
            return ScriptResponse(script_json=script_json)
        
        except Exception as e:
            # 오류 발생 시 로깅 및 예외 처리
            print(f"스크립트트 JSON 생성 중 오류 발생: {str(e)}")
            raise
