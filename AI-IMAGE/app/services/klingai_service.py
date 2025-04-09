"""
이미지 생성 서비스
"""

import os
from xxlimited import Str
import jwt
import time
import json
import glob
import requests
from fastapi import HTTPException
from typing import Dict, Any, Optional

from app.core.config import settings
from app.core.logger import app_logger
from app.core.api_config import klingai_config
from app.core.storage_config import s3_config
from app.schemas.models import SceneInfo, PreviousSceneData
from app.services.utils import encode_image_to_base64


class KlingAIService:
    """Kling AI를 사용한 이미지 생성 서비스"""

    @staticmethod
    def get_reference_image_base64(story_id: int, scene_id: int, style: str) -> str:
        """참조 이미지를 Base64로 인코딩하여 반환합니다.
        참조 이미지는 story_id, scene_id, style 에 따라 결정됩니다.
            1. 이전 씬의 이미지가 존재하는 경우 이전 씬의 이미지를 사용
            2. 이전 씬의 이미지가 존재하지 않는 경우 스타일별 참조 이미지를 사용

        Args:
            story_id (int): 스토리 ID
            scene_id (int): 씬 ID
            style (str): 이미지 스타일

        Returns:
            str: Base64로 인코딩된 이미지
        """
        reference_image_path = None
        previous_scene_id = scene_id - 1

        # 2. 씬 ID 가 1 이 아닌 경우 이전 씬의 이미지를 사용
        if scene_id > 1:
            # 이전 씬의 이미지 파일 찾기
            formatted_story_id = f"{story_id:08d}"
            formatted_previous_scene_id = f"{previous_scene_id:04d}"

            # story_id 별 이미지 저장 경로
            images_dir_by_story = (
                f"images/{s3_config.BUCKET_NAME}/{formatted_story_id}/images"
            )

            # 이미지 저장 경로가 존재하는 경우
            if os.path.exists(images_dir_by_story):
                app_logger.debug(f"Image save path: {images_dir_by_story}")
                # 이전 scene_id로 시작하는 모든 이미지 파일 찾기
                # 이미지 파일 형식: {scene_id:04d}_timestamp.jpg
                previous_scene_image_pattern = (
                    f"{images_dir_by_story}/{formatted_previous_scene_id}_*.jpg"
                )
                previous_scene_image_files = glob.glob(
                    previous_scene_image_pattern
                )  # 패턴에 매칭되는 파일 모두 반환 (리스트)

                if previous_scene_image_files:
                    # 가장 최근 파일 (어차피 하나의 스토리의 하나의 씬에는 하나의 이미지만 존재.)
                    previous_scene_image_files.sort()
                    reference_image_path = previous_scene_image_files[-1]
                    app_logger.info(
                        f"Reference image found (previous scene image): {reference_image_path}"
                    )
                else:
                    # 이전 scene_id에 해당하는 이미지가 없으면 같은 스토리 내 가장 최근 이미지 사용
                    all_scene_image_pattern = f"{images_dir_by_story}/*.jpg"
                    all_scene_image_files = glob.glob(all_scene_image_pattern)
                    if all_scene_image_files:
                        # 타임스탬프 기준으로 정렬 (가장 최신 파일이 마지막에 오도록)
                        all_scene_image_files.sort()
                        reference_image_path = all_scene_image_files[-1]
                        app_logger.info(
                            f"No previous scene image. Using the latest image: {reference_image_path}"
                        )

        # 이전 씬 이미지가 없는 경우 스타일별 참조 이미지 사용
        if not reference_image_path:
            app_logger.info(f"No previous scene image. Using style reference image.")
            style_reference_dict = {
                "GHIBLI": "images/references/ghibli/ghibli-reference-01.jpg",
                "ANIME": "images/references/anime/anime-reference-01.jpg",
                "DISNEY": "images/references/disney/disney-reference-01.jpg",
                "DISNEY-PIXAR": "images/references/disney/disney-reference-01.jpg",
            }
            reference_image_path = style_reference_dict.get(style)
            app_logger.info(f"Style reference image path: {reference_image_path}")

            # 참조 이미지가 존재하지 않는 경우 기본 이미지 사용
            if not reference_image_path or not os.path.exists(reference_image_path):
                app_logger.warning(
                    f"{style} style reference image does not exist. Using default reference image."
                )
                # 지브리 이미지를 기본 참조 이미지로 사용
                reference_image_path = (
                    "images/references/ghibli/ghibli-reference-01.jpg"
                )
                app_logger.info(f"Default reference image path: {reference_image_path}")

        # 참조 이미지 Base64로 인코딩 후 반환
        encoded_image = encode_image_to_base64(reference_image_path)
        if encoded_image is None:
            app_logger.warning(
                f"Failed to encode reference image: {reference_image_path}. Proceeding without reference image."
            )
        return encoded_image

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
    def save_scene_data(
        story_id: int,
        scene_id: int,
        scene_info: SceneInfo,
        image_prompt: Dict[str, Any],
    ) -> None:
        """씬 데이터를 JSON 파일에 저장합니다."""

        # 파일 저장
        try:
            file_path = KlingAIService.get_scene_data_path(story_id, scene_id)
            # 씬 데이터 구조화 - 문자열 대신 딕셔너리로 저장
            data = {
                "scene_info": scene_info.model_dump(),  # 객체를 딕셔너리로 직렬화
                "image_prompt": image_prompt
            }

            with open(file_path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            app_logger.info(
                f"Saved scene data: story_id={story_id}, scene_id={scene_id}"
            )
        except Exception as e:
            app_logger.error(f"Scene data save error: {str(e)}")

    @staticmethod
    def get_standard_http_status_code(response_code: int) -> int:
        """Kling AI API 응답 코드를 표준 HTTP 상태 코드로 변환합니다."""
        return klingai_config.KLING_TO_HTTP[response_code]

    @staticmethod
    def legacy_scene_data_migration(story_id: int, scene_id: int) -> bool:
        """
        기존 문자열 형태로 저장된 scene_info를 새로운 형식(딕셔너리)으로 마이그레이션합니다.
        
        Args:
            story_id: 스토리 ID
            scene_id: 씬 ID
            
        Returns:
            bool: 마이그레이션 성공 여부
        """
        try:
            # 씬 데이터 파일 경로
            file_path = KlingAIService.get_scene_data_path(story_id, scene_id)
            
            if not os.path.exists(file_path):
                app_logger.info(f"Scene data file not found: {file_path}")
                return False
                
            # 파일 읽기
            with open(file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                
            # 이미 딕셔너리 형태인 경우 마이그레이션 불필요
            if isinstance(data.get("scene_info"), dict):
                app_logger.info(f"Scene data already in new format: {file_path}")
                return True
                
            # 문자열 형태인 경우 파싱하여 딕셔너리로 변환
            if isinstance(data.get("scene_info"), str):
                try:
                    scene_info_dict = json.loads(data["scene_info"])
                    data["scene_info"] = scene_info_dict
                    
                    # 이미지 프롬프트에 original_prompt 필드가 없는 경우 추가
                    if "image_prompt" in data and isinstance(data["image_prompt"], dict):
                        if "original_prompt" not in data["image_prompt"] and "prompt" in data["image_prompt"]:
                            data["image_prompt"]["original_prompt"] = data["image_prompt"]["prompt"]
                    
                    # 업데이트된 데이터 저장
                    with open(file_path, "w", encoding="utf-8") as f:
                        json.dump(data, f, ensure_ascii=False, indent=2)
                        
                    app_logger.info(f"Successfully migrated scene data: {file_path}")
                    return True
                    
                except json.JSONDecodeError as je:
                    app_logger.error(f"Failed to parse scene_info string: {str(je)}")
                    return False
                    
            return False
            
        except Exception as e:
            app_logger.error(f"Scene data migration error: {str(e)}")
            return False

    @staticmethod
    async def generate_image(
        story_id: int,
        scene_id: int,
        prompt: str,
        negative_prompt: Optional[str] = None,
        scene_info: Any = None,
        style: str = "DISNEY-PIXAR",
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
        original_prompt = prompt
        try:
            # API 키 확인
            if not klingai_config.ACCESS_KEY or not klingai_config.SECRET_KEY:
                app_logger.error("Kling AI API key is not set.")
                raise HTTPException(
                    status_code=500, detail="Kling AI API key is not set."
                )

            # JWT 토큰이 만료되었는지 확인하고 필요하면 갱신
            try:
                jwt.decode(
                    klingai_config.JWT_TOKEN,
                    klingai_config.SECRET_KEY,
                    algorithms=["HS256"],
                )
            except jwt.ExpiredSignatureError:
                # 토큰이 만료되었으면 새로 생성
                from app.core.api_config import encode_jwt_token

                klingai_config.JWT_TOKEN = encode_jwt_token(
                    klingai_config.ACCESS_KEY, klingai_config.SECRET_KEY
                )
            except Exception:
                # 다른 오류가 발생하면 토큰 새로 생성
                from app.core.api_config import encode_jwt_token

                klingai_config.JWT_TOKEN = encode_jwt_token(
                    klingai_config.ACCESS_KEY, klingai_config.SECRET_KEY
                )

            # API 요청 헤더
            headers = {
                "Content-Type": "application/json",
                "Authorization": f"Bearer {klingai_config.JWT_TOKEN}",
            }

            # API 요청 데이터
            payload = {
                "model": klingai_config.MODEL_V1_5,
                "prompt": prompt,
                "negative_prompt": (
                    negative_prompt
                    if negative_prompt and not reference_image_base64
                    else ""
                ),
                "n": klingai_config.NUM_OF_IMAGES,
                "aspect_ratio": klingai_config.ASPECT_RATIO,
            }

            ####################################### 이미지 레퍼런스 : 현재 사용 x #################################
            reference_image_base64 = None
            if klingai_config.USE_REFERENCE_IMAGE:  # default: False
                reference_image_base64 = KlingAIService.get_reference_image_base64(
                    story_id, scene_id, style
                )
                payload["image"] = reference_image_base64
                payload["image_fidelity"] = klingai_config.IMAGE_FIDELITY

            ######################################################################################################

            # API 요청 보내기 (이미지 생성 요청)
            response = requests.post(
                klingai_config.API_URL, headers=headers, json=payload
            ).json()

            response_code = response["code"]

            # 이미지 생성 요청 실패
            if response_code != 0:
                error_message = "KLING AI API error"
                try:
                    if "message" in response:
                        error_message = response["message"]
                except:
                    error_message = response.text

                # Kling AI API의 응답 코드를 표준 HTTP 상태 코드로 변환
                http_status_code = KlingAIService.get_standard_http_status_code(
                    response_code
                )

                raise HTTPException(
                    status_code=http_status_code,
                    detail=f"KLING AI API error (code: {response_code}): {error_message}",
                )

            # 응답 데이터
            app_logger.debug(
                f"Response from KLING AI: \n{json.dumps(response, ensure_ascii=False, indent=2)}"
            )
            try:
                response_data = response["data"]
                if response_code == 0 and response_data:
                    task_id = response_data["task_id"]
                    image_urls = await KlingAIService.get_task_result(task_id)

                    if image_urls and len(image_urls) > 0:

                        # 이미지 프롬프트 정보 구성
                        image_prompt_to_save = {
                            "prompt": prompt,
                            "negative_prompt": negative_prompt,
                            "style": style,
                            "original_prompt": original_prompt
                        }

                        # 씬 데이터를 JSON 파일에 저장
                        KlingAIService.save_scene_data(
                            story_id, scene_id, scene_info, image_prompt_to_save
                        )

                        # 결과 반환
                        return {"image_url": image_urls[0]["url"], "prompt": prompt}
                    else:
                        app_logger.error(
                            f"Failed to generate image: KLING AI image url is empty"
                        )
                        raise HTTPException(
                            status_code=500,
                            detail=f"Failed to generate image: KLING AI image url is empty",
                        )
            except:
                raise HTTPException(status_code=500, detail="응답 데이터 파싱 오류")

        except HTTPException:
            raise
        except Exception as e:
            app_logger.error(f"Error occurred while generating image: {str(e)}")
            raise HTTPException(
                status_code=500,
                detail=f"Error occurred while generating image: {str(e)}",
            )

    @staticmethod
    async def get_task_result(task_id: str) -> Optional[list]:
        """
        이미지 생성 태스크의 결과를 가져옵니다.

        Args:
            task_id: 태스크 ID
            max_attempts: 최대 시도 횟수
            delay: 각 시도 사이의 지연 시간 (초)

        Returns:
            생성된 이미지 URL 목록 또는 None (실패 시)
        """
        url = f"{klingai_config.TASK_URL}/{task_id}"
        headers = {"Authorization": f"Bearer {klingai_config.JWT_TOKEN}"}
        params = {"pageSize": 500}

        for _ in range(klingai_config.MAX_ATTEMPTS):
            try:
                response = requests.get(url, headers=headers, params=params).json()
                response_code = response["code"]
                if response_code == 0 and "data" in response:
                    task_data = response["data"]
                    if task_data["task_status"] == "succeed":
                        return task_data["task_result"][
                            "images"
                        ]  # [{"index": int, "url": string}]
                    elif task_data["task_status"] == "failed":
                        app_logger.error(
                            f"Task failed: {task_data.get('error_msg', 'Unknown error')}"
                        )
                        return None

                # 아직 완료되지 않은 경우 대기
                app_logger.info(f"Task in progress... {_+1}/{klingai_config.MAX_ATTEMPTS} attempts")
                time.sleep(klingai_config.DELAY)

            except Exception as e:
                app_logger.error(f"Failed to get task result: {str(e)}")
                time.sleep(klingai_config.DELAY)

        app_logger.error(
            f"Maximum number of attempts ({klingai_config.MAX_ATTEMPTS}) exceeded. Task ID: {task_id}"
        )
        return None


# 서비스 인스턴스 생성
klingai_service = KlingAIService()
