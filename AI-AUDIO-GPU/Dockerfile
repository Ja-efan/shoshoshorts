FROM pytorch/pytorch:2.6.0-cuda12.4-cudnn9-devel
RUN pip install uv

RUN apt update && \
    apt install -y espeak-ng && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . ./

RUN uv pip install --system -e . && uv pip install --system -e .[compile]

# 포트 노출
EXPOSE 8000

# 기본 실행 명령어 설정 (gradio_interface.py 또는 main.py 중 선택 가능)
CMD ["python", "main.py", "--host", "0.0.0.0", "--port", "8000"]
