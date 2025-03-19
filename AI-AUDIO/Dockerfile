FROM pytorch/pytorch:2.6.0-cuda12.4-cudnn9-devel
RUN pip install uv

RUN apt update && \
    apt install -y espeak-ng && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 종속성 파일만 먼저 복사하여 캐시 활용
COPY pyproject.toml ./
RUN uv pip install --system -e . && uv pip install --system -e .[compile]

# 포트 노출
EXPOSE 8000

# 소스 코드 복사 (자주 변경되는 부분을 마지막에 배치)
COPY . ./

# 기본 실행 명령어 설정 (FastAPI/main.py로 변경)
CMD ["python", "FastAPI/main.py", "--host", "0.0.0.0", "--port", "8000"]
