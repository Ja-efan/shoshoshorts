from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    PROJECT_NAME: str = "Novel to Audio API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    
    # TTS 모델 설정
    TTS_MODEL_NAME: str = "tts_models/ko/kss/v2"
    
    # LLM 모델 설정
    LLM_MODEL_NAME: str = "gogamza/kobart-base-v2"
    
    class Config:
        case_sensitive = True

settings = Settings() 