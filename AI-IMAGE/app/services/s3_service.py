"""
AWS S3 이미지 업로드 서비스
"""
import os
import boto3
from botocore.exceptions import NoCredentialsError
from dotenv import load_dotenv
import time
from datetime import datetime
import pytz
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
    
    async def upload_image(self, image_path: str, story_id: int, scene_id: int) -> Optional[str]:
        """
        이미지를 S3에 업로드합니다.
        
        Args:
            image_path: 로컬 이미지 파일 경로
            story_id: 스토리 ID
            scene_id: 장면 ID
            
        Returns:
            업로드된 이미지의 S3 URL 또는 None (실패 시)
        """
        if not self.s3_client or not self.s3_bucket:
            print("S3 자격 증명 또는 버킷 이름이 설정되지 않았습니다.")
            return None
        
        try:
            # ID를 형식에 맞게 변환 (story_id: 8자리, scene_id: 4자리)
            formatted_story_id = f"{story_id:08d}"  # 8자리 (예: 00000001)
            formatted_scene_id = f"{scene_id:04d}"  # 4자리 (예: 0001)
            
            # 현재 파일명에서 타임스탬프를 추출하거나 새 타임스탬프 생성
            base_filename = os.path.basename(image_path)
            
            # 이미 타임스탬프가 있는 파일명이면 그대로 사용, 아니면 새로 생성
            if '_20' in base_filename:  # 타임스탬프가 포함된 파일명인지 확인
                # 형식에 맞게 scene_id 부분 교체
                parts = base_filename.split('_', 1)
                if len(parts) >= 2:
                    new_filename = f"{formatted_scene_id}_{parts[1]}"
                else:
                    new_filename = base_filename
            else:
                # 새 타임스탬프 생성 (한국 시간, KST)
                kst = pytz.timezone('Asia/Seoul')
                now = datetime.now(kst)
                timestamp = now.strftime("%Y%m%d_%H%M%S")
                file_ext = os.path.splitext(base_filename)[1]  # 파일 확장자
                new_filename = f"{formatted_scene_id}_{timestamp}{file_ext}"
            
            # S3에 업로드할 객체 키 생성 (경로)
            object_key = f"{formatted_story_id}/images/{new_filename}"
            
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