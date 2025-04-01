"""
이미지 생성 서비스
"""
import os
import jwt
import time
import requests
from fastapi import HTTPException
from typing import Dict, Any, Optional
import glob

from app.core.config import settings
from app.core.logger import app_logger
from app.core.api_config import klingai_config
from app.services.utils import encode_image_to_base64

class ImageService:
    """Kling AI를 사용한 이미지 생성 서비스"""
    
    @staticmethod
    async def generate_image(
        story_id: int,
        scene_id: int,
        prompt: str,
        negative_prompt: Optional[str] = None,
        style: str ="GHIBLI",
    ) -> Dict[str, Any]:
        """
        Kling AI API를 사용하여 이미지를 생성합니다.
        
        Args:
            prompt: 이미지 생성을 위한 텍스트 프롬프트
            negative_prompt: 이미지에 포함하지 않을 요소를 지정하는 텍스트
            
        Returns:
            생성된 이미지 URL이 포함된 딕셔너리
            
        Raises:
            HTTPException: API 호출 중 오류가 발생한 경우
        """
        try:
            # API 키 확인
            if not settings.KLING_ACCESS_KEY or not settings.KLING_SECRET_KEY:
                app_logger.error("Kling AI API 키가 설정되지 않았습니다.")
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
            
            # instruction="""
            # {{{A cheerful character in the style of a classic Disney animated movie, with large expressive eyes, soft lighting, vibrant colors, smooth outlines, and a magical fairytale background, highly detailed and whimsical, 2D animation style}}}
            # """
            
            ########################################### 이미지 참조 (동일 story_id 내 이미지 참조) ###########################################  
            # # 현재 scene의 스토리 id에 대한 이전 이미지가 있는 경우 추가 
            # reference_image = None
            # previous_scene_id = scene_id - 1
            # formatted_story_id = f"{story_id:08d}"
            # formatted_scene_id = f"{scene_id:04d}"
            # formatted_previous_scene_id = f"{previous_scene_id:04d}"
            
            # # 이미지 파일이 {scene_id}_{timestamp}.png 형식으로 저장되어 있는지 확인
            # # glob을 사용하여 와일드카드 패턴으로 검색
            # image_path = f"images/{settings.S3_BUCKET_NAME}/{formatted_story_id}/images"
            # if os.path.exists(image_path):
            #     # 이전 scene_id로 시작하는 모든 이미지 파일 찾기
            #     pattern = f"{image_path}/{formatted_previous_scene_id}_*.jpg"
            #     matching_files = glob.glob(pattern)
                
            #     if matching_files:
            #         # 가장 최근 파일 (알파벳 순으로 마지막 파일이 일반적으로 최신 타임스탬프)
            #         reference_image = matching_files[-1]
            #         app_logger.info(f"참조 이미지 찾음: {reference_image}")
            #     else:
            #         # 이전 scene_id에 해당하는 이미지가 없으면 디렉토리 내 모든 이미지 중 가장 최근 것 사용
            #         all_images_pattern = f"{image_path}/*.jpg"
            #         all_images = glob.glob(all_images_pattern)
            #         if all_images:
            #             # 타임스탬프 기준으로 정렬 (가장 최신 파일이 마지막에 오도록)
            #             all_images.sort()
            #             reference_image = all_images[-1]
            #             app_logger.info(f"이전 scene 이미지 없음. 가장 최근 이미지로 대체: {reference_image}")
            
            # else: 
            #     app_logger.info(f"이전 scene 이미지 없음. 참조 이미지 없음")
                
            ########################################### 이미지 참조 (동일 style 내 이미지 참조) ###########################################  
            reference_image = None 
            style_reference_dict = {
                "GHIBLI": "images/references/ghibli/ghibli-reference-01.jpg",
                "ANIME": "images/references/anime/anime-reference-01.jpg",
                "DISNEY": "images/references/disney/disney-reference-01.jpg"
            }
            reference_image = style_reference_dict[style]
            app_logger.info(f"참조 이미지: {reference_image}")
            
            # 참조 이미지가 있는 경우 Base64로 인코딩
            reference_image_base64 = None
            if reference_image:
                reference_image_base64 = encode_image_to_base64(reference_image)

            # API 요청 데이터
            payload = {
                "model": klingai_config.MODEL_V1 if reference_image else klingai_config.MODEL_V1_5,
                "prompt": prompt,
                "negative_prompt": negative_prompt if negative_prompt else "",
                # TODO: "image" 추가 (Reference Image -> Base64 인코딩 or image URL)
                # image 추가 하는 경우 -> 현재 scene의 스토리 id에 대한 이전 이미지가 있는 경우 추가 
                # image를 추가하는 경우에 negative_prompt는 무시됨 
                "image": reference_image_base64 if reference_image_base64 else None,
                "image_fidelity": klingai_config.IMAGE_FIDELITY if reference_image else 0.5,
                "n": klingai_config.N,
                "aspect_ratio": klingai_config.ASPECT_RATIO
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
                
                # Kling API의 응답 코드를 표준 HTTP 상태 코드로 변환
                http_status_code = 500  # 기본적으로 서버 오류로 설정
                
                # Kling AI 응답 코드에 따른 HTTP 상태 코드 매핑
                if response_code in klingai_config.KLING_TO_HTTP:
                    http_status_code = klingai_config.KLING_TO_HTTP[response_code]
                
                raise HTTPException(
                    status_code=http_status_code,  
                    detail=f"Kling AI API 오류 (코드: {response_code}): {error_message}"
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
                        app_logger.error(f"이미지 생성 실패: KLINAI image url 없음")
                        raise HTTPException(status_code=500, detail=f"이미지 생성 실패: KLINAI image url 없음")
            except:
                raise HTTPException(status_code=500, detail="응답 데이터 파싱 오류")
            
        except HTTPException:
            raise
        except Exception as e:
            app_logger.error(f"이미지 생성 중 오류 발생: {str(e)}")
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
                app_logger.error(f"이미지 생성 task 결과 가져오기 실패: {str(e)}")
                time.sleep(delay)
        
        return None

# 서비스 인스턴스 생성
image_service = ImageService()

