"""
장면 정보 생성 테스트 모듈
"""
import json
from fastapi import APIRouter
from app.schemas.models import Scene
from app.services.openai_service import OpenAIService
from app.services.utils import encode_image_to_base64
from app.core.logger import app_logger

router = APIRouter(prefix="/tests", tags=["tests"])


@router.post("/convert_image_to_base64")
async def test_convert_image_to_base64():
    """이미지 파일을 Base64로 인코딩하여 반환"""
    image_path = "images/shoshoshorts/00000001/images/0002_20250331_162613.jpg"
    response = encode_image_to_base64(image_path)
    return response


@router.post("/scene_info")
async def test_generate_scene_info(scene: Scene):
    """이미지 프롬프트 생성 테스트"""
    app_logger.info(f"장면 정보 생성 테스트 시작: \n{json.dumps(scene.model_dump(), ensure_ascii=False, indent=2)}")
    response = await OpenAIService.generate_scene_info(scene)
    app_logger.info(f"장면 정보 생성 테스트 완료: \n{json.dumps(response.model_dump(), ensure_ascii=False, indent=2)}")
    return response


@router.post("/image_prompt")
async def test_generate_image_prompt(scene: Scene):
    """이미지 프롬프트 생성 테스트"""
    app_logger.info(f"이미지 프롬프트 생성 테스트 시작: \n{json.dumps(scene.model_dump(), ensure_ascii=False, indent=2)}")
    response = await OpenAIService.generate_image_prompt(scene)
    prompt = response[0]
    negative_prompt = response[1]
    scene_info = response[2]
    
    app_logger.info(f"이미지 프롬프트 생성 테스트 완료:")
    app_logger.info(f"이미지 프롬프트: {prompt}")
    app_logger.info(f"부정 프롬프트: {negative_prompt}")
    app_logger.info(f"장면 정보: \n{json.dumps(scene_info.model_dump(), ensure_ascii=False, indent=2)}")
    
    return prompt, negative_prompt, scene_info
