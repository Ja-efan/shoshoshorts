"""
이미지 다운로드 서비스
"""

import os
import aiohttp
import aiofiles
import time
import asyncio
from datetime import datetime
import pytz
from typing import Optional, Tuple
from app.core.logger import app_logger
from app.services.s3_service import s3_service


class DownloadService:
    """이미지 다운로드 서비스"""

    @staticmethod
    async def download_image(
        image_url: str, story_id: int, scene_id: int
    ) -> Optional[str]:
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
            kst = pytz.timezone("Asia/Seoul")
            now = datetime.now(kst)
            timestamp = now.strftime("%Y%m%d_%H%M%S")

            # 저장할 파일명 생성 (scene_id와 타임스탬프 사용)
            filename = f"{formatted_scene_id}_{timestamp}.jpg"
            save_path = os.path.join(images_dir, filename)

            # 재시도 설정
            max_retries = 5
            retry_delay = 2  # 초
            
            # 이미지 다운로드
            success = await DownloadService._download_with_retry(image_url, save_path, max_retries, retry_delay)
            
            if success:
                app_logger.info(f"Image successfully saved to: {save_path}")
                return save_path
            else:
                return None

        except Exception as e:
            app_logger.error(f"Error occurred while downloading image: {str(e)}", exc_info=True)
            return None
            
    @staticmethod
    async def _download_with_retry(image_url: str, save_path: str, max_retries: int, retry_delay: int) -> bool:
        """이미지 다운로드를 재시도하는 헬퍼 메서드"""
        
        for attempt in range(1, max_retries + 1):
            try:
                # 타임아웃 설정 - 연결과 읽기 타임아웃 분리
                timeout = aiohttp.ClientTimeout(
                    connect=20,    # 연결 타임아웃
                    sock_read=60,  # 읽기 타임아웃
                    total=90       # 전체 요청 타임아웃
                )
                
                # SSL 검증 비활성화 및 연결 설정
                connector = aiohttp.TCPConnector(
                    verify_ssl=False, 
                    force_close=True,
                    limit=10       # 동시 연결 제한
                )
                
                async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                    app_logger.info(f"다운로드 시도 {attempt}/{max_retries}: {image_url}...")
                    
                    try:
                        async with session.get(image_url) as response:
                            if response.status != 200:
                                app_logger.error(
                                    f"이미지 다운로드 실패: 상태 코드 {response.status}, URL: {image_url}"
                                )
                                if attempt < max_retries:
                                    await asyncio.sleep(retry_delay)
                                    continue
                                return False

                            # 이미지 데이터를 메모리에 먼저 읽어옴
                            image_data = await response.read()
                            
                            # 이미지를 파일로 저장
                            async with aiofiles.open(save_path, "wb") as f:
                                await f.write(image_data)
                                
                            return True
                            
                    except aiohttp.ClientError as client_err:
                        app_logger.error(f"HTTP 요청 실패 (시도 {attempt}/{max_retries}): {str(client_err)}, URL: {image_url}")
                        if attempt < max_retries:
                            await asyncio.sleep(retry_delay)
                            continue
                        return False
                        
            except asyncio.TimeoutError:
                app_logger.error(f"타임아웃 발생 (시도 {attempt}/{max_retries}): URL: {image_url}")
                if attempt < max_retries:
                    await asyncio.sleep(retry_delay)
                    continue
                return False
                
            except Exception as e:
                app_logger.error(f"이미지 다운로드 중 예외 발생 (시도 {attempt}/{max_retries}): {str(e)}, URL: {image_url}", exc_info=True)
                if attempt < max_retries:
                    await asyncio.sleep(retry_delay)
                    continue
                return False
                
        return False


# 서비스 인스턴스 생성
download_service = DownloadService()
