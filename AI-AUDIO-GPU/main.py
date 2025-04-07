import os
import torch
import torchaudio
import tempfile
import uvicorn
import argparse
import asyncio
from fastapi import FastAPI, HTTPException, BackgroundTasks, Request
from fastapi.middleware.cors import CORSMiddleware
import numpy as np
from io import BytesIO
import soundfile as sf
from datetime import datetime
import logging
import pytz
import base64
import uuid

from zonos.model import Zonos, DEFAULT_BACKBONE_CLS as ZonosBackbone
from zonos.conditioning import make_cond_dict, supported_language_codes
from zonos.utils import DEFAULT_DEVICE as device

from starlette.responses import JSONResponse
from app.service.s3 import set_environment

from app.service.s3 import upload_binary_to_s3

from app.schema.zonos import (
    TTSRequest,
    TTSResponse,
    RegisterSpeakerRequest,
    RegisterSpeakerResponse
)

from app.service.speaker_embedding_cache import SPEAKER_EMBEDDING_CACHE

logger = logging.getLogger(__name__)

# 전역 변수 선언: 현재 로드된 모델 타입과 모델 인스턴스를 저장
CURRENT_MODEL_TYPE = None
CURRENT_MODEL = None

# 서버 시작 이벤트 핸들러 추가
async def startup_event():
    """
    서버 시작 시 실행되는 이벤트 핸들러
    기본 모델을 미리 로드합니다.
    """
    try:
        # 환경 변수에서 기본 모델 타입 가져오기 (없으면 transformer 사용)
        default_model = os.environ.get("ZONOS_DEFAULT_MODEL", "Zyphra/Zonos-v0.1-transformer")
        logger.info(f"서버 시작 시 기본 모델 '{default_model}' 로드 중...")
        
        # 모델 로드
        model = load_model_if_needed(default_model)
        logger.info(f"모델 '{default_model}' 로드 완료! 서버 준비 완료.")
    except Exception as e:
        logger.info(f"모델 로드 중 오류 발생: {str(e)}")
        logger.info("서버는 시작되지만, 첫 요청 시 모델을 로드해야 합니다.")

ENV = os.getenv("ENV", "development")  # 기본값은 development
API_PWD = os.getenv("API_PWD")

if ENV == "prod":
    docs_url = None
    redoc_url = None
    openapi_url = None
else:
    docs_url = "/docs"
    redoc_url = "/redoc"
    openapi_url = "/openapi.json"

app = FastAPI(
    title="Zonos TTS API",
    description="텍스트를 음성으로 변환하는 Zonos TTS 모델 API",
    version="1.0.0",
    docs_url = docs_url,
    redoc_url = redoc_url,
    openapi_url = openapi_url
)

# 시작 이벤트 핸들러 등록
app.add_event_handler("startup", startup_event)

# 비밀번호 관련 middleware
@app.middleware("http")
async def check_pwd_middleware(request: Request, call_next):
    # POST 요청에만 적용
    if request.method == "POST":
        api_pwd = request.headers.get("apiPwd")
        
        # 비밀번호가 없거나 유효하지 않은 경우
        if not api_pwd:
            return JSONResponse(
                status_code=401,
                content={"message": "Missing API pwd"}
            )
        
        # 개발 환경 비밀번호 확인 (dev로 시작하는 비밀번호는 개발 환경으로 인식)
        if api_pwd.startswith("dev"):
            # 개발 환경으로 설정
            set_environment(is_dev_environment=True)
            # 유효한 개발 환경 비밀번호인지 확인
            if api_pwd != "dev"+API_PWD:
                return JSONResponse(
                    status_code=401,
                    content={"message": "Invalid development API pwd"}
                )
        elif api_pwd.startswith("prod"):
            # 프로덕션 환경으로 설정
            set_environment(is_dev_environment=False)
            # 유효한 프로덕션 비밀번호인지 확인
            if api_pwd != "prod"+API_PWD:
                return JSONResponse(
                    status_code=401,
                    content={"message": "Invalid production API pwd"}
                )
        else:
            return JSONResponse(
                    status_code=401,
                    content={"message": "API pwd 앞에 dev 또는 prod가 없습니다."}
                )

    response = await call_next(request)
    return response

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 모든 오리진 허용 (프로덕션에서는 특정 오리진만 허용하는 것이 좋음)
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
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
        logger.info(f"모델 로딩 중: {model_choice}...")
        CURRENT_MODEL = Zonos.from_pretrained(model_choice, device=device)
        CURRENT_MODEL.requires_grad_(False).eval()  # 평가 모드로 설정하고 그래디언트 계산 비활성화
        CURRENT_MODEL_TYPE = model_choice
        logger.info(f"모델 로딩 완료: {model_choice}!")
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

def decode_audio_bytes(audio_bytes: bytes):
    """
    바이트로 인코딩된 오디오 데이터를 디코딩하는 함수
    """
    try:
        # 임시 파일에 오디오 데이터 저장
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as temp_file:
            temp_file.write(audio_bytes)
            temp_file_path = temp_file.name
        
        try:
            # 오디오 파일 로드
            wav, sr = torchaudio.load(temp_file_path)
            return wav, sr
        finally:
            # 임시 파일 삭제
            os.unlink(temp_file_path)
    except Exception as e:
        raise ValueError(f"바이트 디코딩 중 오류 발생: {str(e)}")

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

def get_audio_bytes(audio_array: np.ndarray, sample_rate: int):
    """
    오디오 배열을 바이트로 변환하는 함수 (WAV 포맷)
    
    Args:
        audio_array: 오디오 배열
        sample_rate: 샘플 레이트
        
    Returns:
        바이트로 된 오디오 데이터
    """
    buffer = BytesIO()
    
    # 오디오 배열이 1D인지 확인하고 필요한 경우 차원 조정
    if len(audio_array.shape) == 1:
        audio_tensor = torch.tensor(audio_array).unsqueeze(0)
    else:
        audio_tensor = torch.tensor(audio_array)
    
    # WAV 포맷으로 버퍼에 저장
    torchaudio.save(buffer, audio_tensor, sample_rate, format="wav")
    buffer.seek(0)
    
    return buffer.read()

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
        
        # if request.speaker_audio_path:
        #     # 파일 경로에서 화자 임베딩 생성
        #     wav, sr = load_audio_from_path(request.speaker_audio_path)
        #     speaker_embedding = model.make_speaker_embedding(wav, sr)
        #     speaker_embedding = speaker_embedding.to(device, dtype=torch.bfloat16)
        # elif request.speaker_audio_bytes:

        # if request.speaker_audio_bytes:
        #     # 바이트로 인코딩된 오디오에서 화자 임베딩 생성
        #     try:
        #         wav, sr = decode_audio_bytes(request.speaker_audio_bytes)
        #         speaker_embedding = model.make_speaker_embedding(wav, sr)
        #         speaker_embedding = speaker_embedding.to(device, dtype=torch.bfloat16)
        #     except Exception as e:
        #         print(f"바이너리 디코딩 중 오류 발생: {str(e)}")
        #         # 오류가 발생해도 계속 진행 (speaker_embedding은 None으로 유지)
        # elif request.speaker_id and request.speaker_id in SPEAKER_EMBEDDING_CACHE:
        #     # 캐시된 화자 임베딩 사용
        #     speaker_embedding = SPEAKER_EMBEDDING_CACHE[request.speaker_id]

        if request.speaker_tensor:
            speaker_embedding = torch.tensor(request.speaker_tensor, dtype=torch.float32)
            speaker_embedding = speaker_embedding.to(device, dtype=torch.bfloat16)
        
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
        # GPU 텐서를 CPU 메모리에 기반한 처리를 위해서 텐서를 CPU로 옮김
        wav_out = model.autoencoder.decode(codes).cpu().detach()
        sr_out = model.autoencoder.sampling_rate
        if wav_out.dim() == 2 and wav_out.size(0) > 1:
            wav_out = wav_out[0:1, :]
        
        # 현재 스크립트 경로 기준으로 output 폴더 생성
        script_dir = os.path.dirname(os.path.abspath(__file__))
        output_dir = os.path.join(script_dir, "output")  # 현재 폴더 내의 output 디렉토리 사용
        os.makedirs(output_dir, exist_ok=True)
        
        # 오디오를 바이트로 인코딩
        audio_bytes = get_audio_bytes(wav_out.squeeze().numpy(), sr_out)
        print("\n\n\naudio_bytes:")
        print(audio_bytes)

        # content_type = response.headers.get("Content-Type", f"audio/{request.output_format}")
        output_format = "mp3"
        content_type = "audio/mp3"
        
        # 파일명 생성 (시간 기반)
        kst = pytz.timezone('Asia/Seoul')
        timestamp = datetime.now(kst).strftime("%Y%m%d_%H%M%S")

        formatted_script_id = f"{request.script_id:08d}"  # 8자리 (예: 00000001)
        formatted_scene_id = f"{request.scene_id:04d}"  # 4자리 (예: 0001)
        formatted_audio_id = f"{request.audio_id:04d}"  # 4자리 (예: 0001)
        object_name = f"{formatted_script_id}/audios/{formatted_scene_id}_{formatted_audio_id}_{timestamp}.{output_format}"
        local_file_name = f"output/{formatted_script_id}/audios/{formatted_scene_id}_{formatted_audio_id}_{timestamp}.{output_format}"
        
        # 1. 로컬 경로에 오디오 저장
        logger.info(f"오디오 파일 저장 중: {local_file_name}")
        try:
            save_audio_to_file(wav_out.squeeze().numpy(), sr_out, local_file_name)
            logger.info(f"오디오 파일 저장 완료: {local_file_name}")
            
            # 파일이 실제로 존재하는지 확인
            if not os.path.exists(local_file_name):
                logger.info(f"경고: 파일이 저장되지 않았습니다: {local_file_name}")
        except Exception as e:
            logger.info(f"오디오 파일 저장 중 오류 발생: {str(e)}")
            # 오류가 발생해도 계속 진행
        
        # 메모리 정리를 위한 백그라운드 태스크
        background_tasks.add_task(torch.cuda.empty_cache)
        
        # 상대 경로로 변환하여 반환
        relative_path = os.path.relpath(object_name, script_dir)

        # 2. S3에 직접 업로드
        logger.info(f"오디오 데이터를 S3에 업로드 중: {len(audio_bytes)} 바이트")
        upload_result = upload_binary_to_s3(audio_bytes, object_name, content_type)
        
        if not upload_result["success"]:
            raise ValueError(f"S3 업로드 실패: {upload_result.get('error')}")
        
        logger.info(f"S3 업로드 완료: {upload_result['url']}")
        
        return TTSResponse(
            s3_url = upload_result["url"],
            audio_path=relative_path,
            sample_rate=sr_out,
            seed=seed
        )
    
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"TTS 처리 중 오류 발생: {str(e)}")

@app.post("/zonos/base64_to_tensor", response_model=RegisterSpeakerResponse)
async def base64_to_tensor(request: RegisterSpeakerRequest, background_tasks: BackgroundTasks):
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

        if request.speaker_audio_base64:
            #base64로 byte 데이터를 만드는 로직

            base64_str = request.speaker_audio_base64
            if ',' in base64_str:
                base64_str = base64_str.split(',')[1]  # "data:audio/mp3;base64,..." 제거

            audio_bytes = base64.b64decode(base64_str)
            # 바이트로 인코딩된 오디오에서 화자 임베딩 생성
            wav, sr = decode_audio_bytes(audio_bytes)
            speaker_embedding = CURRENT_MODEL.make_speaker_embedding(wav, sr)
        else:
            raise ValueError("화자 오디오 파일 경로 또는 오디오 데이터가 필요합니다.")
        
        # float로 바꾸기
        speaker_embedding_3d = speaker_embedding.cpu().float().tolist()

        # 2. 예시 mp3 만들기
        # 샘플링 파라미터 추출
        sampling_params = {
            "top_p": 0.0,
            "top_k": 0,
            "min_p": 0.0,
            "linear": 0.5,
            "conf": 0.4,
            "quad": 0.0
        }
        # 시드 설정정
        seed = torch.randint(0, 2**32 - 1, (1,)).item()
        torch.manual_seed(seed)

        # 감정 텐서 생성
        emotion_tensor = torch.tensor([0.1, 0, 0, 0, 0, 0, 0, 1.0], device=device)
        
        # VQ 점수 텐서 생성
        vq_tensor = torch.tensor([0.78] * 8, device=device).unsqueeze(0)

        # 입력 파라미터 타입 변환 및 검증
        speaker_noised_bool = bool(False)
        fmax = float(22050)
        pitch_std = float(45.0)
        speaking_rate = float(20.0)
        cfg_scale = float(2.0)

        speaker_embedding = speaker_embedding.to(device, dtype=torch.bfloat16)

        # 조건부 딕셔너리 생성
        cond_dict = make_cond_dict(
            text="등록한 목소리로 생성한 샘플 음성입니다.",
            language="ko",
            speaker=speaker_embedding,
            emotion=emotion_tensor,
            vqscore_8=vq_tensor,
            fmax=fmax,
            pitch_std=pitch_std,
            speaking_rate=speaking_rate,
            speaker_noised=speaker_noised_bool,
            device=device,
            unconditional_keys=[],
        )
        conditioning = CURRENT_MODEL.prepare_conditioning(cond_dict)

        # 오디오 생성
        max_new_tokens = 86 * 30  # 약 30초 분량의 오디오 생성 (86 토큰/초)

        # 생성 시간 및 단계 추정
        estimated_generation_duration = 30 * len("등록한 목소리로 생성한 샘플 음성입니다.") / 400
        estimated_total_steps = int(estimated_generation_duration * 86)

        # 진행 상태 업데이트 콜백 함수 (로깅용)
        def update_progress(_frame: torch.Tensor, step: int, _total_steps: int) -> bool:
            if step % 10 == 0:  # 10단계마다 로그 출력
                print(f"생성 진행 중: {step}/{estimated_total_steps} ({step/estimated_total_steps*100:.1f}%)")
            return True
        
        codes = CURRENT_MODEL.generate(
            prefix_conditioning=conditioning,
            audio_prefix_codes=None,
            max_new_tokens=max_new_tokens,
            cfg_scale=cfg_scale,
            batch_size=1,
            sampling_params=sampling_params,
            callback=update_progress,
        )

        # GPU 텐서를 CPU 메모리에 기반한 처리를 위해서 텐서를 CPU로 옮김
        wav_out = CURRENT_MODEL.autoencoder.decode(codes).cpu().detach()
        sr_out = CURRENT_MODEL.autoencoder.sampling_rate
        if wav_out.dim() == 2 and wav_out.size(0) > 1:
            wav_out = wav_out[0:1, :]
        
        # 현재 스크립트 경로 기준으로 output 폴더 생성
        script_dir = os.path.dirname(os.path.abspath(__file__))
        output_dir = os.path.join(script_dir, "output/speaker")  # 현재 폴더 내의 output 디렉토리 사용
        os.makedirs(output_dir, exist_ok=True)
        
        # 오디오를 바이트로 인코딩
        audio_bytes = get_audio_bytes(wav_out.squeeze().numpy(), sr_out)
        print("\n\n\naudio_bytes:")
        print(audio_bytes)

        # content_type = response.headers.get("Content-Type", f"audio/{request.output_format}")
        output_format = "mp3"
        content_type = "audio/mp3"
        
        # 파일명 생성 (시간 기반)
        kst = pytz.timezone('Asia/Seoul')
        timestamp = datetime.now(kst).strftime("%Y%m%d_%H%M%S")
        random_filename = str(uuid.uuid4())

        object_name = f"speaker/{timestamp}_{random_filename}.{output_format}"
        local_file_name = f"output/speaker/{timestamp}_{random_filename}.{output_format}"
        
        # 로컬 경로에 오디오 저장
        logger.info(f"오디오 파일 저장 중: {local_file_name}")
        try:
            save_audio_to_file(wav_out.squeeze().numpy(), sr_out, local_file_name)
            logger.info(f"오디오 파일 저장 완료: {local_file_name}")
            
            # 파일이 실제로 존재하는지 확인
            if not os.path.exists(local_file_name):
                logger.info(f"경고: 파일이 저장되지 않았습니다: {local_file_name}")
        except Exception as e:
            logger.info(f"오디오 파일 저장 중 오류 발생: {str(e)}")
            # 오류가 발생해도 계속 진행
        
        # 메모리 정리를 위한 백그라운드 태스크
        background_tasks.add_task(torch.cuda.empty_cache)

        # 3. 예시 mp3를 S3에 업로드 한 후 URL 반환
        logger.info(f"오디오 데이터를 S3에 업로드 중: {len(audio_bytes)} 바이트")
        upload_result = upload_binary_to_s3(audio_bytes, object_name, content_type)
        
        if not upload_result["success"]:
            raise ValueError(f"S3 업로드 실패: {upload_result.get('error')}")
        
        logger.info(f"S3 업로드 완료: {upload_result['url']}")
        
        return RegisterSpeakerResponse(
            s3_url = upload_result["url"],
            speaker_tensor=speaker_embedding_3d
        )
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"화자 등록 중 오류 발생: {str(e)}")

@app.get("/supported_languages")
async def get_supported_languages():
    """
    지원되는 언어 코드 목록을 반환하는 API 엔드포인트
    """
    return {"supported_languages": supported_language_codes}

# @app.get("/supported_models")
# async def get_supported_models():
#     """
#     지원되는 모델 목록을 반환하는 API 엔드포인트
#     """
#     supported_models = []
#     if "transformer" in ZonosBackbone.supported_architectures:
#         supported_models.append("Zyphra/Zonos-v0.1-transformer")

#     if "hybrid" in ZonosBackbone.supported_architectures:
#         supported_models.append("Zyphra/Zonos-v0.1-hybrid")
    
#     return {"supported_models": supported_models}

# @app.get("/registered_speakers")
# async def get_registered_speakers():
#     """
#     등록된 화자 목록을 반환하는 API 엔드포인트
#     """
#     return {"registered_speakers": list(SPEAKER_EMBEDDING_CACHE.keys())}

# @app.get("/health")
# async def health_check():
#     """
#     서버 상태 확인을 위한 API 엔드포인트
#     """
#     return {"status": "healthy", "model_loaded": CURRENT_MODEL_TYPE}

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
        "main:app", 
        host=args.host, 
        port=args.port, 
        reload=args.reload
    ) 