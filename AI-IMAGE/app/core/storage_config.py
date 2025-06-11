"""
스토리지 관련 설정 관리
"""

import os
from dotenv import load_dotenv

# 환경 변수 로드
load_dotenv()


class S3Config:
    """S3 스토리지 관련 설정"""

    # DEV S3 설정
    BUCKET_NAME: str = os.getenv("S3_BUCKET_NAME", "")
    REGION: str = os.getenv("S3_REGION", "ap-northeast-2")
    ACCESS_KEY: str = os.getenv("S3_ACCESS_KEY", "")
    SECRET_KEY: str = os.getenv("S3_SECRET_KEY", "")

    # Release S3 설정
    RELEASE_BUCKET_NAME: str = os.getenv("RELEASE_S3_BUCKET_NAME", BUCKET_NAME)
    RELEASE_REGION: str = os.getenv("RELEASE_S3_REGION", "us-east-2")
    RELEASE_ACCESS_KEY: str = os.getenv("RELEASE_S3_ACCESS_KEY", ACCESS_KEY)
    RELEASE_SECRET_KEY: str = os.getenv("RELEASE_S3_SECRET_KEY", SECRET_KEY)


# 설정 인스턴스 생성
s3_config = S3Config()
