"""
기본 애플리케이션 설정 관리
"""

import os
from dotenv import load_dotenv
from pydantic_settings import BaseSettings

# 환경 변수 로드
load_dotenv()


class Settings(BaseSettings):
    """기본 애플리케이션 설정"""

    API_V1_STR: str = "/api/v1"
    PROJECT_NAME: str = "Kling AI 이미지 생성 API"

    # Prompt 파일 경로
    PROMPT_DIR: str = "app/prompts"
    SYSTEM_PROMPT_DIR: str = os.path.join(PROMPT_DIR, "system-prompts")

    # API 비밀번호
    API_PWD: str = os.getenv("API_PWD")

    # prod로 설정시 docs_url, redoc_url, openapi_url을 볼 수 없도록 설정
    ENV: str = os.getenv("ENV", "development")  # 기본값은 development

    # S3 오류 설정
    USE_LOCAL_URL_ON_S3_FAILURE: bool = (
        os.getenv("USE_LOCAL_URL_ON_S3_FAILURE", "false").lower() == "true"
    )

    class Config:
        case_sensitive = True


# 설정 인스턴스 생성
settings = Settings()
