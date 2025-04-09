"""
장면 기반 이미지 생성 라우트
"""

import time
import random
from fastapi import APIRouter, HTTPException
from app.schemas.models import Scene, ImageGenerationResponse
from app.services.openai_service import openai_service
from app.services.klingai_service import klingai_service
from app.services.download_service import download_service
from app.services.s3_service import s3_service
from app.services.utils import check_scene
from app.core.logger import app_logger
from app.core.config import settings

# 라우터 생성
router = APIRouter(prefix="/images", tags=["images"])


@router.post("/generations/external", response_model=ImageGenerationResponse)
async def generate_scene_image(scene: Scene, style: str = 'pixar'):
    """
    장면 정보를 기반으로 이미지를 생성합니다.

    흐름:
    1. SpringBoot BE로부터 scene 객체 수신
    2. OpenAI(gpt-4o)를 사용하여 이미지 프롬프트 생성
    3. KLING AI를 사용하여 이미지 생성
    4. 생성된 이미지를 로컬에 저장
    5. 이미지를 S3에 업로드
    6. 결과 반환
    
    Args:
        scene (Scene): 장면 정보
        style (str): 이미지 생성 스타일 (기본값: disney)
            - disney: 디즈니 애니메이션 스튜디오 스타일
            - pixar: 픽사 3D 스타일
            - illustrate: 일러스트레이트 스타일
    """

    start_time = time.time()
    app_logger.info("Received request for image generation...")

    app_logger.info(
        f"Story ID: {scene.story_metadata.story_id}, Scene ID: {scene.scene_id}, Style: {style}"
    )
    app_logger.info(f"Scene: \n{scene.model_dump_json(indent=4)}")

    try:
        # 1. 장면 정보 검증 - 비즈니스 로직 유효성 검사
        check_scene_result = await check_scene(scene)
        if check_scene_result["result"] == False:
            raise HTTPException(
                status_code=400, detail=check_scene_result["validation_errors"]
            )

        # 2. 이미지 프롬프트 생성
        app_logger.info("Generating image prompt...")

        all_prompts = await openai_service.generate_image_prompt(scene, style)

        image_prompt = all_prompts["image_prompt"]
        negative_prompt = all_prompts["negative_prompt"]
        scene_info = all_prompts["scene_info"]

        app_logger.info(f"Successfully generated image prompt")

        app_logger.debug(f"Image Prompt: \n{image_prompt}")
        app_logger.debug(f"Negative Prompt: \n{negative_prompt}")
        app_logger.debug(f"Scene Info: \n{scene_info}")

        # 3. KLING AI를 사용하여 이미지 생성
        app_logger.info("Generating image...")
        image_data = await klingai_service.generate_image(
            story_id=scene.story_metadata.story_id,
            scene_id=scene.scene_id,
            prompt=image_prompt,
            negative_prompt=negative_prompt,
            scene_info=scene_info,
            style=style,
        )

        if not image_data or "image_url" not in image_data:
            raise HTTPException(status_code=500, detail="Failed to generate image")

        image_url = image_data["image_url"]
        app_logger.info(f"Successfully generated image: \n{image_url}")

        # 4. 생성된 이미지를 로컬에 저장
        app_logger.info("Downloading image...")
        local_image_path = await download_service.download_image(
            image_url, scene.story_metadata.story_id, scene.scene_id
        )
        if local_image_path:
            app_logger.info(f"Successfully downloaded image: \n{local_image_path}")
        else:
            app_logger.error(f"Failed to download image: \n{image_url}")
            raise HTTPException(status_code=500, detail="Failed to download image")

        # 5. 이미지를 S3에 업로드
        app_logger.info("Uploading image to S3...")
        s3_result = await s3_service.upload_image(
            local_image_path, scene.story_metadata.story_id, scene.scene_id
        )

        # S3 업로드 결과 처리
        if s3_result["success"]:
            s3_url = s3_result["url"]
            app_logger.info(f"Successfully uploaded image to S3: \n{s3_url}")
        else:
            app_logger.error(f"Failed to upload image to S3: \n{s3_result}")

            # 업로드 실패 처리
            error_msg = s3_result["error"]
            error_type = s3_result["error_type"]
            local_path = s3_result["local_path"]

            # 환경 설정에 따라 로컬 URL 사용 또는 에러 발생
            if settings.USE_LOCAL_URL_ON_S3_FAILURE:
                app_logger.warning(
                    f"Failed to upload image to S3 ({error_type}): {error_msg}. Using local URL instead."
                )

                if local_path.startswith("images/"):
                    relative_path = local_path[7:]  # 'images/' 부분 제거
                    s3_url = f"/static/images/{relative_path}"
                else:
                    # 경로에 'images/'가 없으면 그대로 사용
                    s3_url = f"/static/images/{local_path}"
                app_logger.info(f"Created local URL: {s3_url}")
            else:
                app_logger.error(f"Failed to upload image to S3: \n{s3_result}")
                # 오류 유형에 따른 HTTP 상태 코드 매핑
                status_code = 500
                if error_type == "file_not_found":
                    status_code = 404
                elif (
                    error_type == "invalid_credentials"
                    or error_type == "credentials_missing"
                ):
                    status_code = 401

                # 오류 반환
                raise HTTPException(
                    status_code=status_code,
                    detail=f"Failed to upload image to S3: {error_msg} (error type: {error_type})",
                )

        # 6. 결과 반환
        end_time = time.time()
        total_time = end_time - start_time
        app_logger.info(f"Total processing time: {total_time} seconds")
        return ImageGenerationResponse(
            scene_id=scene.scene_id,
            image_prompt=image_prompt,
            image_url=s3_url,
        )

    except HTTPException:
        raise
    except Exception as e:
        app_logger.error(f"Error occurred while generating image: {str(e)}")
        raise HTTPException(
            status_code=500, detail=f"Error occurred while generating image: {str(e)}"
        )
