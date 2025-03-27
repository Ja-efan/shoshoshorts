import os 
import time 
from fastapi import APIRouter, HTTPException
from app.schemas.models import Scene
from app.services.openai_service import openai_service
from app.services.stablediffusion_service import stablediffusion_service
from app.services.s3_service import s3_service
from app.core.config import settings
from app.core.logger import app_logger

# 테스트 라우터 생성 
router = APIRouter(prefix="/tests", tags=["tests"])

@router.post("/image-prompt/stable-diffusion")
async def test_generate_image_prompt_stable_diffusion(scene: Scene):
    """
    Stable Diffusion 이미지 프롬프트 생성 테스트 
    """
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
        app_logger.info(f"[Test] 스토리 {story_id}, 장면 {scene_id}에 대한 이미지 프롬프트 생성 중...")
        
        image_prompt = await openai_service.generate_image_prompt_stable_diffusion(scene)
        app_logger.info(f"[Test] Stable Diffusion 이미지 프롬프트: {image_prompt}")
        image_prompt_end_time = time.time()
        image_prompt_time = image_prompt_end_time - image_prompt_start_time
        app_logger.info(f"[Test] Stable Diffusion 이미지 프롬프트 생성 시간: {image_prompt_time}초")
        
        return {
            "image_prompt": image_prompt,
            "image_prompt_time": image_prompt_time
        }
        
    except Exception as e:
        app_logger.error(f"[Test] Stable Diffusion 이미지 프롬프트 생성 중 오류 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Stable Diffusion 이미지 프롬프트 생성 중 오류 발생: {str(e)}")
    
@router.post("/image/stable-diffusion")
async def test_generate_image_stable_diffusion(scene: Scene, style: str="ghibli"):
    """
    Stable Diffusion 이미지 생성 테스트 
    """
    start_time = time.time()
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
        
        # 2. Stable Diffusion 이미지 프롬프트 생성 
        try:
            image_prompt_start_time = time.time()
            app_logger.info(f"[Test] 스토리 {story_id}, 장면 {scene_id}에 대한 이미지 프롬프트 생성 중...")
            
            image_prompt = await openai_service.generate_image_prompt_stable_diffusion(scene)
            
            # app_logger.debug(f"[Test] Stable Diffusion 이미지 프롬프트: {image_prompt}")
            image_prompt_end_time = time.time()
            image_prompt_time = image_prompt_end_time - image_prompt_start_time
            app_logger.info(f"[Test] Stable Diffusion 이미지 프롬프트 생성 시간: {image_prompt_time}초")
            
        except Exception as e:
            app_logger.error(f"[Test] Stable Diffusion 이미지 프롬프트트 생성 중 오류 발생: {str(e)}")
            
        # 3. Stable Diffusion 이미지 생성 
        try:
            image_generation_start_time = time.time()
            app_logger.info(f"[Test] 스토리 {story_id}, 장면 {scene_id}에 대한 이미지 생성 중...")
            
            image_path = stablediffusion_service.generate_image(
                story_id=story_id,
                scene_id=scene_id,
                bucket_name=s3_service.s3_bucket,
                style=style,
                prompt=image_prompt.prompt,
                negative_prompt=image_prompt.negative_prompt,
                sampler=image_prompt.sampler,
                cfg_scale=image_prompt.cfg_scale,
                steps=image_prompt.steps,
                clip_skip=image_prompt.clip_skip
            )
            app_logger.info(f"[Test] Stable Diffusion 이미지 생성 완료!")
            app_logger.info(f"[Test] Stable Diffusion 이미지 생성 경로: {image_path}")
            image_generation_end_time = time.time()
            image_generation_time = image_generation_end_time - image_generation_start_time
            app_logger.info(f"[Test] Stable Diffusion 이미지 생성 시간: {image_generation_time}초")
            
        except Exception as e:
            app_logger.error(f"[Test] Stable Diffusion 이미지 생성 중 오류 발생: {str(e)}")
            raise HTTPException(status_code=500, detail=f"Stable Diffusion 이미지 생성 중 오류 발생: {str(e)}")
        
        return {
            "image_path": image_path,
            "image_generation_time": image_generation_time
        }
        
    except Exception as e:
        app_logger.error(f"[Test] Stable Diffusion 이미지 생성 중 오류 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Stable Diffusion 이미지 생성 중 오류 발생: {str(e)}")
    
    finally:
        end_time = time.time()
        app_logger.info(f"[Test] 총 시간: {end_time - start_time}초")