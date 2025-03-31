"""
장면 정보 생성 테스트 모듈
"""
from fastapi import APIRouter
from app.schemas.models import Scene
from app.services.openai_service import OpenAIService
router = APIRouter(prefix="/tests", tags=["tests"])

@router.post("/test/generate_scene_info")    
async def test_generate_scene_info(scene: Scene):
    """장면 정보 생성 테스트"""
    
    # OpenAI API 호출 및 결과 반환
    response = await OpenAIService.generate_scene_info(scene)
    
    return response


