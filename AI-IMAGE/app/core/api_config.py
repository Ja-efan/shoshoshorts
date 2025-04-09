"""
API 서비스별 구성 설정 관리
"""

import os
import time
import jwt
from dotenv import load_dotenv

# 환경 변수 로드
load_dotenv()


def encode_jwt_token(ak, sk):
    """JWT 토큰 생성 함수"""
    headers = {"alg": "HS256", "typ": "JWT"}
    payload = {"iss": ak, "exp": int(time.time()) + 1800, "nbf": int(time.time()) - 5}
    return jwt.encode(payload, sk, headers=headers)

def parse_bool_env(value, default=False):
        if isinstance(value, str):
            return value.lower() in ('true', 't', 'yes', 'y', '1')
        return bool(value) if value is not None else default
class KlingAIConfig:
    """Kling AI API 관련 설정"""

    # API 엔드포인트 및 인증 설정
    ACCESS_KEY: str = os.getenv("KLING_ACCESS_KEY", "")
    SECRET_KEY: str = os.getenv("KLING_SECRET_KEY", "")
    API_URL: str = "https://api.klingai.com/v1/images/generations"
    TASK_URL: str = "https://api.klingai.com/v1/images/generations"

    # 모델 및 이미지 생성 설정
    MODEL_V1 = "kling-v1"  # 참조 이미지 사용하기 위해서는 kling-v1 사용
    MODEL_V1_5 = "kling-v1-5"
    ASPECT_RATIO = os.getenv("ASPECT_RATIO", "1:1")
    USE_REFERENCE_IMAGE = parse_bool_env(os.getenv("USE_REFERENCE_IMAGE"), False)  # default: False
    USE_PREVIOUS_SCENE_DATA = parse_bool_env(os.getenv("USE_PREVIOUS_SCENE_DATA"), True)  # default: True
    NUM_OF_IMAGES = int(os.getenv("NUM_OF_IMAGES", 1))
    IMAGE_REFERENCE = os.getenv("IMAGE_REFERENCE", "subject")  # "subject" or "face"
    IMAGE_FIDELITY = float(os.getenv("IMAGE_FIDELITY", 0.1))  # 참조 이미지 참조 정도 (0 ~ 1 소수)
    DEFAULT_IMAGE_STYLE = os.getenv("DEFAULT_IMAGE_STYLE", "DISNEY-PIXAR")
    MAX_ATTEMPTS = int(os.getenv("MAX_ATTEMPTS", 50))  # 최대 시도 횟수
    DELAY = int(os.getenv("DELAY", 3))  # 시도 간격 (초)


    # 응답 코드 매핑
    KLING_TO_HTTP = {
        # 200 OK
        0: 200,
        # 401 Unauthorized
        1000: 401,
        1001: 401,
        1002: 401,
        1003: 401,
        1004: 401,
        # 429 Too Many Requests
        1100: 429,
        1101: 429,
        1102: 429,
        1302: 429,
        1303: 429,
        1304: 429,
        # 403 Forbidden
        1103: 403,
        # 400 Bad Request
        1200: 400,
        1201: 400,
        1300: 400,
        1301: 400,
        # 404 Not Found
        1202: 404,
        1203: 404,
        # 500 Internal Server Error
        5000: 500,
        # 503 Service Unavailable
        5001: 503,
        # 504 Gateway Timeout
        5002: 504,
    }


class OpenAIConfig:
    """OpenAI API 관련 설정"""

    # API 인증 설정
    API_KEY: str = os.getenv("OPENAI_API_KEY", "")

    # 이미지 프롬프트 설정 
    IMAGE_PROMPT_GENERATION_MODEL = os.getenv("IMAGE_PROMPT_GENERATION_MODEL", "gpt-4o")
    IMAGE_PROMPT_MAX_TOKENS = int(os.getenv("IMAGE_PROMPT_MAX_TOKENS", 500))
    IMAGE_PROMPT_TEMPERATURE = float(os.getenv("IMAGE_PROMPT_TEMPERATURE", 0.3))
    
    # 장면 정보 설정 
    SCENE_INFO_GENERATION_MODEL = os.getenv("SCENE_INFO_GENERATION_MODEL", "gpt-4o-mini")
    SCENE_INFO_MAX_TOKENS = int(os.getenv("SCENE_INFO_MAX_TOKENS", 500))
    SCENE_INFO_TEMPERATURE = float(os.getenv("SCENE_INFO_TEMPERATURE", 0.3))

    # Prompt 파일 경로
    PROMPT_DIR: str = "app/prompts"
    SYSTEM_PROMPT_DIR: str = os.path.join(PROMPT_DIR, "system-prompts")
    
    # 스타일별 시스템 프롬프트 및 참고 이미지 설정
    IMAGE_STYLES = {
        "disney": {
            "prompt": os.path.join(
                SYSTEM_PROMPT_DIR, "image_prompts", os.getenv("DISNEY_STYLE_PROMPT", "disney/disney_v01.txt")
            ),
            "reference_image": os.getenv("DISNEY_STYLE_REFERENCE_IMAGE", "disney/disney-reference.png")
        },
        "pixar": {
            "prompt": os.path.join(
                SYSTEM_PROMPT_DIR, "image_prompts", os.getenv("PIXAR_STYLE_PROMPT", "pixar/pixar_v01.txt")
            ),
            "reference_image": os.getenv("PIXAR_STYLE_REFERENCE_IMAGE", "pixar/pixar-reference.png")
        },
        "illustrate": {
            "prompt": os.path.join(
                SYSTEM_PROMPT_DIR, "image_prompts", os.getenv("ILLUSTRATE_STYLE_PROMPT", "illustrate/Illustrate_v01.txt")
            ),
            "reference_image": os.getenv("ILLUSTRATE_STYLE_REFERENCE_IMAGE", "illustrate/illustrate-reference.png")
        }
    }
    
    # 기본 시스템 프롬프트 설정 (스타일별 설정이 없는 경우 사용)
    SYSTEM_PROMPT = {
        "image_prompt": os.path.join(
            SYSTEM_PROMPT_DIR, "image_prompts", os.getenv("SYSTEM_PROMPT_FOR_IMAGE_PROMPT")
        ),
        "scene_info": os.path.join(
            SYSTEM_PROMPT_DIR, "sceneinfo_prompts", os.getenv("SYSTEM_PROMPT_FOR_SCENE_INFO")
        ),
    }


# 설정 인스턴스 생성
klingai_config = KlingAIConfig()
openai_config = OpenAIConfig()

# JWT 토큰 생성
klingai_config.JWT_TOKEN = encode_jwt_token(
    klingai_config.ACCESS_KEY, klingai_config.SECRET_KEY
)
