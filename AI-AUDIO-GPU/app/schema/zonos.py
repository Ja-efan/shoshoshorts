from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any, Union

# 요청 모델 정의
class TTSRequest(BaseModel):
    model_choice: str = Field(
        default="Zyphra/Zonos-v0.1-transformer", 
        description="사용할 모델 타입 (transformer 또는 hybrid)"
    )
    text: str = Field(
        default="안녕하세요, 기본 음성입니다.", 
        description="합성할 텍스트"
    )
    language: str = Field(
        default="ko", 
        description="언어 코드"
    )
    speaker_audio_s3_url: Optional[str] = Field(
        default=None, 
        description="화자 오디오 파일 S3 URL (선택 사항)"
    )
    speaker_audio_path: Optional[str] = Field(
        default=None, 
        description="화자 오디오 파일 경로 (선택 사항)"
    )
    speaker_audio_bytes: Optional[bytes] = Field(
        default=None,
        description="바이트로 인코딩된 화자 오디오 데이터 (선택 사항)"
    )
    speaker_id: Optional[str] = Field(
        default=None, 
        description="이전에 등록된 화자 ID (speaker_audio_path가 없을 경우 사용)"
    )
    script_id: int = Field(default=None, description="스크립트 ID")
    scene_id: int = Field(default=None, description="씬 번호")
    audio_id: int = Field(default=None, description="오디오 번호")
    emotion: List[float] = Field(
        default=[0.1, 0, 0, 0, 0, 0, 0, 1.0], 
        description="8개의 감정 값 (행복, 슬픔, 혐오, 두려움, 놀람, 분노, 기타, 중립)"
    )
    vq_score: float = Field(
        default=0.78, 
        description="VQ 점수 (0.5-0.8)"
    )
    fmax: float = Field(
        default=22050, 
        description="최대 주파수 (Hz)"
    )
    pitch_std: float = Field(
        default=45.0, 
        description="피치 표준 편차"
    )
    speaking_rate: float = Field(
        default=20.0, 
        description="말하기 속도"
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
        default=True, 
        description="시드 랜덤화 여부"
    )
    unconditional_keys: List[str] = Field(
        default=[], 
        description="비조건부 키 목록"
    )

# 응답 모델 정의
class TTSResponse(BaseModel):
    s3_url: str = Field(
        description="S3에 업로드된 오디오 파일 URL"
    )
    audio_path: str = Field(
        description="생성된 오디오 파일 경로"
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
    speaker_audio_bytes: Optional[bytes] = Field(
        default=None,
        description="바이트로 인코딩된 화자 오디오 데이터"
    )

# 화자 등록 응답 모델
class RegisterSpeakerResponse(BaseModel):
    speaker_id: str = Field(
        description="등록된 화자 ID"
    )
    message: str = Field(
        description="등록 결과 메시지"
    )