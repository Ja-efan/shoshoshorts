"""
OpenAI 서비스
"""

import os
import json
from enum import Enum
from typing import Optional, Union, Tuple

from openai import OpenAI

from app.schemas.models import (
    Scene,
    SceneInfo,
    SceneMetadata,
    CharacterNSceneSummary,
    PreviousSceneData,
    Character,
)
from app.core.config import settings
from app.core.logger import app_logger
from app.core.api_config import openai_config, klingai_config
from app.services.klingai_service import KlingAIService

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
    DISNEY_PIXAR = "DISNEY_PIXAR"

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
    async def get_system_prompt(which: str) -> str:
        """
        시스템 프롬프트를 반환합니다.
        """
        if which == "scene_info":
            return open(openai_config.SYSTEM_PROMPT["scene_info"], "r").read()
        elif which == "image_prompt":
            return open(openai_config.SYSTEM_PROMPT["image_prompt"], "r").read()

    @staticmethod
    async def get_negative_prompt(style: ImageStyle) -> str:
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
    async def get_scene_data_path(story_id: int, scene_id: int) -> str:
        """씬 데이터 JSON 파일 경로를 반환합니다."""
        # 데이터 저장 디렉토리 생성
        story_base_dir = os.path.join("data", "stories")
        data_dir = os.path.join(story_base_dir, f"{story_id:08d}")
        os.makedirs(data_dir, exist_ok=True)

        # 씬별 JSON 파일 경로
        return os.path.join(data_dir, f"{scene_id:04d}.json")

    @staticmethod
    async def get_previous_scene_data(
        story_id: int, scene_id: int
    ) -> Optional[PreviousSceneData]:
        """이전 씬의 정보와 이미지 프롬프트를 가져옵니다."""
        if scene_id <= 1:
            app_logger.info(
                f"Previous scene data is not available for scene_id: {scene_id}"
            )
            return None

        previous_scene_id = scene_id - 1
        previous_scene_data_path = await OpenAIService.get_scene_data_path(
            story_id, previous_scene_id
        )

        if not os.path.exists(previous_scene_data_path):
            app_logger.info(
                f"Previous scene data file not found: {previous_scene_data_path}"
            )
            return None
            
        # 기존 데이터 마이그레이션 (문자열 -> 딕셔너리)
        KlingAIService.legacy_scene_data_migration(story_id, previous_scene_id)

        try:
            with open(previous_scene_data_path, "r", encoding="utf-8") as f:
                previous_scene_data = json.load(f)
            
            # scene_info가 문자열인 경우 JSON으로 파싱
            if isinstance(previous_scene_data.get("scene_info"), str):
                try:
                    scene_info_dict = json.loads(previous_scene_data["scene_info"])
                    previous_scene_data["scene_info"] = scene_info_dict
                    app_logger.info(f"Successfully parsed scene_info string to dictionary")
                except json.JSONDecodeError as je:
                    app_logger.error(f"Failed to parse scene_info string: {str(je)}")
                    return None
            
            # scene_info가 딕셔너리인지 확인
            if not isinstance(previous_scene_data.get("scene_info"), dict):
                app_logger.error(f"Invalid scene_info format in {previous_scene_data_path}")
                return None
                
            try:
                # SceneMetadata 생성 시도
                scene_metadata = SceneMetadata(
                    title=previous_scene_data["scene_info"]["scene_metadata"]["title"],
                    scene_id=previous_scene_data["scene_info"]["scene_metadata"]["scene_id"],
                    style=previous_scene_data["scene_info"]["scene_metadata"]["style"]
                )
                
                # 이전 씬 데이터 반환
                return PreviousSceneData(
                    scene_info=SceneInfo(
                        characters=[Character(**char) for char in previous_scene_data["scene_info"]["characters"]],
                        scene_content=previous_scene_data["scene_info"]["scene_content"],
                        scene_summary=previous_scene_data["scene_info"]["scene_summary"],
                        scene_metadata=scene_metadata
                    ),
                    image_prompt=previous_scene_data["image_prompt"],
                )
            except KeyError as ke:
                app_logger.error(f"Missing key in scene_info: {str(ke)}")
                return None

        except Exception as e:
            app_logger.error(f"Previous scene data retrieval error: {str(e)}")
            return None

    @staticmethod
    async def generate_scene_info(
        scene: Scene, style: Union[str, ImageStyle], system_prompt: str | None = None
    ) -> SceneInfo:
        """
        장면 정보를 바탕으로 이미지 프롬프트 생성에 필요한 장면 정보(scene_info)를 생성합니다.
        scene_info 형식:
            {
                "characters": [
                    {
                        "name": "캐릭터 이름",
                        "gender": "남자/여자",
                        "description": "캐릭터 설명"
                    }
                ],
                "scene_content": "장면 내용",
                "scene_summary": "장면 요약"
            }
        """

        # 시스템 프롬프트 (scene_info)
        if system_prompt is None:
            system_prompt = await OpenAIService.get_system_prompt("scene_info")
        else:
            system_prompt = system_prompt

        # TODO: 캐릭터가 없는 경우 (나레이션만 존재하는 경우) 처리 -> 일단 JAVA에서 기본 캐릭터 추가해서 전송

        # 장면에 등장하는 '캐릭터' 추출 및 '장면 요약' 생성 (gpt-4o-mini)
        response = client.beta.chat.completions.parse(
            model=openai_config.SCENE_INFO_GENERATION_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {
                    "role": "user",
                    "content": json.dumps(
                        scene.model_dump(), ensure_ascii=False, indent=2
                    ),
                },
            ],
            max_tokens=openai_config.SCENE_INFO_MAX_TOKENS,
            temperature=openai_config.SCENE_INFO_TEMPERATURE,
            response_format=CharacterNSceneSummary,
        )
        characters_n_scene_summary = response.choices[0].message.parsed

        """
        SCENE_CONTENT 추출 
        형식:  
            "{'type': 'narration', 'text': '기사님도 좀 이상하셨는지 물어보셨어.', 'character': 'narration', 'emotion': None}\n
            {'type': 'dialogue', 'text': '할아버지~ 안 내리세요?', 'character': '버스 기사님', 'emotion': 'concern'}"
        """
        scene_audios = scene.audios
        scene_content_list = []
        for audio in scene_audios:
            audio_dict = dict(audio.model_dump())
            audio_str = f"{audio_dict}"
            scene_content_list.append(audio_str)
        scene_content = "\n".join(scene_content_list)

        return SceneInfo(
            characters=characters_n_scene_summary.characters,
            scene_content=scene_content,
            scene_summary=characters_n_scene_summary.scene_summary,
            scene_metadata=SceneMetadata(
                title=scene.story_metadata.title,
                scene_id=scene.scene_id,
                style=style,
            ),
        )

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
                    f"Invalid image style '{style}'. Using default value '{ImageStyle.DISNEY_PIXAR}'."
                )
                style = ImageStyle.DISNEY_PIXAR

        # 캐릭터 성별 정보
        for character in scene.story_metadata.characters:
            character.gender = "남자" if character.gender == 0 else "여자"

        # 장면 정보 생성 (gpt-4o-mini)
        """
        scene_info 형식:
            {
                "characters": [
                    {
                        "name": "캐릭터 이름",
                        "gender": "남자/여자",
                        "description": "캐릭터 설명"
                    }
                ],
                "scene_content": "장면 내용",
                "scene_summary": "장면 요약"
            }
        """
        scene_info = await OpenAIService.generate_scene_info(
            scene, style
        )  # SceneInfo 객체 반환

        ####################################### 이전 씬 정보 및 이미지 프롬프트 #######################################
        previous_scene_data = None
        if klingai_config.USE_PREVIOUS_SCENE_DATA:  # default: True
            previous_scene_data = await OpenAIService.get_previous_scene_data(
                scene.story_metadata.story_id, scene.scene_id
            )  # PreviousSceneData | None 반환

        #############################################################################################################
        if previous_scene_data:
            # 구조화된 딕셔너리 생성
            content_dict = {
                "current_scene": scene_info.model_dump(),
                "previous_scene": previous_scene_data.scene_info.model_dump(),
                "previous_image_prompt": previous_scene_data.image_prompt["original_prompt"]
            }

            # 딕셔너리를 JSON 문자열로 변환
            content = json.dumps(content_dict, ensure_ascii=False, indent=2)
        else:
            # 이전 씬 정보가 없는 경우 현재 씬 정보만 포함
            content = json.dumps({"current_scene": scene_info.model_dump()}, ensure_ascii=False, indent=2)

        # app_logger.info(f"Content for Generate Image Prompt: \n{content}")

        try:
            # 시스템 프롬프트 (image_prompt)
            system_prompt = await OpenAIService.get_system_prompt("image_prompt")  # str 반환
            
            # OpenAI API 호출
            response = client.chat.completions.create(
                model=openai_config.IMAGE_PROMPT_GENERATION_MODEL,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": content},
                ],
                max_tokens=openai_config.IMAGE_PROMPT_MAX_TOKENS,
                temperature=openai_config.IMAGE_PROMPT_TEMPERATURE,
            )

            image_prompt = response.choices[0].message.content.strip()
            negative_prompt = None
            # negative_prompt = OpenAIService.get_negative_prompt(style)

            # app_logger.debug(f"Positive Prompt: \n{image_prompt}")
            # app_logger.debug(f"Negative Prompt: {negative_prompt}")

            # 생성된 프롬프트 반환
            return {
                "image_prompt": image_prompt,
                "negative_prompt": negative_prompt,
                "scene_info": scene_info,
            }

        except Exception as e:
            # 오류 발생 시 기본 프롬프트 반환
            app_logger.error(f"OpenAI API 오류: {str(e)}")
            raise e


# 서비스 인스턴스 생성
openai_service = OpenAIService()
