from pydantic import BaseModel, Field
from typing import Optional

# 요청 모델 정의
class ElevenLabsTTSRequest(BaseModel):
    text: str = Field(description="변환할 텍스트")
    #남자 목소리: 4JJwo477JUAx3HV0T7n7
    # PLfpgtLkFW07fDYbUiRJ
    # v1jVu1Ky28piIPEJqRrm
    # WqVy7827vjE2r3jWvbnP
    #여자 목소리: uyVNoMrnUku1dZyVEXwD
    # xi3rF0t7dg7uN2M0WUhr
    # z6Kj0hecH20CdetSElRT
    # DMkRitQrfpiddSQT5adl
    voice_code: Optional[str] = Field(default="uyVNoMrnUku1dZyVEXwD", description="사용할 음성 ID")
    model_id: str = Field(default="eleven_multilingual_v2", description="사용할 모델 ID")
    output_format: str = Field(
        default="mp3", 
        description="출력 포맷 (mp3, pcm, wav, ogg, flac)"
    )
    script_id: int = Field(default=None, description="스크립트 ID")
    scene_id: int = Field(default=None, description="씬 번호")
    audio_id: int = Field(default=None, description="오디오 번호")


# 응답 모델 정의
class ElevenLabsTTSResponse(BaseModel):
    s3_url: str = Field(description="S3에 업로드된 오디오 파일 URL")
    content_type: str = Field(description="오디오 콘텐츠 타입")
    file_size: int = Field(description="오디오 파일 크기 (바이트)")