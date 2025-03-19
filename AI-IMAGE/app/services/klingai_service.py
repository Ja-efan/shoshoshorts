"""
이미지 생성 서비스
"""
import requests
import time
from fastapi import HTTPException
from typing import Dict, Any, Optional
import jwt

from app.core.config import settings

class ImageService:
    """Kling AI를 사용한 이미지 생성 서비스"""
    
    @staticmethod
    async def generate_image(
        prompt: str,
        negative_prompt: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Kling AI API를 사용하여 이미지를 생성합니다.
        
        Args:
            prompt: 이미지 생성을 위한 텍스트 프롬프트
            negative_prompt: 이미지에 포함하지 않을 요소를 지정하는 텍스트
            width: 이미지 너비
            height: 이미지 높이
            
        Returns:
            생성된 이미지 URL이 포함된 딕셔너리
            
        Raises:
            HTTPException: API 호출 중 오류가 발생한 경우
        """
        try:
            # API 키 확인
            if not settings.KLING_ACCESS_KEY or not settings.KLING_SECRET_KEY:
                raise HTTPException(status_code=500, detail="Kling AI API 키가 설정되지 않았습니다.")
            
            # JWT 토큰이 만료되었는지 확인하고 필요하면 갱신
            try:
                jwt.decode(settings.JWT_TOKEN, settings.KLING_SECRET_KEY, algorithms=["HS256"])
            except jwt.ExpiredSignatureError:
                # 토큰이 만료되었으면 새로 생성
                from app.core.config import encode_jwt_token
                settings.JWT_TOKEN = encode_jwt_token(settings.KLING_ACCESS_KEY, settings.KLING_SECRET_KEY)
            except Exception:
                # 다른 오류가 발생하면 토큰 새로 생성
                from app.core.config import encode_jwt_token
                settings.JWT_TOKEN = encode_jwt_token(settings.KLING_ACCESS_KEY, settings.KLING_SECRET_KEY)
            
            # API 요청 헤더
            headers = {
                "Content-Type": "application/json",
                "Authorization": f"Bearer {settings.JWT_TOKEN}"
            }
            
            # API 요청 데이터
            payload = {
                "model": "kling-v1",
                "prompt": prompt,
                "negative_prompt": negative_prompt if negative_prompt else "",
                "n": 1,
                "aspect_ratio": "1:1"
            }
            
            # API 요청 보내기 (이미지 생성 요청)
            response = requests.post(settings.KLING_API_URL, headers=headers, json=payload).json()
            
            # 응답 코드 
            # https://docs.qingque.cn/d/home/eZQCQxBrX8eeImjK6Ddz5iOi5?identityId=1oEG9JKKMFv#section=h.kfrlcz6g2rlh
            response_code = response["code"]

            # 이미지 생성 요청 실패
            if response_code != 0:
                error_message = "API 오류"
                try:
                    if "message" in response:
                        error_message = response["message"]
                except:
                    error_message = response.text
                
                raise HTTPException(
                    status_code=response_code,  
                    detail=f"Kling AI API 오류: {error_message}"
                )
            
            # 응답 데이터
            try:
                response_data = response["data"]
                if response_code == 0 and response_data:
                    task_id = response_data["task_id"]
                    image_urls = await ImageService.get_task_result(task_id)

                    if image_urls and len(image_urls) > 0:
                        return {
                            "image_url": image_urls[0]["url"],
                            "prompt": prompt
                        }
                    else:
                        raise HTTPException(status_code=500, detail="이미지 생성 시간 초과")
            except:
                raise HTTPException(status_code=500, detail="응답 데이터 파싱 오류")
            
        except HTTPException:
            raise
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"이미지 생성 중 오류 발생: {str(e)}")
    
    @staticmethod
    async def get_task_result(task_id: str, max_attempts: int = 10, delay: int = 3) -> Optional[list]:
        """
        이미지 생성 태스크의 결과를 가져옵니다.
        
        Args:
            task_id: 태스크 ID
            max_attempts: 최대 시도 횟수
            delay: 각 시도 사이의 지연 시간 (초)
            
        Returns:
            생성된 이미지 URL 목록 또는 None (실패 시)
        """
        url = settings.KLING_API_URL
        headers = {"Authorization": f"Bearer {settings.JWT_TOKEN}"}
        params = {"pageSize": 500}
        
        for _ in range(max_attempts):
            try:
                response = requests.get(url, headers=headers, params=params).json()
                response_code = response["code"]
                if response_code == 0 and "data" in response:
                    response_data = response["data"]
                    for task in response_data:
                        if task["task_id"] == task_id and task["task_status"] == "succeed":
                            return task["task_result"]["images"]  # [{"index": int, "url": string}]
                
                # 아직 완료되지 않은 경우 대기
                time.sleep(delay)
                
            except Exception as e:
                print(f"이미지 생성 task 결과 가져오기 실패: {str(e)}")
                time.sleep(delay)
        
        return None

# 서비스 인스턴스 생성
image_service = ImageService()

