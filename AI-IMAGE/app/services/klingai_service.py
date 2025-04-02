"""
이미지 생성 서비스
"""
import os
import jwt
import time
import json
import requests
from fastapi import HTTPException
from typing import Tuple, Dict, Any, Optional
import glob
from datetime import datetime

from app.core.config import settings
from app.core.logger import app_logger
from app.core.api_config import klingai_config
from app.services.utils import encode_image_to_base64

class ImageService:
    """Kling AI를 사용한 이미지 생성 서비스"""
    
    @staticmethod
    def get_reference_image_path(story_id: int, scene_id: int, return_base64: bool = True) -> str:
        """참조 이미지를 반환합니다. 
        return_base64 가 True 인 경우 Base64로 인코딩된 이미지를 반환합니다.
        Args:
            story_id (int): 스토리 ID
            scene_id (int): 씬 ID
            return_base64 (bool, optional): 참조 이미지를 Base64로 인코딩 여부. Defaults to True.

        Returns:
            str: 참조 이미지 경로
        """
        reference_image_path = None
        previous_scene_id = scene_id - 1
        
        # 2. 씬 ID 가 1 이 아닌 경우 이전 씬의 이미지를 사용 
        if scene_id > 1:
            # 이전 씬의 이미지 파일 찾기
            formatted_story_id = f"{story_id:08d}"
            formatted_previous_scene_id = f"{previous_scene_id:04d}"
            
            # story_id 별 이미지 저장 경로 
            images_dir_by_story = f"images/{settings.S3_BUCKET_NAME}/{formatted_story_id}/images"
            
            # 이미지 저장 경로가 존재하는 경우
            if os.path.exists(images_dir_by_story):
                app_logger.info(f"이미지 저장 경로: {images_dir_by_story}")
                # 이전 scene_id로 시작하는 모든 이미지 파일 찾기
                # 이미지 파일 형식: {scene_id:04d}_timestamp.jpg
                previous_scene_image_pattern = f"{images_dir_by_story}/{formatted_previous_scene_id}_*.jpg"
                previous_scene_image_files = glob.glob(previous_scene_image_pattern)  # 패턴에 매칭되는 파일 모두 반환 (리스트)
                
                if previous_scene_image_files:
                    # 가장 최근 파일 (어차피 하나의 스토리의 하나의 씬에는 하나의 이미지만 존재.)
                    previous_scene_image_files.sort()
                    reference_image = previous_scene_image_files[-1]
                    app_logger.info(f"참조 이미지 찾음(이전 scene 이미지): {reference_image}")
                else:
                    # 이전 scene_id에 해당하는 이미지가 없으면 같은 스토리 내 가장 최근 이미지 사용
                    all_scene_image_pattern = f"{images_dir_by_story}/*.jpg"
                    all_scene_image_files = glob.glob(all_scene_image_pattern)
                    if all_scene_image_files:
                        # 타임스탬프 기준으로 정렬 (가장 최신 파일이 마지막에 오도록)
                        all_scene_image_files.sort()
                        reference_image = all_scene_image_files[-1]
                        app_logger.info(f"이전 scene 이미지 없음. 가장 최근 이미지로 대체: {reference_image}")
        
        # 이전 씬 이미지가 없는 경우 스타일별 참조 이미지 사용
        if not reference_image_path:
            app_logger.info(f"이전 scene 이미지 없음. 스타일별 참조 이미지 사용")
            style_reference_dict = {
                "GHIBLI": "images/references/ghibli/ghibli-reference-01.jpg",
                "ANIME": "images/references/anime/anime-reference-01.jpg",
                "DISNEY": "images/references/disney/disney-reference-01.jpg"
            }
            reference_image_path = style_reference_dict.get(style)
            app_logger.info(f"스타일 참조 이미지 경로: {reference_image_path}")
        
        
        # 참조 이미지 Base64로 인코딩 후 반환
        return encode_image_to_base64(reference_image_path) if return_base64 else reference_image_path
        
    @staticmethod
    def get_scene_data_path(story_id: int, scene_id: int) -> str:
        """씬 데이터 JSON 파일 경로를 반환합니다."""
        # 데이터 저장 디렉토리 생성
        story_base_dir = os.path.join("data", "stories")
        data_dir = os.path.join(story_base_dir, f"{story_id:08d}")
        os.makedirs(data_dir, exist_ok=True)
        
        # 씬별 JSON 파일 경로
        return os.path.join(data_dir, f"{scene_id:04d}.json")
    
    @staticmethod
    def get_previous_scene_data(story_id: int, scene_id: int) -> Optional[Dict[str, Any]]:
        """이전 씬의 정보와 이미지 프롬프트를 가져옵니다."""
        if scene_id <= 1:
            return None
            
        previous_scene_id = scene_id - 1
        previous_scene_data_path = ImageService.get_scene_data_path(story_id, previous_scene_id)
        
        if not os.path.exists(previous_scene_data_path):
            app_logger.info(f"이전 씬 데이터 파일 없음: {previous_scene_data_path}")
            return None
        
        try:
            with open(previous_scene_data_path, 'r', encoding='utf-8') as f:
                previous_scene_data = json.load(f)
            return {
                "scene_info": previous_scene_data["scene_info"],
                "image_prompt": previous_scene_data["image_prompt"]
            }
        
        except Exception as e:
            app_logger.error(f"이전 씬 데이터 조회 오류: {str(e)}")
            return None
    
    @staticmethod
    def save_scene_data(story_id: int, scene_id: int, scene_info: Dict[str, Any], image_prompt: Dict[str, Any]) -> None:
        """씬 데이터를 JSON 파일에 저장합니다."""
        file_path = ImageService.get_scene_data_path(story_id, scene_id)
        
        # 씬 데이터 구조화
        data = {
            "scene_info": scene_info,
            "image_prompt": image_prompt
        }
        
        # 파일 저장
        try:
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            app_logger.info(f"씬 데이터 저장 완료: story_id={story_id}, scene_id={scene_id}")
        except Exception as e:
            app_logger.error(f"씬 데이터 저장 오류: {str(e)}")
            
    @staticmethod
    def get_standard_http_status_code(response_code: int) -> int:
        """Kling AI API 응답 코드를 표준 HTTP 상태 코드로 변환합니다."""
        return klingai_config.KLING_TO_HTTP[response_code]
    
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
            
            ########################################### 이미지 레퍼런스 ###########################################
            reference_image_base64 = ImageService.get_reference_image_path(story_id, scene_id, return_base64=True)
            
            ############################### 이전 씬 정보 및 이미지 프롬프트 레퍼런스 ################################
            previous_scene_data = ImageService.get_previous_scene_data(story_id, scene_id)
            
            if previous_scene_data:
                # TODO 이전 씬 정보 참조 전략 추가 필요 (prompt + previous_scene_info + previous_image_prompt)
                prompt = f"{prompt}, 
                previous scene info: {previous_scene_data['scene_info']}, 
                previous image prompt: {previous_scene_data['image_prompt']}"
                
            # API 요청 데이터
            payload = {
                "model": klingai_config.MODEL_V1 if reference_image_base64 else klingai_config.MODEL_V1_5,
                "prompt": prompt, 
                "negative_prompt": negative_prompt if negative_prompt and not reference_image_base64 else "",
                "image": reference_image_base64 if reference_image_base64 else None,
                "image_fidelity": klingai_config.IMAGE_FIDELITY if reference_image_base64 else 0.5,
                "n": klingai_config.N,
                "aspect_ratio": klingai_config.ASPECT_RATIO
            }
            
            # API 요청 보내기 (이미지 생성 요청)
            response = requests.post(settings.KLING_API_URL, headers=headers, json=payload).json()

            response_code = response["code"]
            # 이미지 생성 요청 실패
            if response_code != 0:
                error_message = "KLING AI API 오류"
                try:
                    if "message" in response:
                        error_message = response["message"]
                except:
                    error_message = response.text
                
                # Kling AI API의 응답 코드를 표준 HTTP 상태 코드로 변환
                http_status_code = ImageService.get_standard_http_status_code(response_code)
                
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
                        # 씬 기본 정보 구성
                        scene_info = {
                            "story_id": story_id,
                            "scene_id": scene_id,
                            "created_at": datetime.now().isoformat(),
                            
                        }
                        
                        # 이미지 프롬프트 정보 구성
                        image_prompt = {
                            "prompt": prompt,
                            "original_prompt": prompt,
                            "negative_prompt": negative_prompt,
                            "style": style,
                        }
                        
                        # 씬 데이터를 JSON 파일에 저장
                        ImageService.save_scene_data(story_id, scene_id, scene_info, image_prompt)
                        
                        # 결과 반환
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

