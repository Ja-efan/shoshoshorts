import os
import boto3
from dotenv import load_dotenv
from typing import Optional
import logging
from io import BytesIO

# .env 파일 로드
load_dotenv()

# 개발 환경용 환경 변수
DEV_AWS_KEY = os.getenv("DEV_AWS_KEY")
DEV_AWS_SECRET = os.getenv("DEV_AWS_SECRET")
DEV_S3_BUCKET_NAME = os.getenv("DEV_S3_BUCKET_NAME")
DEV_S3_REGION = os.getenv("DEV_S3_REGION")

# AWS 환경 설정을 위한 기본 환경 변수
RELEASE_AWS_KEY = os.getenv("RELEASE_AWS_KEY", DEV_AWS_KEY)
RELEASE_AWS_SECRET = os.getenv("RELEASE_AWS_SECRET", DEV_AWS_SECRET)
RELEASE_S3_BUCKET_NAME = os.getenv("RELEASE_S3_BUCKET_NAME", DEV_S3_BUCKET_NAME)
RELEASE_S3_REGION = os.getenv("RELEASE_S3_REGION", DEV_S3_REGION)

# 로깅 설정
logger = logging.getLogger(__name__)

def set_environment(is_dev_environment: bool):
    """
    개발 환경 또는 프로덕션 환경에 따라 S3 관련 환경 변수를 설정하는 함수
    
    Args:
        is_dev_environment: 개발 환경 여부 (True: 개발 환경, False: 프로덕션 환경)
    """
    global current_aws_key, current_aws_secret, current_s3_bucket_name, current_s3_region
    
    if is_dev_environment:
        current_aws_key = DEV_AWS_KEY
        current_aws_secret = DEV_AWS_SECRET
        current_s3_bucket_name = DEV_S3_BUCKET_NAME
        current_s3_region = DEV_S3_REGION
        logger.info("개발 환경 S3 설정이 적용되었습니다.")
    else:
        current_aws_key = RELEASE_AWS_KEY
        current_aws_secret = RELEASE_AWS_SECRET
        current_s3_bucket_name = RELEASE_S3_BUCKET_NAME
        current_s3_region = RELEASE_S3_REGION
        logger.info("프로덕션 환경 S3 설정이 적용되었습니다.")


def upload_binary_to_s3(binary_data: bytes, object_name: str, content_type: str) -> dict:
    """
    바이너리 데이터를 S3 버킷에 직접 업로드하고 URL을 반환하는 함수
    
    Args:
        binary_data: 업로드할 바이너리 데이터
        object_name: S3에 저장될 객체 이름
        content_type: 컨텐츠 타입 (예: 'audio/mp3')
        
    Returns:
        업로드 결과 정보 (성공 여부, URL, 오류 메시지 등)
    """
    # AWS 자격 증명 확인
    if not all([current_aws_key, current_aws_secret, current_s3_bucket_name, current_s3_region]):
        error_msg = "AWS 자격 증명 또는 S3 설정이 누락되었습니다. .env 파일을 확인하세요."
        logger.error(error_msg)
        return {
            "success": False,
            "error": error_msg,
            "url": None
        }
    
    # S3 클라이언트 생성
    try:
        s3_client = boto3.client(
            's3',
            aws_access_key_id=current_aws_key,
            aws_secret_access_key=current_aws_secret,
            region_name=current_s3_region
        )
        
        # 바이너리 데이터를 BytesIO 객체로 변환
        file_obj = BytesIO(binary_data)
        
        # 파일 업로드
        logger.info(f"S3에 바이너리 데이터 업로드 중: {len(binary_data)} 바이트 -> {current_s3_bucket_name}/{object_name}")
        print(f"S3에 바이너리 데이터 업로드 중: {len(binary_data)} 바이트 -> {current_s3_bucket_name}/{object_name}")
        
        # 바이너리 데이터 업로드
        s3_client.upload_fileobj(
            file_obj, 
            current_s3_bucket_name, 
            object_name,
            ExtraArgs={
                'ContentType': content_type
            }
        )
        
        # S3 URL 생성
        url = f"https://{current_s3_bucket_name}.s3.{current_s3_region}.amazonaws.com/{object_name}"
        
        logger.info(f"바이너리 데이터 업로드 성공: {url}")
        print(f"바이너리 데이터 업로드 성공: {url}")
        return {
            "success": True,
            "url": url,
            "bucket": current_s3_bucket_name,
            "key": object_name,
            "region": current_s3_region,
            "size": len(binary_data)
        }
    
    except Exception as e:
        error_msg = f"S3 바이너리 업로드 중 오류 발생: {str(e)}"
        print(f"S3 바이너리 업로드 중 오류 발생: {str(e)}")
        logger.error(error_msg)
        return {
            "success": False,
            "error": error_msg,
            "url": None
        }