"""
AWS S3 이미지 업로드 서비스
"""
import os
import boto3
from botocore.exceptions import NoCredentialsError
from dotenv import load_dotenv
import uuid
from typing import Optional
from app.core.config import settings

# 환경 변수 로드 (강제 재로드)
load_dotenv(override=True)

class S3Service:
    """AWS S3에 이미지를 업로드하는 서비스"""
    
    def __init__(self):
        """S3 클라이언트 초기화"""
        self.aws_access_key = settings.S3_ACCESS_KEY
        self.aws_secret_key = settings.S3_SECRET_KEY
        
        # 버킷 이름 가져오기 및 정제
        s3_bucket = settings.S3_BUCKET_NAME
        
        # 버킷 이름 디버깅 출력
        print(f"원본 S3_BUCKET_NAME: {s3_bucket}")
        
        # 버킷 이름에서 프로토콜과 슬래시 제거
        if s3_bucket and s3_bucket.startswith("s3://"):
            s3_bucket = s3_bucket.replace("s3://", "")
        
        # 끝의 슬래시 제거
        if s3_bucket and s3_bucket.endswith("/"):
            s3_bucket = s3_bucket.rstrip("/")
            
        # 정제된 버킷 이름 저장
        self.s3_bucket = s3_bucket
        self.s3_region = settings.S3_REGION
            
        print(f"정제된 S3_BUCKET_NAME: {self.s3_bucket}")
        
        # S3 클라이언트 생성
        self.s3_client = boto3.client(
            's3',
            aws_access_key_id=self.aws_access_key,
            aws_secret_access_key=self.aws_secret_key,
            region_name=self.s3_region
        ) if self.aws_access_key and self.aws_secret_key else None
        
        # 클라이언트 생성 여부 출력
        print(f"S3 클라이언트 생성 성공: {self.s3_client is not None}")
    
    async def upload_image(self, image_path: str, script_id: int, scene_id: int) -> Optional[str]:
        """
        이미지를 S3에 업로드합니다.
        
        Args:
            image_path: 로컬 이미지 파일 경로
            script_id: 스크립트 ID
            scene_id: 장면 ID
            
        Returns:
            업로드된 이미지의 S3 URL 또는 None (실패 시)
        """
        if not self.s3_client or not self.s3_bucket:
            print("S3 자격 증명 또는 버킷 이름이 설정되지 않았습니다.")
            return None
        
        try:
            # S3에 업로드할 객체 키 생성 (경로)
            file_name = os.path.basename(image_path)
            object_key = f"scripts/{script_id}/scenes/{scene_id}/{file_name}"
            
            # 파일 업로드
            self.s3_client.upload_file(image_path, self.s3_bucket, object_key)
            
            # S3 URL 생성
            s3_url = f"https://{self.s3_bucket}.s3.{self.s3_region}.amazonaws.com/{object_key}"
            return s3_url
            
        except FileNotFoundError:
            print(f"업로드할 파일을 찾을 수 없습니다: {image_path}")
            return None
        except NoCredentialsError:
            print("AWS 자격 증명이 잘못되었습니다.")
            return None
        except Exception as e:
            print(f"S3 업로드 중 오류 발생: {str(e)}")
            return None

# 서비스 인스턴스 생성
s3_service = S3Service() 