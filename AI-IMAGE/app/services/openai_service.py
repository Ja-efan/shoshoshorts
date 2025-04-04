"""
OpenAI 서비스
"""

import os
import json
from enum import Enum
from typing import Union, Tuple

from openai import OpenAI

from app.schemas.models import Scene, SceneInfo, SceneSummary
from app.core.config import settings
from app.core.logger import app_logger
from app.core.api_config import openai_config

# OpenAI API 키 가져오기
api_key = openai_config.API_KEY
if not api_key:
    raise ValueError("OPENAI_API_KEY가 .env 파일에 설정되어 있지 않습니다.")

# OpenAI 클라이언트 초기화
client = OpenAI(api_key=api_key)


class ImageStyle(str, Enum):
    """
    - ANIME: anime-style, cartoon, cel-shaded, Studio Ghibli-style
    - REALISM: photorealistic, cinematic realism, ultra-realistic
    - ARTISTIC: watercolor, oil painting, pastel sketch, digital painting, hand-drawn
    - CONCEPTUAL: fantasy concept art, sci-fi illustration, character sheet
    - RETRO/LOW-FI: pixel art, 8-bit, VHS aesthetic, low-poly
    - GENRE: cyberpunk, steampunk, gothic fantasy, dark academia
    - LIGHTING: cinematic lighting, golden hour glow, soft ambient light, dramatic shadows
    - MOOD: minimalist, surreal, dreamy, vibrant, moody, textured
    - GHIBLI: Studio Ghibli-style, anime-style, cartoon, cel-shaded
    - DISNEY: Disney-style, cartoon, vibrant colors, expressive characters
    """

    ANIME = "ANIME"
    REALISM = "REALISM"
    ARTISTIC = "ARTISTIC"
    CONCEPTUAL = "CONCEPTUAL"
    RETRO = "RETRO/LOW-FI"
    GENRE = "GENRE"
    LIGHTING = "LIGHTING"
    MOOD = "MOOD"
    GHIBLI = "GHIBLI"
    DISNEY_PIXAR = "DISNEY-PIXAR"

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
    def get_system_prompt(which: str):
        """
        시스템 프롬프트를 반환합니다.
        """
        if which == "scene_info":
            return open(openai_config.SYSTEM_PROMPT["scene_info"], "r").read()
        elif which == "image_prompt":
            return open(openai_config.SYSTEM_PROMPT["image_prompt"], "r").read()

    @staticmethod
    def get_negative_prompt(style: ImageStyle) -> str:
        """
        이미지 스타일에 따른 부정 프롬프트를 반환합니다.
        """
        base_negative_prompt = "low quality, bad anatomy, blurry, pixelated, disfigured"
        if style == ImageStyle.GHIBLI:
            return f"{base_negative_prompt}, Disney Style"
        elif style == ImageStyle.DISNEY_PIXAR:
            return f"{base_negative_prompt}, Studio Ghibli Style"
        return base_negative_prompt

    @staticmethod
    async def generate_scene_info(scene: Scene, style: Union[str, ImageStyle]) -> str:
        """
        장면 정보를 바탕으로 이미지 프롬프트 생성에 필요한 장면 정보(scene_info)를 생성합니다.
        """

        # 시스템 프롬프트 (scene_info)
        system_prompt = OpenAIService.get_system_prompt("scene_info")

        # 장면 요약 생성 (gpt-4o-mini)
        response = client.beta.chat.completions.parse(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": system_prompt},
                {
                    "role": "user",
                    "content": json.dumps(scene.model_dump(), ensure_ascii=False),
                },
            ],
            response_format=SceneSummary,
        )

        # 장면 정보 생성
        scene_info = f"""
            장면 제목: {scene.story_metadata.title}
            장면 ID: {scene.scene_id}
            장면 스타일: {style}

            등장인물:
        """

        for char in scene.story_metadata.characters:
            gender = "남자" if char.gender == 0 else "여자"
            scene_info += f"- {char.name} ({gender}): {char.description}\n"

        scene_info += "\n장면 내용:\n"

        for audio in scene.audios:
            if audio.type == "narration":
                scene_info += f"[내레이션] {audio.text}\n"
            elif audio.type == "dialogue":
                emotion_text = f" ({audio.emotion})" if audio.emotion else ""
                scene_info += f"[대사] {audio.character}{emotion_text}: {audio.text}\n"
            elif audio.type == "sound":
                scene_info += f"[효과음] {audio.text}\n"

        scene_info += f"장면 요약: {response.choices[0].message.parsed.summary}"

        app_logger.debug(f"Scene Info: \n{scene_info}")
        return scene_info

    @staticmethod
    async def generate_image_prompt(
        scene: Scene, style: Union[str, ImageStyle] = ImageStyle.DISNEY_PIXAR
    ) -> Tuple[str, str, SceneInfo]:
        """
        장면 정보를 바탕으로 이미지 생성(KLING AI)에 사용할 이미지 프롬프트를 생성합니다.

        Args:
            scene(Scene): 장면 정보
            style(Union[str, ImageStyle]): 이미지 스타일 (대소문자 구분 없이 입력 가능)

        Returns(str):
            생성된 이미지 프롬프트
        """
        # 문자열로 입력된 경우 ImageStyle Enum으로 변환
        if isinstance(style, str) and not isinstance(style, ImageStyle):
            try:
                style = ImageStyle(style)
            except ValueError:
                app_logger.warning(
                    f"Invalid image style '{style}'. Using default value '{settings.DEFAULT_IMAGE_STYLE}'."
                )
                style = ImageStyle(settings.DEFAULT_IMAGE_STYLE)

        # 장면 정보 생성 (gpt-4o-mini)
        scene_info = await OpenAIService.generate_scene_info(scene, style)

        system_prompt = OpenAIService.get_system_prompt("image_prompt")

        try:
            # OpenAI API 호출
            response = client.chat.completions.create(
                model=openai_config.MODEL,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": scene_info},
                ],
                max_tokens=openai_config.MAX_TOKENS,
                temperature=openai_config.TEMPERATURE,
            )

            image_prompt = response.choices[0].message.content.strip()
            negative_prompt = OpenAIService.get_negative_prompt(style)

            # app_logger.debug(f"Positive Prompt: \n{image_prompt}")
            # app_logger.debug(f"Negative Prompt: {negative_prompt}")

            # 생성된 프롬프트 반환
            return image_prompt, negative_prompt, scene_info

        except Exception as e:
            # 오류 발생 시 기본 프롬프트 반환
            app_logger.error(f"OpenAI API 오류: {str(e)}")
            raise e


# 서비스 인스턴스 생성
openai_service = OpenAIService()
