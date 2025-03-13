import os
import requests
import json
import base64
from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field
from dotenv import load_dotenv

load_dotenv()

# ElevenLabs API 키 가져오기
api_key = os.getenv("ELEVENLABS_API_KEY")
if not api_key:
    raise ValueError("ELEVENLABS_API_KEY가 .env 파일에 설정되어 있지 않습니다.")

# ElevenLabs API 기본 URL
ELEVENLABS_API_URL = "https://api.elevenlabs.io/v1"

# 요청 모델 정의
class ElevenLabsTTSRequest(BaseModel):
    text: str = Field(description="변환할 텍스트")
    #남자 목소리: 4JJwo477JUAx3HV0T7n7
    #여자 목소리: uyVNoMrnUku1dZyVEXwD
    voice_id: str = Field(default="uyVNoMrnUku1dZyVEXwD", description="사용할 음성 ID")
    model_id: str = Field(default="eleven_multilingual_v2", description="사용할 모델 ID")
    output_format: str = Field(
        default="mp3", 
        description="출력 포맷 (mp3, pcm, wav, ogg, flac)"
    )

# 응답 모델 정의
class ElevenLabsTTSResponse(BaseModel):
    audio_path: str = Field(description="저장된 오디오 파일 경로")
    content_type: str = Field(description="오디오 콘텐츠 타입")
    file_size: int = Field(description="오디오 파일 크기 (바이트)")

async def generate_tts_with_elevenlabs(request: ElevenLabsTTSRequest, save_path: str) -> ElevenLabsTTSResponse:
    """
    ElevenLabs API를 사용하여 텍스트를 음성으로 변환하고 파일로 저장하는 함수
    
    Args:
        request: TTS 요청 객체
        save_path: 오디오를 저장할 경로
        
    Returns:
        TTS 응답 객체
    """
    try:
        # 저장 경로 디렉토리가 존재하는지 확인하고 없으면 생성
        os.makedirs(os.path.dirname(save_path), exist_ok=True)
        
        # API 요청 URL 구성
        url = f"{ELEVENLABS_API_URL}/text-to-speech/{request.voice_id}"
        
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
        
        # 파일로 저장
        with open(save_path, "wb") as f:
            f.write(audio_data)
        
        # 파일 크기 확인
        file_size = os.path.getsize(save_path)
        
        print(f"오디오 파일 저장 완료: {save_path} (크기: {file_size} 바이트)")
        
        return ElevenLabsTTSResponse(
            audio_path=save_path,
            content_type=content_type,
            file_size=file_size
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

