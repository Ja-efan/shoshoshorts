"""
이미지 다운로드 서비스
"""
import os
import aiohttp
import aiofiles
import time
from datetime import datetime
import pytz
from typing import Optional
from app.core.config import settings
from app.services.s3_service import s3_service

class DownloadService:
    """이미지 다운로드 서비스"""
    
    @staticmethod
    async def download_image(image_url: str, story_id: int, scene_id: int) -> Optional[str]:
        """
        URL에서 이미지를 다운로드하여 로컬에 저장합니다.
        
        Args:
            image_url: 다운로드할 이미지의 URL
            story_id: 스토리 ID
            scene_id: 장면 ID
            
        Returns:
            저장된 이미지의 로컬 경로 또는 None (실패 시)
        """
        try:
            # ID를 형식에 맞게 변환 (story_id: 8자리, scene_id: 4자리)
            formatted_story_id = f"{story_id:08d}"  # 8자리 (예: 00000001)
            formatted_scene_id = f"{scene_id:04d}"  # 4자리 (예: 0001)
            
            # 기본 디렉토리 설정 (AI-IMAGE/{bucket_name})
            # s3_bucket_name = settings.S3_BUCKET_NAME #이전 경로로
            s3_bucket_name = s3_service.s3_bucket

            # 버킷 이름에서 프로토콜과 슬래시 제거
            if s3_bucket_name and s3_bucket_name.startswith("s3://"):
                s3_bucket_name = s3_bucket_name.replace("s3://", "")
            # 끝의 슬래시 제거
            if s3_bucket_name and s3_bucket_name.endswith("/"):
                s3_bucket_name = s3_bucket_name.rstrip("/")
                
            base_dir = os.path.join("images", s3_bucket_name)
            os.makedirs(base_dir, exist_ok=True)
            
            # 스토리 ID 기준 디렉토리
            story_dir = os.path.join(base_dir, formatted_story_id)
            os.makedirs(story_dir, exist_ok=True)
            
            # 스토리 내 이미지 디렉토리 생성
            images_dir = os.path.join(story_dir, "images")
            os.makedirs(images_dir, exist_ok=True)
            
            # 현재 타임스탬프 (한국 시간, KST)
            kst = pytz.timezone('Asia/Seoul')
            now = datetime.now(kst)
            timestamp = now.strftime("%Y%m%d_%H%M%S")
            
            # 저장할 파일명 생성 (scene_id와 타임스탬프 사용)
            filename = f"{formatted_scene_id}_{timestamp}.jpg"
            save_path = os.path.join(images_dir, filename)
            
            # 이미지 다운로드 - SSL 검증 비활성화
            connector = aiohttp.TCPConnector(verify_ssl=False)
            async with aiohttp.ClientSession(connector=connector) as session:
                print(f"다운로드 시도 중: {image_url}")
                async with session.get(image_url) as response:
                    if response.status != 200:
                        print(f"이미지 다운로드 실패: 상태 코드 {response.status}")
                        return None
                    
                    # 이미지를 파일로 저장
                    async with aiofiles.open(save_path, 'wb') as f:
                        await f.write(await response.read())
            
            print(f"이미지가 저장되었습니다: {save_path}")
            return save_path
            
        except Exception as e:
            print(f"이미지 다운로드 중 오류 발생: {str(e)}")
            return None

# 서비스 인스턴스 생성
download_service = DownloadService() 