"""
애플리케이션 설정 관리
"""
import os
from pydantic_settings import BaseSettings
from dotenv import load_dotenv

# 환경 변수 로드
load_dotenv()

class Settings(BaseSettings):
    """애플리케이션 설정"""
    API_V1_STR: str = "/api/v1"
    PROJECT_NAME: str = "Kling AI 이미지 생성 API"
    
    # 이미지 저장 경로 
    IMAGE_SAVE_PATH: str = "app/../images"
    
    # OpenAI API 설정
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")

    # DEV S3 설정
    S3_BUCKET_NAME: str = os.getenv("S3_BUCKET_NAME", "")
    S3_REGION: str = os.getenv("S3_REGION", "ap-northeast-2")
    S3_ACCESS_KEY: str = os.getenv("S3_ACCESS_KEY", "")
    S3_SECRET_KEY: str = os.getenv("S3_SECRET_KEY", "")

    # Release S3 설정정
    RELEASE_S3_BUCKET_NAME: str = os.getenv("RELEASE_S3_BUCKET_NAME", S3_BUCKET_NAME)
    RELEASE_S3_REGION: str = os.getenv("RELEASE_S3_REGION", "us-east-2")
    RELEASE_S3_ACCESS_KEY: str = os.getenv("RELEASE_S3_ACCESS_KEY", S3_ACCESS_KEY)
    RELEASE_S3_SECRET_KEY: str = os.getenv("RELEASE_S3_SECRET_KEY", S3_SECRET_KEY)

    # API 비밀번호
    API_PWD: str = os.getenv("API_PWD")

    #prod로 설정시 docs_url, redoc_url, openapi_url을 볼 수 없도록 설정
    ENV: str = os.getenv("ENV", "development")  # 기본값은 development

    # JWT 토큰 설정
    JWT_TOKEN: str = ""

    class Config:
        case_sensitive = True

# 설정 인스턴스 생성
settings = Settings()