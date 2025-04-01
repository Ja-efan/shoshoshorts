"""
장면 정보 생성 테스트 모듈
"""
from fastapi import APIRouter
from app.schemas.models import Scene
from app.services.openai_service import OpenAIService
from app.services.utils import encode_image_to_base64
router = APIRouter(prefix="/tests", tags=["tests"])

@router.post("/generate_scene_info")    
async def test_generate_scene_info(scene: Scene):
    """장면 정보 생성 테스트"""
    
    # OpenAI API 호출 및 결과 반환
    response = await OpenAIService.generate_scene_info(scene)
    
    return response


@router.post("/convert_image_to_base64")
async def test_convert_image_to_base64():
    """이미지 파일을 Base64로 인코딩하여 반환"""
    image_path = "images/shoshoshorts/00000001/images/0002_20250331_162613.jpg"
    response = encode_image_to_base64(image_path)
    return response
