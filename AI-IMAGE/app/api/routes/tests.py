"""
장면 정보 생성 테스트 모듈
"""
from fastapi import APIRouter
from app.schemas.models import Scene

router = APIRouter(prefix="/tests", tags=["tests"])

@router.post("/test/generate_scene_info")    
async def test_generate_scene_info(scene: Scene):
    """장면 정보 생성 테스트"""
    # 장면 정보 생성
    scene_info = f"""
장면 제목: {scene.story_metadata.title}
장면 ID: {scene.scene_id}
장면 스타일: ANIME

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
    
    # OpenAI 서비스를 사용하여 장면 정보 생성
    from app.services.openai_service import openai_service
    
    # OpenAI API 호출 및 결과 반환
    response = await openai_service.generate_scene_info(scene_info)
    
    return response


