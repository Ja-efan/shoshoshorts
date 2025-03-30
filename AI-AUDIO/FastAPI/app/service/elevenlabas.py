import os
import requests
from typing import Dict, Any, List
from dotenv import load_dotenv
from datetime import datetime
# S3 모듈 임포트
from app.service.s3 import upload_binary_to_s3
import pytz
from app.schema.elevenlabs import (
    ElevenLabsTTSRequest, 
    ElevenLabsTTSResponse,
)

load_dotenv()

# ElevenLabs API 키 가져오기
api_key = os.getenv("ELEVENLABS_API_KEY")
if not api_key:
    raise ValueError("ELEVENLABS_API_KEY가 .env 파일에 설정되어 있지 않습니다.")

# ElevenLabs API 기본 URL
ELEVENLABS_API_URL = "https://api.elevenlabs.io/v1"

async def generate_tts_with_elevenlabs(request: ElevenLabsTTSRequest) -> ElevenLabsTTSResponse:
    """
    ElevenLabs API를 사용하여 텍스트를 음성으로 변환하고 S3에 직접 업로드하는 함수
    
    Args:
        request: TTS 요청 객체
        
    Returns:
        TTS 응답 객체
    """
    try:
        if request.voice_code is None:
            cur_voice_code = "uyVNoMrnUku1dZyVEXwD"
        else:
            cur_voice_code = request.voice_code

        # API 요청 URL 구성
        url = f"{ELEVENLABS_API_URL}/text-to-speech/{cur_voice_code}"
        
        # 요청 헤더 설정
        headers = {
            "xi-api-key": api_key,
            "Content-Type": "application/json",
            "Accept": f"audio/{request.output_format}"
        }
        
        # 요청 본문 구성
        payload = {
            "text": request.text,
            "model_id": request.model_id,
            "output_format": request.output_format
        }
            
        print(f"ElevenLabs API 요청 중: {url}")
        print(f"텍스트: {request.text[:50]}{'...' if len(request.text) > 50 else ''}")
        
        # API 요청 보내기
        response = requests.post(url, json=payload, headers=headers)
        
        # 응답 확인
        if response.status_code != 200:
            error_message = f"ElevenLabs API 오류: {response.status_code}"
            try:
                error_data = response.json()
                error_message += f" - {error_data.get('detail', {}).get('message', '')}"
            except:
                error_message += f" - {response.text}"
            raise ValueError(error_message)
        
        # 오디오 데이터 가져오기
        audio_data = response.content
        content_type = response.headers.get("Content-Type", f"audio/{request.output_format}")
        
        # 파일명 생성 (시간 기반)
        kst = pytz.timezone('Asia/Seoul')
        timestamp = datetime.now(kst).strftime("%Y%m%d_%H%M%S")

        formatted_script_id = f"{request.script_id:08d}"  # 8자리 (예: 00000001)
        formatted_scene_id = f"{request.scene_id:04d}"  # 4자리 (예: 0001)
        formatted_audio_id = f"{request.audio_id:04d}"  # 4자리 (예: 0001)
        object_name = f"{formatted_script_id}/audios/{formatted_scene_id}_{formatted_audio_id}_{timestamp}.{request.output_format}"
        
        # S3에 직접 업로드
        print(f"오디오 데이터를 S3에 업로드 중: {len(audio_data)} 바이트")
        upload_result = upload_binary_to_s3(audio_data, object_name, content_type)
        
        if not upload_result["success"]:
            raise ValueError(f"S3 업로드 실패: {upload_result.get('error')}")
        
        print(f"S3 업로드 완료: {upload_result['url']}")
        
        return ElevenLabsTTSResponse(
            s3_url=upload_result["url"],
            content_type=content_type,
            file_size=len(audio_data)
        )
    
    except Exception as e:
        # 오류 발생 시 로깅 및 예외 처리
        print(f"ElevenLabs TTS 생성 중 오류 발생: {str(e)}")
        raise

async def get_available_voices() -> List[Dict[str, Any]]:
    """
    ElevenLabs에서 사용 가능한 음성 목록을 가져오는 함수
    
    Returns:
        사용 가능한 음성 목록
    """
    try:
        # API 요청 URL 구성
        url = f"{ELEVENLABS_API_URL}/voices"
        
        # 요청 헤더 설정
        headers = {
            "xi-api-key": api_key,
            "Content-Type": "application/json"
        }
        
        # API 요청 보내기
        response = requests.get(url, headers=headers)
        
        # 응답 확인
        if response.status_code != 200:
            error_message = f"ElevenLabs API 오류: {response.status_code}"
            try:
                error_data = response.json()
                error_message += f" - {error_data.get('detail', {}).get('message', '')}"
            except:
                error_message += f" - {response.text}"
            raise ValueError(error_message)
        
        # 음성 목록 가져오기
        voices_data = response.json()
        return voices_data.get("voices", [])
    
    except Exception as e:
        # 오류 발생 시 로깅 및 예외 처리
        print(f"ElevenLabs 음성 목록 가져오기 중 오류 발생: {str(e)}")
        raise

