## 1. Fast API server + Zonos 도커 이미지 빌드 및 실행 방법

### 1.1. 도커 이미지 빌드

```bash
# 프로젝트 루트 디렉토리에서 실행
docker build -t zonos-tts-api .
```

### 1.2. 도커 컨테이너 실행

### GPU 사용 (권장)

```bash
# Windows (Git Bash)
docker run -it --gpus all -p 8000:8000 -v "C:/Users/SSAFY/Desktop/Zonos":/app zonos-tts-api

# Linux/macOS
docker run -it --gpus all -p 8000:8000 -v $(pwd):/app zonos-tts-api
```

<!-- ```
# gradio_interface.py 실행 (기존 방식)
docker run -it --gpus all -p 7860:7860 -v "C:/Users/SSAFY/Desktop/Zonos":/app zonos-tts-api python gradio_interface.py

# main.py 실행 (새로운 API 서버)
docker run -it --gpus all -p 8000:8000 -v "C:/Users/SSAFY/Desktop/Zonos":/app zonos-tts-api
``` -->

#### 환경 변수 설정하면서 실행하고 싶은 경우

```
# 특정 모델 사용
docker run -it --gpus all -p 8000:8000 -e ZONOS_DEFAULT_MODEL="Zyphra/Zonos-v0.1-hybrid" -v "C:/Users/SSAFY/Desktop/Zonos":/app zonos-tts-api
```

## 2. 실행 이후 확인

### 2.1. API 서버 (main.py) 확인

```
http://localhost:8000/docs
```

위 URL에 접속하면 Swagger UI를 통해 API 문서를 확인하고 테스트할 수 있음.

## 3. 도커 컴포즈 실행

```
# 모든 서비스 실행
docker-compose up

# 백그라운드에서 실행
docker-compose up -d

# 특정 서비스만 실행 (현재 gradio는 제거함.)
docker-compose up zonos-api
docker-compose up zonos-gradio
```

로그 확인 방법

```
# 컨테이너 로그 확인
docker logs <container_id>

# 실시간 로그 확인
docker logs -f <container_id>
```

## 4. post 양식 api

```
{
  "model_choice": "Zyphra/Zonos-v0.1-transformer",
  "text": "안녕하세요, Zonos TTS 모델입니다.",
  "language": "ko",
  "speaker_audio_path": "/path/to/speaker.wav",
  "speaker_id": "string",
  "emotion": [0.8, 0.05, 0.05, 0.05, 0.05, 0.05, 0.1, 0.2],
  "vq_score": 0.78,
  "fmax": 24000,
  "pitch_std": 45,
  "speaking_rate": 15,
  "dnsmos_ovrl": 4,
  "speaker_noised": false,
  "cfg_scale": 2,
  "sampling_params": {
    "top_p": 0,
    "top_k": 0,
    "min_p": 0,
    "linear": 0.5,
    "conf": 0.4,
    "quad": 0
  },
  "seed": 42,
  "randomize_seed": false,
  "unconditional_keys": ["emotion"]
}
```

## 5. 반환 양식

현재 base64 양식으로 반환되고 로컬에는 값이 저장이 안됨 <<< 리눅스 환경경에 저장되고 있나본데

base64는 https://base64.guru/converter/decode/audio 로 변환해서 확인 가능능
