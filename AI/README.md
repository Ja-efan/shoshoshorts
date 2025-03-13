## 1. Fast API server + AI 도커 이미지 빌드 및 실행 방법

### 1.1. 도커 이미지 빌드

```bash
# 프로젝트 루트 디렉토리에서 실행
docker build -t ai-api .
docker-compose up --build ai-api
```

### 1.2. 도커 컨테이너 실행

### GPU 사용 (권장)

```bash
# Windows (Git Bash)
docker run -it --gpus all -p 8000:8000 -v "C:/Users/SSAFY/Desktop/S12P21B106/AI":/app ai-api

# Linux/macOS
docker run -it --gpus all -p 8000:8000 -v $(pwd):/app ai-api
```

#### 환경 변수 설정하면서 실행하고 싶은 경우

```
# 특정 모델 사용
docker run -it --gpus all -p 8000:8000 -e ZONOS_DEFAULT_MODEL="Zyphra/Zonos-v0.1-hybrid" -v "C:/Users/SSAFY/Desktop/S12P21B106/AI":/app ai-api
```

## 2. 실행 이후 확인

### 2.1. API 서버 확인

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

# 특정 서비스만 실행
docker-compose up ai-api
```

로그 확인 방법

```
# 컨테이너 로그 확인
docker logs <container_id>

# 실시간 로그 확인
docker logs -f <container_id>
```

## 4. 지원하는 API 엔드포인트

### 4.1. Zonos TTS API

```
POST /zonos/tts
```

요청 예시:

```json
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

### 4.2. ElevenLabs TTS API

```
POST /elevenlabs/tts
```

요청 예시:

```json
{
  "text": "안녕하세요, ElevenLabs TTS API입니다.",
  "voice_id": "your_voice_id",
  "model_id": "eleven_multilingual_v2",
  "output_format": "mp3"
}
```

```
GET /elevenlabs/voices
```

### 4.3. 스크립트 변환 API

```
POST /script/convert
```

## 5. 환경 변수 설정

`.env` 파일을 프로젝트 루트에 생성하여 다음 환경 변수를 설정할 수 있습니다:

```
OPENAI_API_KEY=your_openai_api_key
ELEVENLABS_API_KEY=your_elevenlabs_api_key
ZONOS_DEFAULT_MODEL=Zyphra/Zonos-v0.1-transformer
```
