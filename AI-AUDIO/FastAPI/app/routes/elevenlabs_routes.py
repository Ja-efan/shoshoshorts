from fastapi import APIRouter, HTTPException, BackgroundTasks
from app.service.elevenlabas import (
    generate_tts_with_elevenlabs,
    get_available_voices,
)

from app.schema.elevenlabs import (
    ElevenLabsTTSRequest, 
    ElevenLabsTTSResponse,
)

router = APIRouter(tags=["TTS"], prefix="/elevenlabs")

# ElevenLabs TTS API 엔드포인트 추가
@router.post("/tts", response_model=ElevenLabsTTSResponse)
async def elevenlabs_tts(request: ElevenLabsTTSRequest, background_tasks: BackgroundTasks):
    """
    ElevenLabs API를 사용하여 텍스트를 음성으로 변환하는 API 엔드포인트
    생성된 오디오는 S3에 직접 업로드됩니다.
    """
    try:
        print(f"ElevenLabs TTS 생성 시작: {request.text[:50]}...")
        
        # ElevenLabs TTS 생성 함수 호출 (S3에 직접 업로드)
        response = await generate_tts_with_elevenlabs(request)
        
        print(f"ElevenLabs TTS 생성 및 S3 업로드 완료: {response.s3_url}")
        
        return response
    
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"ElevenLabs TTS 생성 중 오류 발생: {str(e)}")

@router.get("/voices")
async def elevenlabs_voices():
    """
    ElevenLabs에서 사용 가능한 음성 목록을 가져오는 API 엔드포인트
    """
    try:
        voices = await get_available_voices()
        return {"voices": voices}
    
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"ElevenLabs 음성 목록 가져오기 중 오류 발생: {str(e)}")
