# CUDA 12.4.1 + cuDNN 8.9.7 (런타임 환경)
FROM nvidia/cuda:12.4.0-base-ubuntu22.04

# 또는 개발 환경이 필요한 경우 (런타임 + 개발 도구)
# FROM nvidia/cuda:12.4.1-cudnn8-devel-ubuntu22.04

# cuDNN 9 버전이 필요한 경우
# FROM nvidia/cuda:12.4.1-cudnn9-runtime-ubuntu22.04

# 작업 디렉토리 설정
WORKDIR /app

# Python 및 필수 패키지 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    software-properties-common \
    && add-apt-repository -y ppa:deadsnakes/ppa \
    && DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC apt-get install -y \
        python3.12 \
        python3.12-dev \
        python3.12-venv \
    && rm -rf /var/lib/apt/lists/*

# Python 기본 버전 설정 (3.12로 변경)
RUN update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.12 1 \
    && update-alternatives --install /usr/bin/python python /usr/bin/python3.12 1

# ensurepip를 통한 pip 설치
RUN python3.12 -m ensurepip --upgrade

# pip 업그레이드 및 패키지 설치
RUN python3.12 -m pip install --upgrade pip setuptools wheel --no-cache-dir
RUN python3.12 -m pip install -r requirements.txt --no-cache-dir

# 애플리케이션 코드 복사
COPY . .

# FastAPI 애플리케이션 실행
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]