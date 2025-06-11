from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional

# 요청 모델 정의
class Character(BaseModel):
    name: str = Field(description="캐릭터 이름")
    gender: str = Field(description="캐릭터 성별")
    properties: str = Field(description="캐릭터 특성 정보")
    voiceCode: str = Field(description="캐릭터 보이스 코드")

class ScriptRequest(BaseModel):
    storyId: int = Field(description="스토리 고유 식별자")
    storyTitle: str = Field(description="스토리 제목")
    characterArr: List[Character] = Field(description="스토리에 등장하는 캐릭터 목록")
    story: str = Field(description="스토리 내용 (텍스트)")
    narVoiceCode: str = Field(description="나레이션 보이스 코드", default="4JJwo477JUAx3HV0T7n7")

# 응답 모델 정의
class ScriptResponse(BaseModel):
    script_json: Dict[str, Any] = Field(description="변환된 스토리 JSON") 