"""
장면 기반 이미지 생성 라우트
"""
import time
from fastapi import APIRouter, HTTPException
from app.schemas.models import Scene, ImageGenerationResponse
from app.services.openai_service import openai_service
from app.services.klingai_service import image_service
from app.services.download_service import download_service
from app.services.s3_service import s3_service
from app.core.logger import app_logger
from app.core.config import settings

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
    app_logger.info("이미지 생성 함수 시작")
    app_logger.debug(f"scene: \n{scene}")
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
        # image_prompt_start_time = time.time()   
        app_logger.info(f"스토리 {story_id}, 장면 {scene_id}")
        image_prompt, negative_prompt = await openai_service.generate_image_prompt(scene)
        # app_logger.debug(f"생성된 이미지 프롬프트: \n{image_prompt}")
        # image_prompt_end_time = time.time()
        # image_prompt_time = image_prompt_end_time - image_prompt_start_time
        # app_logger.info(f"이미지 프롬프트 생성 시간: {image_prompt_time}초")
        
        # 3. KLING AI를 사용하여 이미지 생성
        # image_generation_start_time = time.time()
        app_logger.info("이미지 생성 중...")
        image_data = await image_service.generate_image(
            story_id=story_id,
            scene_id=scene_id,
            prompt=image_prompt,
            negative_prompt=negative_prompt
        )
        # image_generation_end_time = time.time()
        # image_generation_time = image_generation_end_time - image_generation_start_time
        # app_logger.info(f"이미지 생성 시간: {image_generation_time}초")
        
        if not image_data or "image_url" not in image_data:
            raise HTTPException(status_code=500, detail="이미지 생성에 실패했습니다.")
        
        image_url = image_data["image_url"]
        app_logger.info(f"이미지 생성 완료: {image_url}")
        
        # 4. 생성된 이미지를 로컬에 저장
        # download_start_time = time.time()   
        # app_logger.info("이미지 다운로드 중...")
        local_image_path = await download_service.download_image(image_url, story_id, scene_id)
        download_end_time = time.time()
        # download_time = download_end_time - download_start_time
        # app_logger.info(f"이미지 다운로드 시간: {download_time}초")
        
        if not local_image_path:
            raise HTTPException(status_code=500, detail="이미지 다운로드에 실패했습니다.")
        
        # 5. 이미지를 S3에 업로드
        # image_upload_start_time = time.time()
        app_logger.info("이미지 S3 업로드 중...")
        s3_result = await s3_service.upload_image(local_image_path, story_id, scene_id)
        # image_upload_end_time = time.time()
        # image_upload_time = image_upload_end_time - image_upload_start_time
        # app_logger.info(f"이미지 S3 업로드 시간: {image_upload_time}초")
        
        # S3 업로드 결과 처리
        if s3_result["success"]:
            s3_url = s3_result["url"]
            app_logger.info(f"S3 업로드 성공: {s3_url}")
        else:
            # 업로드 실패 처리
            error_msg = s3_result["error"]
            error_type = s3_result["error_type"]
            local_path = s3_result["local_path"]
            
            # 환경 설정에 따라 로컬 URL 사용 또는 에러 발생
            if settings.USE_LOCAL_URL_ON_S3_FAILURE:
                app_logger.warning(f"S3 업로드 실패 ({error_type}): {error_msg}. 로컬 URL을 대체 사용합니다.")
        
                if local_path.startswith("images/"):
                    relative_path = local_path[7:]  # 'images/' 부분 제거
                    s3_url = f"/static/images/{relative_path}"
                else:
                    # 경로에 'images/'가 없으면 그대로 사용
                    s3_url = f"/static/images/{local_path}"
                app_logger.info(f"대체 로컬 URL 생성: {s3_url}")
            else:
                # 오류 유형에 따른 HTTP 상태 코드 매핑
                status_code = 500
                if error_type == "file_not_found":
                    status_code = 404
                elif error_type == "invalid_credentials" or error_type == "credentials_missing":
                    status_code = 401
                
                # 오류 반환
                raise HTTPException(
                    status_code=status_code,
                    detail=f"S3 업로드 실패: {error_msg} (오류 유형: {error_type})"
                )
        
        # 6. 결과 반환
        end_time = time.time()
        total_time = end_time - start_time
        app_logger.info(f"총 처리 시간: {total_time}초")
        return ImageGenerationResponse(
            scene_id=scene_id,
            image_prompt=image_prompt,
            image_url=s3_url
        )
    
        
    except HTTPException:
        raise
    except Exception as e:
        app_logger.error(f"이미지 생성 중 오류 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=f"이미지 생성 중 오류 발생: {str(e)}") 