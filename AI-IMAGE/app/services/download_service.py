"""
이미지 다운로드 서비스
"""
import os
import aiohttp
import aiofiles
import uuid
from typing import Optional

class DownloadService:
    """이미지 다운로드 서비스"""
    
    @staticmethod
    async def download_image(image_url: str, script_id: int, scene_id: int) -> Optional[str]:
        """
        URL에서 이미지를 다운로드하여 로컬에 저장합니다.
        
        Args:
            image_url: 다운로드할 이미지의 URL
            script_id: 스크립트 ID
            scene_id: 장면 ID
            
        Returns:
            저장된 이미지의 로컬 경로 또는 None (실패 시)
        """
        try:
            # 이미지 저장 디렉토리 생성 (script_id 기준)
            save_dir = os.path.join("images", str(script_id))
            os.makedirs(save_dir, exist_ok=True)
            
            # 저장할 파일명 생성 (scene_id와 UUID 사용)
            filename = f"scene_{scene_id}_{uuid.uuid4()}.png"
            save_path = os.path.join(save_dir, filename)
            
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