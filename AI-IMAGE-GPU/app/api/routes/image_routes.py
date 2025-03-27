"""
장면 기반 이미지 생성 라우트
"""
from fastapi import APIRouter, HTTPException, BackgroundTasks
from app.schemas.models import Scene, ImageGenerationResponse
from app.services.openai_service import openai_service
from app.services.klingai_service import klingai_service
from app.services.stablediffusion_service import stablediffusion_service
from app.services.download_service import download_service
from app.services.s3_service import s3_service
import os
import time
from app.core.config import settings
from app.core.logger import app_logger

# 라우터 생성
router = APIRouter(prefix="/images", tags=["images"])

@router.post("/generations/internal/stable-diffusion", response_model=ImageGenerationResponse)
async def generate_scene_image_internal_stable_diffusion(scene: Scene):
    """
    주어진 프롬프트를 사용하여 이미지를 생성합니다.
    
    Args:
        prompt: 이미지 생성을 위한 프롬프트
    
    Returns:
        ImageGenerationResponse: 생성된 이미지의 정보(scene_id, image_prompt, image_url)
    """
    """
    장면 정보를 기반으로 이미지를 생성합니다.
    
    흐름:
    1. SpringBoot BE로부터 scene 객체 수신
    2. OpenAI API를 사용하여 이미지 프롬프트 생성
    3. Stable Diffusion w/ LoRA 사용하여 이미지 생성 및 로컬에 저장 
    4. 이미지를 S3에 업로드
    5. 결과 반환
    """
    start_time = time.time()
    app_logger.info("이미지 생성 함수 시작")
    try:
        # 1. 장면 정보 검증
        if not scene.scene_id:
            raise HTTPException(status_code=400, detail="장면 ID가 누락되었습니다.")
        if not scene.story_metadata:
            raise HTTPException(status_code=400, detail="스토리 메타데이터가 누락되었습니다.")
        if not scene.story_metadata.story_id:
            raise HTTPException(status_code=400, detail="스토리 ID가 누락되었습니다.")
        if not scene.audios or len(scene.audios) == 0:
            raise HTTPException(status_code=400, detail="장면 오디오 정보가 누락되었습니다.")
        
        # 필요한 ID 추출
        story_id = scene.story_metadata.story_id
        scene_id = scene.scene_id
        
        # 2. OpenAI를 사용하여 이미지 프롬프트 생성
        image_prompt_start_time = time.time()
        app_logger.info(f"스토리 {story_id}, 장면 {scene_id}에 대한 이미지 프롬프트 생성 중...")

        stablediffusion_response = await openai_service.generate_image_prompt_stable_diffusion(scene)  # stable diffusion 전용 이미지 프롬프트 생성 함수 

        app_logger.info(f"생성된 이미지 프롬프트: {stablediffusion_response.prompt}")
        image_prompt_end_time = time.time()
        image_prompt_time = image_prompt_end_time - image_prompt_start_time
        app_logger.info(f"이미지 프롬프트 생성 시간: {image_prompt_time}초")
        
        # TODO: Stable Diffusion w/ LoRA 사용하여 이미지 생성 및 로컬에 저장 
        local_image_path = await stablediffusion_service.generate_image(
            story_id=story_id,
            scene_id=scene_id,
            bucket_name=s3_service.s3_bucket,
            prompt=stablediffusion_response.prompt,
            negative_prompt=stablediffusion_response.negative_prompt,
            sampler=stablediffusion_response.sampler,
            cfg_scale=stablediffusion_response.cfg_scale,
            steps=stablediffusion_response.steps,
            clip_skip=stablediffusion_response.clip_skip
        )
        
        # 4. 이미지를 S3에 업로드
        image_upload_start_time = time.time()
        app_logger.info(f"이미지 S3 업로드 중...")
        s3_url = await s3_service.upload_image(local_image_path, story_id, scene_id)
        image_upload_end_time = time.time()
        image_upload_time = image_upload_end_time - image_upload_start_time
        app_logger.info(f"이미지 S3 업로드 시간: {image_upload_time}초")
        
        # S3 자격 증명이 없거나 업로드에 실패한 경우, 로컬 URL 사용
        if not s3_url:
            app_logger.info("S3 업로드 실패, 로컬 URL 사용")
            filename = os.path.basename(local_image_path)
            formatted_story_id = f"{story_id:08d}"  # 8자리 (예: 00000001)
            
            # 버킷 이름 정제
            # s3_bucket_name = settings.S3_BUCKET_NAME # 이전 경로로
            s3_bucket_name = s3_service.s3_bucket
            if s3_bucket_name and s3_bucket_name.startswith("s3://"):
                s3_bucket_name = s3_bucket_name.replace("s3://", "")
            if s3_bucket_name and s3_bucket_name.endswith("/"):
                s3_bucket_name = s3_bucket_name.rstrip("/")
                
            s3_url = f"/AI-IMAGE/{s3_bucket_name}/{formatted_story_id}/images/{filename}"
        
        # 5. 결과 반환
        end_time = time.time()
        total_time = end_time - start_time
        app_logger.info(f"총 처리 시간: {total_time}초")
        return ImageGenerationResponse(
            scene_id=scene_id,
            image_prompt=stablediffusion_response.prompt,
            image_url=s3_url
        )
    
        
    except HTTPException:
        raise
    except Exception as e:
        app_logger.error(f"이미지 생성 중 오류 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=f"이미지 생성 중 오류 발생: {str(e)}") 
    
    