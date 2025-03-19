from pydantic import BaseModel
from typing import List, Optional

class NovelInput(BaseModel):
    content: str
    title: Optional[str] = None

class ScriptOutput(BaseModel):
    script: List[dict]
    title: str

class AudioOutput(BaseModel):
    audio_url: str
    duration: float
    title: str

class NovelResponse(BaseModel):
    status: str
    message: str
    data: Optional[dict] = None 