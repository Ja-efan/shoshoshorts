import os
import boto3
from dotenv import load_dotenv
from typing import Optional
import logging
from io import BytesIO

# .env 파일 로드
load_dotenv()

# 환경 변수에서 AWS 자격 증명 및 S3 설정 가져오기
AWS_KEY = os.getenv("AWS_KEY")
AWS_SECRET = os.getenv("AWS_SECRET")
S3_BUCKET_NAME = os.getenv("S3_BUCKET_NAME")
S3_REGION = os.getenv("S3_REGEON")

# 로깅 설정
logger = logging.getLogger(__name__)

def upload_file_to_s3(file_path: str, object_name: Optional[str] = None) -> dict:
    """
    지정된 파일을 S3 버킷에 업로드하고 URL을 반환하는 함수
    
    Args:
        file_path: 업로드할 파일의 경로
        object_name: S3에 저장될 객체 이름 (지정하지 않으면 파일 이름 사용)
        
    Returns:
        업로드 결과 정보 (성공 여부, URL, 오류 메시지 등)
    """
    # AWS 자격 증명 확인
    if not all([AWS_KEY, AWS_SECRET, S3_BUCKET_NAME, S3_REGION]):
        error_msg = "AWS 자격 증명 또는 S3 설정이 누락되었습니다. .env 파일을 확인하세요."
        logger.error(error_msg)
        return {
            "success": False,
            "error": error_msg,
            "url": None
        }
    
    # 파일 존재 여부 확인
    if not os.path.exists(file_path):
        error_msg = f"파일이 존재하지 않습니다: {file_path}"
        logger.error(error_msg)
        return {
            "success": False,
            "error": error_msg,
            "url": None
        }
    
    # S3 객체 이름 설정 (지정하지 않은 경우 파일 이름 사용)
    if object_name is None:
        object_name = os.path.basename(file_path)
    
    # S3 클라이언트 생성
    try:
        s3_client = boto3.client(
            's3',
            aws_access_key_id=AWS_KEY,
            aws_secret_access_key=AWS_SECRET,
            region_name=S3_REGION
        )
        
        # 파일 업로드
        logger.info(f"S3에 파일 업로드 중: {file_path} -> {S3_BUCKET_NAME}/{object_name}")
        print(f"S3에 파일 업로드 중: {file_path} -> {S3_BUCKET_NAME}/{object_name}")
        s3_client.upload_file(file_path, S3_BUCKET_NAME, object_name)
        
        # S3 URL 생성
        url = f"https://{S3_BUCKET_NAME}.s3.{S3_REGION}.amazonaws.com/{object_name}"
        
        logger.info(f"파일 업로드 성공: {url}")
        print(f"파일 업로드 성공: {url}")
        return {
            "success": True,
            "url": url,
            "bucket": S3_BUCKET_NAME,
            "key": object_name,
            "region": S3_REGION
        }
    
    except Exception as e:
        error_msg = f"S3 업로드 중 오류 발생: {str(e)}"
        print(f"S3 업로드 중 오류 발생: {str(e)}")
        logger.error(error_msg)
        return {
            "success": False,
            "error": error_msg,
            "url": None
        }

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
    if not all([AWS_KEY, AWS_SECRET, S3_BUCKET_NAME, S3_REGION]):
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
            aws_access_key_id=AWS_KEY,
            aws_secret_access_key=AWS_SECRET,
            region_name=S3_REGION
        )
        
        # 바이너리 데이터를 BytesIO 객체로 변환
        file_obj = BytesIO(binary_data)
        
        # 파일 업로드
        logger.info(f"S3에 바이너리 데이터 업로드 중: {len(binary_data)} 바이트 -> {S3_BUCKET_NAME}/{object_name}")
        print(f"S3에 바이너리 데이터 업로드 중: {len(binary_data)} 바이트 -> {S3_BUCKET_NAME}/{object_name}")
        
        # 바이너리 데이터 업로드
        s3_client.upload_fileobj(
            file_obj, 
            S3_BUCKET_NAME, 
            object_name,
            ExtraArgs={
                'ContentType': content_type
            }
        )
        
        # S3 URL 생성
        url = f"https://{S3_BUCKET_NAME}.s3.{S3_REGION}.amazonaws.com/{object_name}"
        
        logger.info(f"바이너리 데이터 업로드 성공: {url}")
        print(f"바이너리 데이터 업로드 성공: {url}")
        return {
            "success": True,
            "url": url,
            "bucket": S3_BUCKET_NAME,
            "key": object_name,
            "region": S3_REGION,
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
