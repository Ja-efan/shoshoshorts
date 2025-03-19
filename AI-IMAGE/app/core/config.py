"""
애플리케이션 설정 관리
"""
import os
import time 
from pydantic_settings import BaseSettings
from dotenv import load_dotenv
import jwt

# 환경 변수 로드
load_dotenv()

def encode_jwt_token(ak, sk):
    headers = {
        "alg": "HS256",
        "typ": "JWT"
    }

    payload = {
        "iss": ak,
        "exp": int(time.time()) + 1800,
        "nbf": int(time.time()) - 5
    }
    
    return jwt.encode(payload, sk, headers=headers)
    
    
class Settings(BaseSettings):
    """애플리케이션 설정"""
    API_V1_STR: str = "/api/v1"
    PROJECT_NAME: str = "Kling AI 이미지 생성 API"
    
    # Kling AI API 설정
    KLING_ACCESS_KEY: str = os.getenv("KLING_ACCESS_KEY", "")
    KLING_SECRET_KEY: str = os.getenv("KLING_SECRET_KEY", "")
    KLING_API_URL: str = "https://api.klingai.com/v1/images/generations"

    # OpenAI API 설정
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")

    # S3 설정
    S3_BUCKET_NAME: str = os.getenv("S3_BUCKET_NAME", "")
    S3_REGION: str = os.getenv("S3_REGION", "ap-northeast-2")
    S3_ACCESS_KEY: str = os.getenv("S3_ACCESS_KEY", "")
    S3_SECRET_KEY: str = os.getenv("S3_SECRET_KEY", "")

    # JWT 토큰 설정
    JWT_TOKEN: str = ""

    class Config:
        case_sensitive = True

# 설정 인스턴스 생성
settings = Settings()

# JWT 토큰 생성
settings.JWT_TOKEN = encode_jwt_token(settings.KLING_ACCESS_KEY, settings.KLING_SECRET_KEY)