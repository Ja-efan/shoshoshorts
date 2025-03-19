"""
OpenAI 서비스
"""
from openai import OpenAI
from app.schemas.models import Scene
from app.core.config import settings    

# OpenAI API 키 가져오기
api_key = settings.OPENAI_API_KEY
if not api_key:
    raise ValueError("OPENAI_API_KEY가 .env 파일에 설정되어 있지 않습니다.")

# OpenAI 클라이언트 초기화
client = OpenAI(api_key=api_key)


class OpenAIService:
    """OpenAI API를 활용하여 이미지 프롬프트를 생성하는 서비스"""
    
    @staticmethod
    async def generate_image_prompt(scene: Scene, style: str="cartoon") -> str:
        """
        장면 정보를 바탕으로 DALL·E에 사용할 이미지 프롬프트를 생성합니다.
        
        Args:
            scene: 장면 정보
            style: 이미지 스타일 (예: cartoon, realistic, anime 등)
            
        Returns:
            생성된 이미지 프롬프트
        """
        # 프롬프트 생성을 위한 컨텍스트 구성
        context = {
            "title": scene.script_metadata.title,
            "characters": [],
            "audios": [],
            "style": style
        }
        
        # 등장인물 정보 추가
        for character in scene.script_metadata.characters:
            character_info = {
                "name": character.name,
                "gender": "남자" if character.gender == 1 else "여자",
                "description": character.description
            }
            context["characters"].append(character_info)
        
        # 오디오 정보 추가
        for audio in scene.audios:
            audio_info = {
                "type": audio.type,
                "text": audio.text
            }
            
            if audio.type == "dialogue" and audio.character:
                audio_info["character"] = audio.character
                audio_info["emotion"] = audio.emotion if audio.emotion else "neutral"
                
            context["audios"].append(audio_info)
        
        # 스타일별 지시사항 설정
        style_instructions = {
            "cartoon": "간결하고 명확한 선, 밝은 색상, 단순화된 형태로 표현해주세요. 사실적인 디테일은 필요하지 않습니다.",
            "realistic": "사실적인 조명, 그림자, 질감과 디테일을 살려 실제 사진처럼 묘사해주세요.",
            "anime": "일본 애니메이션 스타일로, 큰 눈, 특징적인 머리스타일, 과장된 표정으로 표현해주세요.",
            "watercolor": "수채화 느낌의 부드러운 색상 전환, 흐릿한 윤곽선, 물감이 번지는 효과를 표현해주세요.",
            "3d": "3D 렌더링된 것처럼 입체감, 질감, 조명 효과를 살려주세요.",
            "pixel": "픽셀 아트 스타일로 낮은 해상도, 제한된 색상 팔레트, 픽셀 단위의 디테일을 표현해주세요."
        }
        
        # cartoon 스타일을 위한 few-shot 예제
        cartoon_examples = [
            "A cheerful cartoon child stands in a vibrant, sunlit playground. The child is dancing and playing. The camera starts with a wide shot and then slowly pans as the child hops and jumps to the left. The scene features playful pastel colors evoking a joyful and energetic mood.",
            "As Pixar 3d cartoon On a sunny morning, Faisal and Farah are playing in the garden. Faisal is trying to ride his small bicycle, but he suffers because he does not learn to ride a bike and Farah stan and watch him Location: Garden with green grass and trees, clear blue sky."
        ]
        
        # 기본 스타일 지시사항
        default_style_instruction = "해당 스타일의 특징을 살려 시각적으로 표현해주세요."
        style_instruction = style_instructions.get(style.lower(), default_style_instruction)
        
        # GPT에 전송할 프롬프트 구성
        prompt = f"""
장면 제목: {context['title']}
장면 ID: {scene.scene_id}

등장인물:
"""
        
        for char in context["characters"]:
            prompt += f"- {char['name']} ({char['gender']}): {char['description']}\n"
            
        prompt += "\n장면 내용:\n"
        
        for audio in context["audios"]:
            if audio["type"] == "narration":
                prompt += f"[내레이션] {audio['text']}\n"
            elif audio["type"] == "dialogue":
                emotion = audio.get("emotion", "")
                emotion_text = f" ({emotion})" if emotion else ""
                prompt += f"[대사] {audio.get('character', '알 수 없음')}{emotion_text}: {audio['text']}\n"
            elif audio["type"] == "sound":
                prompt += f"[효과음] {audio['text']}\n"
        
        prompt += f"\n위 장면을 '{style}' 스타일로 시각화하는 고품질 이미지를 생성하기 위한 디테일한 프롬프트를 작성해주세요. {style_instruction} 장면의 분위기, 등장인물의 특징, 배경 등을 묘사해주세요. 영어로 작성해주세요."
        
        if style.lower() == "cartoon":
            prompt += "\n\n예제:\n"
            for example in cartoon_examples:
                prompt += f"- {example}\n"
        
        try:
            # OpenAI API 호출
            response = client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": f"당신은 이미지 생성 프롬프트를 작성하는 전문가입니다. 주어진 장면을 '{style}' 스타일로 시각적으로 묘사하는 영어 프롬프트를 작성해주세요. {style_instruction}"},
                    {"role": "user", "content": prompt}
                ],
                max_tokens=500,
                temperature=0.7
            )
            
            # 생성된 프롬프트 반환
            return response.choices[0].message.content.strip()
            
        except Exception as e:
            # 오류 발생 시 기본 프롬프트 반환
            print(f"OpenAI API 오류: {str(e)}")
            
            # 기본 프롬프트 생성
            default_prompt = f"A scene from the story '{context['title']}' in {style} style, showing "
            
            # 스타일별 추가 지시사항
            if style.lower() == "cartoon":
                default_prompt += "with simple lines and bright colors, "
            elif style.lower() == "realistic":
                default_prompt += "with photorealistic details, "
            elif style.lower() == "anime":
                default_prompt += "with anime-style character features, "
            
            # 등장인물 정보 추가
            characters_str = ", ".join([f"{c['name']}" for c in context["characters"]])
            default_prompt += f"{characters_str}. "
            
            # 첫 번째 나레이션이나 대화 내용 추가
            for audio in context["audios"]:
                if audio["type"] in ["narration", "dialogue"]:
                    default_prompt += f"{audio['text']}"
                    break
            
            return default_prompt

# 서비스 인스턴스 생성
openai_service = OpenAIService() 