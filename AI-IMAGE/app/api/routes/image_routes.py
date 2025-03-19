"""
장면 기반 이미지 생성 라우트
"""
from fastapi import APIRouter, HTTPException, BackgroundTasks
from app.schemas.models import Scene, ImageGenerationResponse
from app.services.openai_service import openai_service
from app.services.klingai_service import image_service
from app.services.download_service import download_service
from app.services.s3_service import s3_service
import os
import time

# 라우터 생성
router = APIRouter(prefix="/images", tags=["images"])

@router.post("/generations/external", response_model=ImageGenerationResponse)
async def generate_scene_image(scene: Scene):
    """
    장면 정보를 기반으로 이미지를 생성합니다.
    
    흐름:
    1. SpringBoot BE로부터 scene 객체 수신
    2. OpenAI를 사용하여 이미지 프롬프트 생성
    3. KLING AI를 사용하여 이미지 생성
    4. 생성된 이미지를 로컬에 저장
    5. 이미지를 S3에 업로드
    6. 결과 반환
    """
    start_time = time.time()
    try:
        # 1. 장면 정보 검증
        if not scene.scene_id:
            raise HTTPException(status_code=400, detail="장면 ID가 누락되었습니다.")
        if not scene.script_metadata:
            raise HTTPException(status_code=400, detail="장면 메타데이터가 누락되었습니다.")
        if not scene.script_metadata.script_id:
            raise HTTPException(status_code=400, detail="스크립트 ID가 누락되었습니다.")
        if not scene.audios or len(scene.audios) == 0:
            raise HTTPException(status_code=400, detail="장면 오디오 정보가 누락되었습니다.")
        
        # 필요한 ID 추출
        script_id = scene.script_metadata.script_id
        scene_id = scene.scene_id
        
        # 2. OpenAI를 사용하여 이미지 프롬프트 생성
        image_prompt_start_time = time.time()
        print(f"스크립트 {script_id}, 장면 {scene_id}에 대한 이미지 프롬프트 생성 중...")
        image_prompt = await openai_service.generate_image_prompt(scene)
        print(f"생성된 이미지 프롬프트: {image_prompt}")
        image_prompt_end_time = time.time()
        image_prompt_time = image_prompt_end_time - image_prompt_start_time
        print(f"이미지 프롬프트 생성 시간: {image_prompt_time}초")
        
        # 3. KLING AI를 사용하여 이미지 생성
        image_generation_start_time = time.time()
        print(f"이미지 생성 중...")
        image_data = await image_service.generate_image(
            prompt=image_prompt,
            negative_prompt="low quality, bad anatomy, blurry, pixelated, disfigured"
        )
        image_generation_end_time = time.time()
        image_generation_time = image_generation_end_time - image_generation_start_time
        print(f"이미지 생성 시간: {image_generation_time}초")
        
        if not image_data or "image_url" not in image_data:
            raise HTTPException(status_code=500, detail="이미지 생성에 실패했습니다.")
        
        image_url = image_data["image_url"]
        print(f"이미지 생성 완료: {image_url}")
        
        # 4. 생성된 이미지를 로컬에 저장
        download_start_time = time.time()   
        print(f"이미지 다운로드 중...")
        local_image_path = await download_service.download_image(image_url, script_id, scene_id)
        download_end_time = time.time()
        download_time = download_end_time - download_start_time
        print(f"이미지 다운로드 시간: {download_time}초")
        
        if not local_image_path:
            raise HTTPException(status_code=500, detail="이미지 다운로드에 실패했습니다.")
        
        # 5. 이미지를 S3에 업로드
        image_upload_start_time = time.time()
        print(f"이미지 S3 업로드 중...")
        s3_url = await s3_service.upload_image(local_image_path, script_id, scene_id)
        image_upload_end_time = time.time()
        image_upload_time = image_upload_end_time - image_upload_start_time
        print(f"이미지 S3 업로드 시간: {image_upload_time}초")
        
        # S3 자격 증명이 없거나 업로드에 실패한 경우, 로컬 URL 사용
        if not s3_url:
            print("S3 업로드 실패, 로컬 URL 사용")
            s3_url = f"/static/images/{os.path.basename(local_image_path)}"
        
        # 6. 결과 반환
        end_time = time.time()
        total_time = end_time - start_time
        print(f"총 처리 시간: {total_time}초")
        return ImageGenerationResponse(
            scene_id=scene_id,
            image_prompt=image_prompt,
            image_s3url=s3_url
        )
    
        
    except HTTPException:
        raise
    except Exception as e:
        print(f"이미지 생성 중 오류 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=f"이미지 생성 중 오류 발생: {str(e)}") 