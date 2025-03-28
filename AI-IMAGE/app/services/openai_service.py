"""
OpenAI 서비스
"""
import os
from enum import Enum
from openai import OpenAI
from app.schemas.models import Scene
from app.core.config import settings    
from app.core.logger import app_logger
from app.core.api_config import openai_config, klingai_config
from typing import Union

# OpenAI API 키 가져오기
api_key = settings.OPENAI_API_KEY
if not api_key:
    raise ValueError("OPENAI_API_KEY가 .env 파일에 설정되어 있지 않습니다.")

# OpenAI 클라이언트 초기화
client = OpenAI(api_key=api_key)


class ImageStyle(str, Enum):
    """이미지 스타일 열거형"""
    ANIME = "ANIME"
    REALISM = "REALISM"
    ARTISTIC = "ARTISTIC"
    CONCEPTUAL = "CONCEPTUAL"
    RETRO = "RETRO/LOW-FI"
    GENRE = "GENRE"
    LIGHTING = "LIGHTING"
    MOOD = "MOOD"
    
    @classmethod
    def _missing_(cls, value):
        """대소문자 구분 없이 Enum 값 찾기"""
        if isinstance(value, str):
            # 문자열을 대문자로 변환하여 비교
            for member in cls:
                if member.value.upper() == value.upper():
                    return member
        return None


class OpenAIService:
    """OpenAI API를 활용하여 이미지 프롬프트를 생성하는 서비스"""
    
    @staticmethod
    async def generate_image_prompt(scene: Scene, style: Union[str, ImageStyle]=ImageStyle.ANIME) -> str:
        """
        장면 정보를 바탕으로 이미지 생성(KLING AI)에 사용할 이미지 프롬프트를 생성합니다.
        
        Args:
            scene(Scene): 장면 정보
            style(Union[str, ImageStyle]): 이미지 스타일 (대소문자 구분 없이 입력 가능)
                - ANIME: anime-style, cartoon, cel-shaded, Studio Ghibli-style
                - REALISM: photorealistic, cinematic realism, ultra-realistic
                - ARTISTIC: watercolor, oil painting, pastel sketch, digital painting, hand-drawn
                - CONCEPTUAL: fantasy concept art, sci-fi illustration, character sheet
                - RETRO/LOW-FI: pixel art, 8-bit, VHS aesthetic, low-poly
                - GENRE: cyberpunk, steampunk, gothic fantasy, dark academia
                - LIGHTING: cinematic lighting, golden hour glow, soft ambient light, dramatic shadows
                - MOOD: minimalist, surreal, dreamy, vibrant, moody, textured
            
        Returns(str):
            생성된 이미지 프롬프트
        """
        # 문자열로 입력된 경우 ImageStyle Enum으로 변환
        if isinstance(style, str) and not isinstance(style, ImageStyle):
            try:
                style = ImageStyle(style)
            except ValueError:
                app_logger.warning(f"유효하지 않은 이미지 스타일 '{style}'. 기본값 'ANIME'를 사용합니다.")
                style = ImageStyle.ANIME
        
        # 프롬프트 생성을 위한 컨텍스트 구성
        context = {
            "title": scene.story_metadata.title,
            "characters": [],
            "audios": [],
            "style": style
        }
        
        # 등장인물 정보 추가
        for character in scene.story_metadata.characters:
            character_info = {
                "name": character.name,
                "gender": "남자" if character.gender == 0 else "여자",
                "description": character.description
            }
            context["characters"].append(character_info)
        
        # 오디오(대사 및 나레이션) 정보 추가
        for audio in scene.audios:
            audio_info = {
                "type": audio.type,
                "text": audio.text
            }
            
            if audio.type == "dialogue" and audio.character:
                audio_info["character"] = audio.character
                audio_info["emotion"] = audio.emotion if audio.emotion else "neutral"
                
            context["audios"].append(audio_info)
        
        # GPT에 전송할 프롬프트 구성
        scene_info = f"""
장면 제목: {context['title']}
장면 ID: {scene.scene_id}
장면 스타일: {style.value if isinstance(style, ImageStyle) else style}

등장인물:
"""
        
        for char in context["characters"]:
            scene_info += f"- {char['name']} ({char['gender']}): {char['description']}\n"
            
        scene_info += "\n장면 내용:\n"
        
        for audio in context["audios"]:
            if audio["type"] == "narration":
                scene_info += f"[내레이션] {audio['text']}\n"
            elif audio["type"] == "dialogue":
                emotion = audio.get("emotion", "")
                emotion_text = f" ({emotion})" if emotion else ""
                scene_info += f"[대사] {audio.get('character', '알 수 없음')}{emotion_text}: {audio['text']}\n"
            elif audio["type"] == "sound":
                scene_info += f"[효과음] {audio['text']}\n"
                
        app_logger.debug(f"이미지 프롬프트 생성을 위해 생성된 장면 정보: \n{scene_info}")
        
        system_prompt = open(os.path.join(settings.SYSTEM_PROMPT_DIR, openai_config.KLINGAI_SYSTEM_PROMPT), "r").read()
        
        try:
            # OpenAI API 호출
            response = client.chat.completions.create(
                model=openai_config.MODEL,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": scene_info}
                ],
                max_tokens=openai_config.MAX_TOKENS,
                temperature=openai_config.TEMPERATURE
            )
            
            # TODO 시스템 프롬프트 수정해서 이미지 프롬프트 길지 않게 생성 (2250자 임시 조치)
            image_prompt = response.choices[0].message.content.strip()[:2250]
            app_logger.debug(f"생성된 이미지 프롬프트: \n{image_prompt}")
            app_logger.debug(f"생성된 이미지 프롬프트 길이: {len(image_prompt)}")

            # 생성된 프롬프트 반환
            return image_prompt
            
        except Exception as e:
            # 오류 발생 시 기본 프롬프트 반환
            app_logger.error(f"OpenAI API 오류: {str(e)}")
            
            # 기본 프롬프트 생성
            default_prompt = f"A scene from the story '{context['title']}' in {style} style, showing "
            
            # 스타일별 추가 지시사항
            style_str = style.value if isinstance(style, ImageStyle) else str(style)
            if style_str.upper() == "ANIME":
                default_prompt += "with anime-style character features, cel-shaded, Studio Ghibli-style, "
            elif style_str.upper() == "REALISM":
                default_prompt += "with photorealistic details, cinematic realism, ultra-realistic, "
            elif style_str.upper() == "ARTISTIC":
                default_prompt += "with watercolor, digital painting, hand-drawn style, "
            elif style_str.upper() == "CONCEPTUAL":
                default_prompt += "with fantasy concept art, character sheet style, "
            elif style_str.upper() == "RETRO":
                default_prompt += "with pixel art, 8-bit style, low-poly, "
            elif style_str.upper() == "GENRE":
                default_prompt += "with cyberpunk, gothic fantasy elements, "
            elif style_str.upper() == "LIGHTING":
                default_prompt += "with cinematic lighting, golden hour glow, dramatic shadows, "
            elif style_str.upper() == "MOOD":
                default_prompt += "with dreamy, vibrant, textured style, "
            
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