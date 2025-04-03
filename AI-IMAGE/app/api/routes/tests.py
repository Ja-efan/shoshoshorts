"""
장면 정보 생성 테스트 모듈
"""
import json
import jwt
import requests
import time
from fastapi import APIRouter, HTTPException
from app.schemas.models import Scene, ImagePromptRequest, SceneInfo, SimpleImagePromptRequest
from app.services.openai_service import OpenAIService
from app.services.utils import encode_image_to_base64
from app.core.logger import app_logger
from app.core.config import settings
from app.core.api_config import klingai_config

router = APIRouter(prefix="/tests", tags=["tests"])


@router.post("/convert_image_to_base64")
async def test_convert_image_to_base64():
    """이미지 파일을 Base64로 인코딩하여 반환"""
    image_path = "images/shoshoshorts/00000001/images/0002_20250331_162613.jpg"
    response = encode_image_to_base64(image_path)
    return response


@router.post("/scene_info")
async def test_generate_scene_info(scene: Scene):
    """이미지 프롬프트 생성 테스트"""
    app_logger.info(f"장면 정보 생성 테스트 시작: \n{json.dumps(scene.model_dump(), ensure_ascii=False, indent=2)}")
    response = await OpenAIService.generate_scene_info(scene)
    app_logger.info(f"장면 정보 생성 테스트 완료: \n{json.dumps(response.model_dump(), ensure_ascii=False, indent=2)}")
    return response


@router.post("/image_prompt")
async def test_generate_image_prompt(scene: Scene):
    """이미지 프롬프트 생성 테스트"""
    app_logger.info(f"이미지 프롬프트 생성 테스트 시작: \n{json.dumps(scene.model_dump(), ensure_ascii=False, indent=2)}")
    response = await OpenAIService.generate_image_prompt(scene)
    prompt = response[0]
    negative_prompt = response[1]
    scene_info = response[2]
    
    app_logger.info(f"이미지 프롬프트 생성 테스트 완료:")
    app_logger.info(f"이미지 프롬프트: {prompt}")
    app_logger.info(f"부정 프롬프트: {negative_prompt}")
    app_logger.info(f"장면 정보: \n{json.dumps(scene_info.model_dump(), ensure_ascii=False, indent=2)}")
    
    return prompt, negative_prompt, scene_info


@router.post("/simple_generate_image")
async def test_generate_image(request: ImagePromptRequest):
    """프롬프트만 입력받아 KlingAI API를 직접 호출하여 이미지 생성 테스트"""
    app_logger.info(f"간단한 이미지 생성 테스트 시작: \n{json.dumps(request.model_dump(), ensure_ascii=False, indent=2)}")
    
    try:
        # API 키 확인
        if not settings.KLING_ACCESS_KEY or not settings.KLING_SECRET_KEY:
            app_logger.error("Kling AI API 키가 설정되지 않았습니다.")
            raise HTTPException(status_code=500, detail="Kling AI API 키가 설정되지 않았습니다.")
        
        # JWT 토큰이 만료되었는지 확인하고 필요하면 갱신
        try:
            jwt.decode(settings.JWT_TOKEN, settings.KLING_SECRET_KEY, algorithms=["HS256"])
        except (jwt.ExpiredSignatureError, Exception):
            # 토큰이 만료되었거나 다른 오류가 발생하면 새로 생성
            from app.core.config import encode_jwt_token
            settings.JWT_TOKEN = encode_jwt_token(settings.KLING_ACCESS_KEY, settings.KLING_SECRET_KEY)
        
        # API 요청 헤더
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {settings.JWT_TOKEN}"
        }
        
        # API 요청 데이터 (레퍼런스 이미지 없이 직접 API 호출)
        payload = {
            "model": klingai_config.MODEL_V1_5,  # 레퍼런스 이미지 없이 V1_5 모델 사용
            "prompt": request.prompt,
            "negative_prompt": request.negative_prompt if request.negative_prompt else "",
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
                error_message = "응답 데이터 파싱 오류"
            
            # Kling AI API의 응답 코드를 표준 HTTP 상태 코드로 변환
            http_status_code = klingai_config.KLING_TO_HTTP.get(response_code, 500)
            
            raise HTTPException(
                status_code=http_status_code,
                detail=f"Kling AI API 오류 (코드: {response_code}): {error_message}"
            )
        
        # 응답 데이터
        app_logger.info(f"응답 데이터: \n{json.dumps(response, ensure_ascii=False, indent=2)}")
        
        response_data = response["data"]
        if response_code == 0 and response_data:
            task_id = response_data["task_id"]
            
            # 이미지 생성 태스크 결과 가져오기
            image_urls = await get_task_result(task_id)
            
            if image_urls and len(image_urls) > 0:
                # 결과 반환
                result = {
                    "image_url": image_urls[0]["url"],
                    "prompt": request.prompt,
                    "style": request.style
                }
                
                app_logger.info(f"이미지 생성 테스트 완료: \n{json.dumps(result, ensure_ascii=False, indent=2)}")
                return result
            else:
                app_logger.error("이미지 생성 실패: KLINAI image url 없음")
                raise HTTPException(status_code=500, detail="이미지 생성 실패: KLINAI image url 없음")
                
    except HTTPException:
        raise
    except Exception as e:
        app_logger.error(f"이미지 생성 중 오류 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=f"이미지 생성 중 오류 발생: {str(e)}")

async def get_task_result(task_id: str, max_attempts: int = 10, delay: int = 3):
    """이미지 생성 태스크의 결과를 가져옵니다."""
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
