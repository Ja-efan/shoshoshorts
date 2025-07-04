"""
API 요청 및 응답을 위한 모델 정의
"""
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field

class Character(BaseModel):
    """등장 인물 모델"""
    name: str
    gender: int  # 1: 남자, 2: 여자
    description: str

class StoryMetadata(BaseModel):
    """스토리 메타데이터 모델"""
    title: str
    story_id: int
    characters: List[Character]

class Audio(BaseModel):
    """오디오 모델"""
    type: str  # narration, dialogue, sound 등
    text: str
    character: Optional[str] = None  # 대화 유형일 경우에만 필요
    emotion: Optional[str] = None  # 대화 유형일 경우 감정 정보

class Scene(BaseModel):
    """장면 모델"""
    story_metadata: StoryMetadata
    scene_id: int
    audios: List[Audio]

class ImageGenerationResponse(BaseModel):
    """이미지 생성 응답 모델"""
    scene_id: int
    image_prompt: str
    image_url: str 

class GenerateImageStableDiffusionResponse(BaseModel):
    """OpenAI 응답 형식 모델"""
    prompt: str
    negative_prompt: str
    sampler: str 
    cfg_scale: float
    steps: int
    clip_skip: int