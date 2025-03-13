#base 64

import os
import torch
import torchaudio
import tempfile
import base64
import uvicorn
import argparse
import asyncio
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any, Union
import numpy as np
from io import BytesIO
import soundfile as sf
from datetime import datetime

# zonos 모듈 경로 수정 (상위 디렉토리 참조)
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from zonos.model import Zonos, DEFAULT_BACKBONE_CLS as ZonosBackbone
from zonos.conditioning import make_cond_dict, supported_language_codes
from zonos.utils import DEFAULT_DEVICE as device

# 스크립트 변환 모듈 임포트
from app.service.scripts import ScriptRequest, ScriptResponse, generate_script_json

# ElevenLabs TTS 모듈 임포트
from app.service.elevenlabas import (
    ElevenLabsTTSRequest, 
    ElevenLabsTTSResponse, 
    generate_tts_with_elevenlabs,
    get_available_voices,
)

# 전역 변수 선언: 현재 로드된 모델 타입과 모델 인스턴스를 저장
CURRENT_MODEL_TYPE = None
CURRENT_MODEL = None

# 화자 임베딩 캐시를 저장하는 딕셔너리
SPEAKER_EMBEDDING_CACHE = {}

# 서버 시작 이벤트 핸들러 추가
async def startup_event():
    """
    서버 시작 시 실행되는 이벤트 핸들러
    기본 모델을 미리 로드합니다.
    """
    try:
        # 환경 변수에서 기본 모델 타입 가져오기 (없으면 transformer 사용)
        default_model = os.environ.get("ZONOS_DEFAULT_MODEL", "Zyphra/Zonos-v0.1-transformer")
        print(f"서버 시작 시 기본 모델 '{default_model}' 로드 중...")
        
        # 모델 로드
        model = load_model_if_needed(default_model)
        print(f"모델 '{default_model}' 로드 완료! 서버 준비 완료.")
    except Exception as e:
        print(f"모델 로드 중 오류 발생: {str(e)}")
        print("서버는 시작되지만, 첫 요청 시 모델을 로드해야 합니다.")

app = FastAPI(
    title="Zonos TTS API",
    description="텍스트를 음성으로 변환하는 Zonos TTS 모델 API",
    version="1.0.0",
)

# 시작 이벤트 핸들러 등록
# app.add_event_handler("startup", startup_event)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 모든 오리진 허용 (프로덕션에서는 특정 오리진만 허용하는 것이 좋음)
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 요청 모델 정의
class TTSRequest(BaseModel):
    model_choice: str = Field(
        default="Zyphra/Zonos-v0.1-transformer", 
        description="사용할 모델 타입 (transformer 또는 hybrid)"
    )
    text: str = Field(
        default="안녕하세요, Zonos TTS 모델입니다.", 
        description="합성할 텍스트"
    )
    language: str = Field(
        default="ko", 
        description="언어 코드"
    )
    speaker_audio_path: Optional[str] = Field(
        default=None, 
        description="화자 오디오 파일 경로 (선택 사항)"
    )
    speaker_audio_base64: Optional[str] = Field(
        default=None, 
        description="Base64로 인코딩된 화자 오디오 데이터 (선택 사항, 이전 버전 호환용)"
    )
    speaker_id: Optional[str] = Field(
        default=None, 
        description="이전에 등록된 화자 ID (speaker_audio_path가 없을 경우 사용)"
    )
    emotion: List[float] = Field(
        default=[0.8, 0.05, 0.05, 0.05, 0.05, 0.05, 0.1, 0.2], 
        description="8개의 감정 값 (행복, 슬픔, 혐오, 두려움, 놀람, 분노, 기타, 중립)"
    )
    vq_score: float = Field(
        default=0.78, 
        description="VQ 점수 (0.5-0.8)"
    )
    fmax: float = Field(
        default=24000, 
        description="최대 주파수 (Hz)"
    )
    pitch_std: float = Field(
        default=45.0, 
        description="피치 표준 편차"
    )
    speaking_rate: float = Field(
        default=15.0, 
        description="말하기 속도"
    )
    dnsmos_ovrl: float = Field(
        default=4.0, 
        description="DNSMOS 전체 점수"
    )
    speaker_noised: bool = Field(
        default=False, 
        description="화자 노이즈 제거 여부"
    )
    cfg_scale: float = Field(
        default=2.0, 
        description="CFG 스케일"
    )
    sampling_params: Dict[str, Any] = Field(
        default={
            "top_p": 0.0,
            "top_k": 0,
            "min_p": 0.0,
            "linear": 0.5,
            "conf": 0.4,
            "quad": 0.0
        }, 
        description="샘플링 파라미터"
    )
    seed: int = Field(
        default=42, 
        description="랜덤 시드"
    )
    randomize_seed: bool = Field(
        default=False, 
        description="시드 랜덤화 여부"
    )
    unconditional_keys: List[str] = Field(
        default=["emotion"], 
        description="비조건부 키 목록"
    )

# 응답 모델 정의
class TTSResponse(BaseModel):
    audio_path: str = Field(
        description="생성된 오디오 파일 경로"
    )
    audio_base64: Optional[str] = Field(
        default=None,
        description="Base64로 인코딩된 오디오 데이터 (이전 버전 호환용)"
    )
    sample_rate: int = Field(
        description="오디오 샘플 레이트"
    )
    seed: int = Field(
        description="사용된 시드 값"
    )

# 화자 등록 요청 모델
class RegisterSpeakerRequest(BaseModel):
    speaker_id: str = Field(
        description="등록할 화자 ID"
    )
    speaker_audio_path: Optional[str] = Field(
        default=None,
        description="화자 오디오 파일 경로"
    )
    speaker_audio_base64: Optional[str] = Field(
        default=None,
        description="Base64로 인코딩된 화자 오디오 데이터 (이전 버전 호환용)"
    )

# 화자 등록 응답 모델
class RegisterSpeakerResponse(BaseModel):
    speaker_id: str = Field(
        description="등록된 화자 ID"
    )
    message: str = Field(
        description="등록 결과 메시지"
    )

def load_model_if_needed(model_choice: str):
    """
    필요한 경우에만 모델을 로드하는 함수
    
    Args:
        model_choice: 로드할 모델 타입 (예: transformer, hybrid)
        
    Returns:
        로드된 모델 인스턴스
    """
    global CURRENT_MODEL_TYPE, CURRENT_MODEL
    if CURRENT_MODEL_TYPE != model_choice:
        if CURRENT_MODEL is not None:
            del CURRENT_MODEL
            torch.cuda.empty_cache()  # GPU 메모리 정리
        print(f"모델 로딩 중: {model_choice}...")
        CURRENT_MODEL = Zonos.from_pretrained(model_choice, device=device)
        CURRENT_MODEL.requires_grad_(False).eval()  # 평가 모드로 설정하고 그래디언트 계산 비활성화
        CURRENT_MODEL_TYPE = model_choice
        print(f"모델 로딩 완료: {model_choice}!")
    return CURRENT_MODEL

def load_audio_from_path(audio_path: str):
    """
    오디오 파일 경로에서 오디오를 로드하는 함수
    
    Args:
        audio_path: 오디오 파일 경로
        
    Returns:
        로드된 오디오 텐서와 샘플 레이트
    """
    # 오디오 파일 로드
    wav, sr = torchaudio.load(audio_path)
    return wav, sr

def decode_audio_base64(audio_base64: str):
    """
    Base64로 인코딩된 오디오 데이터를 디코딩하는 함수
    
    Args:
        audio_base64: Base64로 인코딩된 오디오 데이터
        
    Returns:
        디코딩된 오디오 텐서와 샘플 레이트
    """
    try:
        audio_data = base64.b64decode(audio_base64)
        
        # 임시 파일에 오디오 데이터 저장
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as temp_file:
            temp_file.write(audio_data)
            temp_file_path = temp_file.name
        
        try:
            # 오디오 파일 로드
            wav, sr = torchaudio.load(temp_file_path)
            return wav, sr
        finally:
            # 임시 파일 삭제
            os.unlink(temp_file_path)
    except Exception as e:
        raise ValueError(f"Base64 디코딩 중 오류 발생: {str(e)}")

def save_audio_to_file(audio_array: np.ndarray, sample_rate: int, file_path: str):
    """
    오디오 배열을 파일로 저장하는 함수
    
    Args:
        audio_array: 오디오 배열
        sample_rate: 샘플 레이트
        file_path: 저장할 파일 경로
        
    Returns:
        저장된 파일 경로
    """
    # 파일 디렉토리가 존재하는지 확인하고 없으면 생성
    os.makedirs(os.path.dirname(file_path), exist_ok=True)
    
    # 오디오 배열이 1D인지 확인하고 필요한 경우 차원 조정
    if len(audio_array.shape) == 1:
        audio_tensor = torch.tensor(audio_array).unsqueeze(0)
    else:
        audio_tensor = torch.tensor(audio_array)
    
    # 파일 확장자에 따라 포맷 결정
    file_ext = os.path.splitext(file_path)[1].lower()
    
    try:
        if file_ext == '.mp3':
            sf.write(file_path, audio_array, sample_rate)
        else:
            torchaudio.save(file_path, audio_tensor, sample_rate)
        
        # 파일이 실제로 저장되었는지 확인
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"파일이 저장되지 않았습니다: {file_path}")
        
        # 파일 크기가 0인지 확인
        if os.path.getsize(file_path) == 0:
            raise ValueError(f"저장된 파일의 크기가 0입니다: {file_path}")
        
        print(f"파일 저장 성공: {file_path} (크기: {os.path.getsize(file_path)} 바이트)")
        return file_path
    except Exception as e:
        print(f"파일 저장 중 오류 발생: {str(e)}")
        raise

def encode_audio_to_base64(audio_array: np.ndarray, sample_rate: int):
    """
    오디오 배열을 Base64로 인코딩하는 함수
    
    Args:
        audio_array: 오디오 배열
        sample_rate: 샘플 레이트
        
    Returns:
        Base64로 인코딩된 오디오 데이터
    """
    # 오디오 배열을 WAV 형식으로 변환
    buffer = BytesIO()
    
    # 오디오 배열이 1D인지 확인하고 필요한 경우 차원 조정
    if len(audio_array.shape) == 1:
        audio_tensor = torch.tensor(audio_array).unsqueeze(0)
    else:
        audio_tensor = torch.tensor(audio_array)
    
    torchaudio.save(buffer, audio_tensor, sample_rate, format="wav")
    buffer.seek(0)
    
    # Base64로 인코딩
    audio_base64 = base64.b64encode(buffer.read()).decode("utf-8")
    return audio_base64

@app.post("/zonos/tts", response_model=TTSResponse)
async def text_to_speech(request: TTSRequest, background_tasks: BackgroundTasks):
    """
    텍스트를 음성으로 변환하는 API 엔드포인트
    생성된 오디오는 output 폴더에 저장됩니다.
    """
    try:
        # 모델 로드
        model = load_model_if_needed(request.model_choice)
        
        # 입력 파라미터 타입 변환 및 검증
        speaker_noised_bool = bool(request.speaker_noised)
        fmax = float(request.fmax)
        pitch_std = float(request.pitch_std)
        speaking_rate = float(request.speaking_rate)
        dnsmos_ovrl = float(request.dnsmos_ovrl)
        cfg_scale = float(request.cfg_scale)
        
        # 샘플링 파라미터 추출
        sampling_params = request.sampling_params
        
        # 시드 설정
        seed = request.seed
        if request.randomize_seed:
            seed = torch.randint(0, 2**32 - 1, (1,)).item()
        torch.manual_seed(seed)
        
        # 화자 임베딩 처리
        speaker_embedding = None
        if request.speaker_audio_path:
            # 파일 경로에서 화자 임베딩 생성
            wav, sr = load_audio_from_path(request.speaker_audio_path)
            speaker_embedding = model.make_speaker_embedding(wav, sr)
            speaker_embedding = speaker_embedding.to(device, dtype=torch.bfloat16)
        elif request.speaker_audio_base64:
            # Base64로 인코딩된 오디오에서 화자 임베딩 생성
            try:
                wav, sr = decode_audio_base64(request.speaker_audio_base64)
                speaker_embedding = model.make_speaker_embedding(wav, sr)
                speaker_embedding = speaker_embedding.to(device, dtype=torch.bfloat16)
            except Exception as e:
                print(f"Base64 디코딩 중 오류 발생: {str(e)}")
                # 오류가 발생해도 계속 진행 (speaker_embedding은 None으로 유지)
        elif request.speaker_id and request.speaker_id in SPEAKER_EMBEDDING_CACHE:
            # 캐시된 화자 임베딩 사용
            speaker_embedding = SPEAKER_EMBEDDING_CACHE[request.speaker_id]
        
        # 감정 텐서 생성
        emotion_tensor = torch.tensor(request.emotion, device=device)
        
        # VQ 점수 텐서 생성
        vq_tensor = torch.tensor([request.vq_score] * 8, device=device).unsqueeze(0)
        
        # 조건부 딕셔너리 생성
        cond_dict = make_cond_dict(
            text=request.text,
            language=request.language,
            speaker=speaker_embedding,
            emotion=emotion_tensor,
            vqscore_8=vq_tensor,
            fmax=fmax,
            pitch_std=pitch_std,
            speaking_rate=speaking_rate,
            dnsmos_ovrl=dnsmos_ovrl,
            speaker_noised=speaker_noised_bool,
            device=device,
            unconditional_keys=request.unconditional_keys,
        )
        conditioning = model.prepare_conditioning(cond_dict)
        
        # 오디오 생성
        max_new_tokens = 86 * 30  # 약 30초 분량의 오디오 생성 (86 토큰/초)
        
        # 생성 시간 및 단계 추정
        estimated_generation_duration = 30 * len(request.text) / 400
        estimated_total_steps = int(estimated_generation_duration * 86)
        
        # 진행 상태 업데이트 콜백 함수 (로깅용)
        def update_progress(_frame: torch.Tensor, step: int, _total_steps: int) -> bool:
            if step % 10 == 0:  # 10단계마다 로그 출력
                print(f"생성 진행 중: {step}/{estimated_total_steps} ({step/estimated_total_steps*100:.1f}%)")
            return True
        
        codes = model.generate(
            prefix_conditioning=conditioning,
            audio_prefix_codes=None,
            max_new_tokens=max_new_tokens,
            cfg_scale=cfg_scale,
            batch_size=1,
            sampling_params=sampling_params,
            callback=update_progress,
        )
        
        # 생성된 코드를 오디오로 디코딩
        wav_out = model.autoencoder.decode(codes).cpu().detach()
        sr_out = model.autoencoder.sampling_rate
        if wav_out.dim() == 2 and wav_out.size(0) > 1:
            wav_out = wav_out[0:1, :]
        
        # 현재 스크립트 경로 기준으로 output 폴더 생성
        script_dir = os.path.dirname(os.path.abspath(__file__))
        base_dir = os.path.dirname(script_dir)  # AI 폴더
        output_dir = os.path.join(base_dir, "output")
        os.makedirs(output_dir, exist_ok=True)
        
        # 파일명 생성 (시간 기반)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        text_short = request.text[:20].replace(" ", "_")  # 텍스트 일부를 파일명에 포함
        filename = os.path.join(output_dir, f"tts_{timestamp}_{text_short}_{seed}.mp3")
        
        # 오디오 저장
        print(f"오디오 파일 저장 중: {filename}")
        try:
            save_audio_to_file(wav_out.squeeze().numpy(), sr_out, filename)
            print(f"오디오 파일 저장 완료: {filename}")
            
            # 파일이 실제로 존재하는지 확인
            if not os.path.exists(filename):
                print(f"경고: 파일이 저장되지 않았습니다: {filename}")
        except Exception as e:
            print(f"오디오 파일 저장 중 오류 발생: {str(e)}")
            # 오류가 발생해도 계속 진행
        
        # 메모리 정리를 위한 백그라운드 태스크
        background_tasks.add_task(torch.cuda.empty_cache)
        
        # 상대 경로로 변환하여 반환 (절대 경로는 보안상 문제가 될 수 있음)
        relative_path = os.path.relpath(filename, base_dir)
        
        # 오디오를 Base64로 인코딩
        audio_base64 = encode_audio_to_base64(wav_out.squeeze().numpy(), sr_out)
        
        return TTSResponse(
            audio_path=relative_path,
            audio_base64=audio_base64,
            sample_rate=sr_out,
            seed=seed
        )
    
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"TTS 처리 중 오류 발생: {str(e)}")

@app.post("/register_speaker", response_model=RegisterSpeakerResponse)
async def register_speaker(request: RegisterSpeakerRequest):
    """
    화자를 등록하는 API 엔드포인트
    """
    try:
        # 모델이 로드되어 있는지 확인
        if CURRENT_MODEL is None:
            # 기본 모델 로드
            load_model_if_needed("Zyphra/Zonos-v0.1-transformer")
        
        # 화자 임베딩 생성
        speaker_embedding = None
        
        if request.speaker_audio_path:
            # 파일 경로에서 화자 임베딩 생성
            wav, sr = load_audio_from_path(request.speaker_audio_path)
            speaker_embedding = CURRENT_MODEL.make_speaker_embedding(wav, sr)
        elif request.speaker_audio_base64:
            # Base64로 인코딩된 오디오에서 화자 임베딩 생성
            wav, sr = decode_audio_base64(request.speaker_audio_base64)
            speaker_embedding = CURRENT_MODEL.make_speaker_embedding(wav, sr)
        else:
            raise ValueError("화자 오디오 파일 경로 또는 Base64 데이터가 필요합니다.")
        
        speaker_embedding = speaker_embedding.to(device, dtype=torch.bfloat16)
        
        # 화자 임베딩 캐시에 저장
        SPEAKER_EMBEDDING_CACHE[request.speaker_id] = speaker_embedding
        
        return RegisterSpeakerResponse(
            speaker_id=request.speaker_id,
            message=f"화자 '{request.speaker_id}'가 성공적으로 등록되었습니다."
        )
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"화자 등록 중 오류 발생: {str(e)}")

# 스크립트 변환 API 엔드포인트 추가
@app.post("/script/convert", response_model=ScriptResponse)
async def convert_script(request: ScriptRequest):
    """
    스크립트 내용을 JSON 형식으로 변환하는 API 엔드포인트
    """
    try:
        # 스크립트 변환 함수 호출
        response = await generate_script_json(request)
        return response
    
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"스크립트 변환 중 오류 발생: {str(e)}")

# ElevenLabs TTS API 엔드포인트 추가
@app.post("/elevenlabs/tts", response_model=ElevenLabsTTSResponse)
async def elevenlabs_tts(request: ElevenLabsTTSRequest, background_tasks: BackgroundTasks):
    """
    ElevenLabs API를 사용하여 텍스트를 음성으로 변환하는 API 엔드포인트
    생성된 오디오는 output/elevenlabs 폴더에 저장됩니다.
    """
    try:
        # 현재 스크립트 경로 기준으로 output 폴더 생성
        script_dir = os.path.dirname(os.path.abspath(__file__))
        base_dir = os.path.dirname(script_dir)  # AI 폴더
        output_dir = os.path.join(base_dir, "output", "elevenlabs")
        os.makedirs(output_dir, exist_ok=True)
        
        # 파일명 생성 (시간 기반)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        text_short = request.text[:20].replace(" ", "_").replace("/", "_").replace("\\", "_")  # 텍스트 일부를 파일명에 포함 (특수문자 제거)
        filename = os.path.join(output_dir, f"elevenlabs_tts_{timestamp}_{text_short}.{request.output_format}")
        
        print(f"ElevenLabs TTS 생성 시작: {filename}")
        
        # ElevenLabs TTS 생성 함수 호출
        response = await generate_tts_with_elevenlabs(request, save_path=filename)
        
        # 상대 경로로 변환 (절대 경로는 보안상 문제가 될 수 있음)
        relative_path = os.path.relpath(filename, base_dir)
        
        # 응답 객체 생성
        result = ElevenLabsTTSResponse(
            audio_path=relative_path,
            content_type=response.content_type,
            file_size=response.file_size
        )
        
        print(f"ElevenLabs TTS 생성 완료: {relative_path}")
        
        return result
    
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"ElevenLabs TTS 생성 중 오류 발생: {str(e)}")

@app.get("/elevenlabs/voices")
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

if __name__ == "__main__":
    # 명령행 인자 파싱
    parser = argparse.ArgumentParser(description="API 서버")
    parser.add_argument(
        "--model", 
        type=str, 
        default="Zyphra/Zonos-v0.1-transformer",
        choices=["Zyphra/Zonos-v0.1-transformer", "Zyphra/Zonos-v0.1-hybrid"],
        help="사용할 모델 타입 (transformer 또는 hybrid)"
    )
    parser.add_argument("--host", type=str, default="0.0.0.0", help="서버 호스트 주소")
    parser.add_argument("--port", type=int, default=8000, help="서버 포트 번호")
    parser.add_argument("--reload", action="store_true", help="코드 변경 시 서버 자동 재시작")
    
    args = parser.parse_args()
    
    # 명령행에서 지정한 모델을 환경 변수로 설정
    os.environ["ZONOS_DEFAULT_MODEL"] = args.model
    
    # 서버 실행
    uvicorn.run(
        "__main__:app",  # main:app 대신 __main__:app 사용
        host=args.host, 
        port=args.port, 
        reload=args.reload
    ) 