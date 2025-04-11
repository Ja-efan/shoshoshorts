# 쇼쇼숏(shoshoshorts) 포팅 매뉴얼

# 1. 개요

- 사용자가 입력한 스토리를 기반으로 AI를 활용하여 숏폼 콘텐츠를 제작하는 웹 플랫폼

# 2. 시스템 요구 사항

- Docker 및 Docker Compose 최신 버전
- Git 최신 버전

# 3. 기술 스택

- FrontEnd
    - React 18.3
        - TypeScript, TailwindCSS, Redux, Node.js 22.12.0, npm 10.9.0
- BackEnd
    - Java 21 + SpringBoot 3.4.0
        - Spring Data JPA, Spring Data MongoDB,
        - Gradle 8.5
    - Python 3.11 +
        - FastAPI
- AI
    - OpenAI API
    - TTS: Zonos, ElevenLabs
    - Image: KLING AI API
- 인증 및 보안
    - OAuth2.0 (Google, Kakao, Naver)
    - JWT (JSON Web Token)
    - YouTube Data API
- 멀티미디어 처리
    - FFmpeg, Thumbnailator
- Database
    - PostgreSQL, MongoDB, Redis, AWS S3,
- Infra
    - EC2, GCP, gitlab, Docker, Jenkins, Metabase, portainer,  sonarqube

# 4. 설치 및 배포 절차

Java Spring Boot와 ReactJS 가 동작하는 서버(EC2)와 음성 및 이미지 관련 Python FastAPI 가 동작하는 서버(GCP)를 구분하여 실행합니다.

순서대로 ‘메인 웹 서버’와 ‘AI 웹 서버’라고 부르겠습니다. 

## 4.1 디렉토리 구

기본적으로 메인 웹 서버는 EC2 인스턴스의 Jenkins 로 CICD 되고 있습니다. 

```python
## 📂 프로젝트 구조
```
📦 프로젝트 루트
├── 📂 FE                   # 프론트엔드 애플리케이션
│   ├── 📂 src              # 소스 코드
│   └── 📜 Dockerfile       # 프론트엔드 도커 이미지 설정
│
├── 📂 BE                   # 백엔드 애플리케이션
│   ├── 📂 src              # 소스 코드
│   └── 📜 Dockerfile              # 백엔드 도커 이미지 설정
│
├── 📂 AI                   # AI 관련 코드
│
├── 📂 jenkins              # Jenkins CI/CD 설정
│   └── 📜 docker-compose.yml # Jenkins 컨테이너 설정
│
├── 📜 docker-compose.yml     # 배포용 컴포즈 파일
├── 📜 docker-compose.dev.yml # 개발용 컴포즈 파일
├── 📜 .env.example           # 환경 변수 예제 파일
├── 📜 Jenkinsfile            # CI/CD 파이프라인 설정
├── 📜 run-backend.sh         # 백엔드 실행 스크립트
├── 📜 run-frontend.sh        # 프론트엔드 실행 스크립트
├── 📜 run-backend-dev.sh     # 백엔드 개발 환경 실행 스크립트
├── 📜 run-frontend-dev.sh    # 프론트엔드 개발 환경 실행 스크립트
└── 📜 stop-containers.sh     # 컨테이너 중지 및 삭제 스크립트

Image-Generation/
├── app/
│   ├── api/
│   │   └── routes/       # API 라우터
│   ├── core/             # 설정 및 유틸리티
│   ├── schemas/          # 데이터 모델
│   └── services/         # 비즈니스 로직
├── images/               # 로컬 이미지 저장소
├── static/               # 정적 파일
├── .env                  # 환경 변수
├── main.py               # 애플리케이션 진입점
└── requirements.txt      # 의존성 목록

AUDIO-Generation/
├── app/
│   ├── __init__.py/
│   ├── routes/           # API 라우터
│   ├── schemas/          # 데이터 모델
│   └── services/         # 비즈니스 로직
├── main.py               # 애플리케이션 진입점
└── requirements.txt      # 의존성 목록

AUDIO-GPU-Generation/
├── app/
│   ├── api/
│   │   └── routes/       # API 라우터
│   ├── core/             # 설정 및 유틸리티
│   ├── schemas/          # 데이터 모델
│   └── services/         # 비즈니스 로직
│── zonos/
│   │   └── backbone/     # Backbone
│   ├── autoencoder.py    # 설정 및 유틸리티
│   ├── model.py          # 데이터 모델
│   ├── config.py         # 비즈니스 로직
│   └── speakerclonging.py # 보이스클로닝 로직
└── main.py               # 애플리케이션 진입점

```

## How to run - Web

```python
# README.prod.md

# 🚀 쇼쇼숏 - 배포 환경 설정 가이드

## 📌 프로젝트 개요
사용자가 입력한 스토리를 기반으로 다양한 AI를 활용하여 숏폼 콘텐츠를 제작하는 웹 플랫폼입니다.

## 🐳 배포 환경 설정

### 사전 요구사항
- Docker 및 Docker Compose 설치
- Git

### 환경 설정
1. 프로젝트 클론
```bash
git clone https://lab.ssafy.com/s12-ai-speech-sub1/S12P21B106.git
cd S12P21B106
```

2. 환경 변수 파일 생성
```bash
cp .env.example .env
```

3. `.env` 파일을 열고 필요한 환경 변수 값을 설정합니다.

### 실행 권한 부여
스크립트를 실행하기 전에 실행 권한을 부여해야 합니다.
```bash
chmod +x build-images.sh run-backend.sh run-frontend.sh stop-containers.sh
```

## 🏗 배포 환경 설정 순서

### 1. Docker 이미지 빌드
```bash
# 대화형 모드로 실행
./build-images.sh

# 또는 배포에 필요한 이미지만 빌드
./build-images.sh base prod
```

### 2. Docker Compose를 사용한 전체 서비스 실행
```bash
docker compose up -d
```

### 3. 개별 서비스 실행 스크립트 사용 (선택사항)
```bash
# 백엔드 서비스 실행 (데이터베이스 포함)
./run-backend.sh

# 프론트엔드 서비스 실행
./run-frontend.sh
```

### 4. 서비스 중지 및 삭제
```bash
# 배포 환경 컨테이너만 중지 및 삭제
./stop-containers.sh prod

# 특정 서비스 중지 및 삭제 (예: 백엔드)
./stop-containers.sh backend

# 특정 서비스 중지 및 삭제 (예: 프론트엔드)
./stop-containers.sh frontend
```

## 📡 Jenkins CI/CD 설정

1. Jenkins 컨테이너 실행
```bash
cd jenkins
docker compose up -d
```

2. Jenkins 초기 설정
   - 브라우저에서 `http://localhost:8090` 접속
   - 초기 관리자 비밀번호 확인: `docker exec sss-jenkins cat /var/jenkins_home/secrets/initialAdminPassword`
   - 권장 플러그인 설치 및 관리자 계정 생성

3. Jenkins 파이프라인 설정
   - 새 파이프라인 작업 생성
   - SCM에서 Git 선택 및 저장소 URL 입력
   - Jenkinsfile 경로 지정: `Jenkinsfile`

## 📝 포트 정보
- 백엔드 API: http://localhost:8080
- 프론트엔드: http://localhost:80
- PostgreSQL: localhost:5432
- MongoDB: localhost:27017
- Jenkins: http://localhost:8090

## ✅ 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너 목록 확인
docker ps

# 컨테이너 로그 확인
docker logs sss-backend
docker logs sss-frontend
docker logs sss-postgres
docker logs sss-mongodb
docker logs sss-jenkins
```

## 🔄 시스템 의존성 관리

새로운 시스템 의존성 추가 방법:

1. 기본 Dockerfile 수정:
```bash
# 백엔드 의존성 추가
vi BE/Dockerfile.base
# 프론트엔드 의존성 추가
vi FE/Dockerfile.base
```

2. 이미지 재빌드:
```bash
./build-images.sh base prod
```

3. 컨테이너 재시작:
```bash
docker compose down
docker compose up -d
```

## ⚠️ 주의사항

- 동일한 포트를 사용하는 서비스가 이미 실행 중인 경우 포트 충돌이 발생할 수 있습니다.
- 개발 환경과 배포 환경을 동시에 실행할 경우 포트 충돌이 발생할 수 있으므로 주의하세요.
- 데이터베이스 데이터는 Docker 볼륨에 저장되므로, 볼륨을 삭제하면 데이터가 손실됩니다.
```

## How to Run-AI Image

```python
### 환경 설정

1. 저장소 클론
```bash
git clone [repository-url]
cd AI-IMAGE
```

2. 환경 변수 설정
`.env.example` 파일을 `.env`로 복사하고 필요한 환경 변수를 설정합니다:
```bash
cp .env.example .env
```

`.env` 파일에 다음 변수들을 설정합니다:
```
# KLING AI
KLING_ACCESS_KEY=your_kling_access_key
KLING_SECRET_KEY=your_kling_secret_key

# OPENAI
OPENAI_API_KEY=your_openai_api_key

# AWS S3
S3_ACCESS_KEY=your_s3_access_key
S3_SECRET_KEY=your_s3_secret_key
S3_BUCKET_NAME=your_s3_bucket_name
S3_REGION=ap-northeast-2

# 오류 처리 설정
USE_LOCAL_URL_ON_S3_FAILURE=false
```

### Docker를 사용한 실행

1. Docker 이미지 빌드 및 컨테이너 실행
```bash
docker compose up --build
```

2. 백그라운드에서 실행하려면
```bash
docker compose up -d --build
```

3. 컨테이너 중지
```bash
docker compose down
```

서버는 `http://localhost:8001`에서 실행됩니다.

```

## How to Run-AI AUDIO

```python
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

### CPU 환경에서 실행 (GPU가 없는 경우)

GPU가 없는 환경에서는 Dockerfile.cpu를 사용하여 CPU 전용 이미지를 빌드하고 실행할 수 있습니다.

```bash
# CPU 전용 이미지 빌드
docker build -f Dockerfile.cpu -t ai-api-cpu .

# CPU 전용 이미지 실행
docker run -p 8000:8000 ai-api-cpu
```

CPU 환경에서는 일부 GPU 의존적인 기능이 제한될 수 있습니다.

#### 환경 변수 설정하면서 실행하고 싶은 경우

```
# 특정 모델 사용 (GPU 환경)
docker run -it --gpus all -p 8000:8000 -e ZONOS_DEFAULT_MODEL="Zyphra/Zonos-v0.1-hybrid" -v "C:/Users/SSAFY/Desktop/S12P21B106/AI":/app ai-api

# 환경 변수 설정 (CPU 환경)
docker run -p 8000:8000 -e OPENAI_API_KEY=your_openai_api_key -e ELEVENLABS_API_KEY=your_elevenlabs_api_key ai-api-cpu
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

### 3.1. CPU 환경에서 도커 컴포즈 실행

CPU 환경에서 도커 컴포즈를 사용하려면 docker-compose.cpu.yml 파일을 생성하고 다음과 같이 실행할 수 있습니다:

```bash
# CPU 환경에서 실행
docker compose -f docker-compose.cpu.yml up
```

로그 확인 방법

```
# 컨테이너 로그 확인
docker logs <container_id>

# 실시간 로그 확인
docker logs -f <container_id>
```

```

### 빌드 시 사용되는 환경 변수 등의 내용 상세 기재

- `.env` 파일에서 관리
- React
    
    ```json
    VITE_GOOGLE_CLIENT_ID=your_google_client_id
    VITE_GOOGLE_CLIENT_SECRET=your_google_client_secret
    
    VITE_NAVER_CLIENT_ID=your_naver_client_id
    VITE_NAVER_CLIENT_SECRET=your_naver_client_secret
    
    VITE_KAKAO_CLIENT_ID=your_kakao_client_id
    
    VITE_API_BASE=/api
    ```
    
- SpringBoot
    
    ```python
    # 백엔드 설정
    BACKEND_PORT=8080  # 서버 환경 세팅에 맞춰 수정하세요
    
    # 프론트엔드 설정
    FRONTEND_PORT=80  # 서버 환경 세팅에 맞춰 수정하세요
    
    # Jenkins 설정
    JENKINS_PORT=8090  # 서버 환경 세팅에 맞춰 수정하세요
    
    # PostgreSQL 설정
    POSTGRES_DB=sss_pg_db
    POSTGRES_USER=postgres_user
    POSTGRES_PASSWORD=postgres_password
    POSTGRES_PORT=5432  # 서버 환경 세팅에 맞춰 수정하세요
    
    # MongoDB 설정
    MONGO_DB=sss_mg_db
    MONGO_USER=mongo_user
    MONGO_PASSWORD=mongo_password
    MONGO_PORT=27017  # 서버 환경 세팅에 맞춰 수정하세요
    
    # S3 설정
    AWS_ACCESS_KEY=your_aws_access_key
    AWS_SECRET_KEY=your_aws_secret_key
    AWS_REGION=your_aws_s3_region
    AWS_BUCKET=your_aws_bucket_name
    
    # 백엔드 환경변수
    FFMPEG_PATH=ffmpeg
    FFPROBE_PATH=ffprobe
    TEMP_DIRECTORY=/tmp/sss/videos
    API_PASSWORD=your_api_password
    SPRING_PROFILES_ACTIVE=prod
    FASTAPI_BASE_URL=your_fastapi_base_url  # http://{ip_address}
    
    # OAuth
    NAVER_CLIENT_ID=your_naver_client_id
    NAVER_CLIENT_SECRET=your_naver_client_secret
    GOOGLE_CLIENT_ID=your_google_client_id
    GOOGLE_CLIENT_SECRET=your_google_client_secret
    KAKAO_CLIENT_ID=your_kakao_client_id
    
    # JWT (변경하시오)
    JWT_SECRET_KEY=your_jwt_secret_key
    ```
    
- FastAPI
    
    ```python
    ######################################################## 
    # ENVIRONMENT CONFIG 
    ########################################################
    
    ENV=prod
    API_PWD=your_api_pwd  # SpringBoot .env의 API_PASSWORD와 동일해야 합니다.
    
    ######################################################## 
    # KLING AI CONFIG 
    ########################################################
    
    KLING_ACCESS_KEY=your_klingai_access_key
    KLING_SECRET_KEY=your_klingai_secret_key
    
    # KLINGAI IMAGE GENERATION CONFIG
    DEFAULT_IMAGE_STYLE="DISNEY-PIXAR"  # 기본 이미지 스타일
    NUM_OF_IMAGES=1  # 생성되는 이미지 개수
    ASPECT_RATIO=1:1  # 생성되는 이미지 비율
    
    # 이전 장면 데이터 사용 여부
    USE_PREVIOUS_SCENE_DATA=true
    
    # 참조 이미지 사용 여부
    USE_REFERENCE_IMAGE=true
    IMAGE_REFERENCE="subject"  # 이미지 참조 기준
    IMAGE_FIDELITY=0.1  # 이미지 참조 정도
    
    # 이미지 스타일별 설정
    # DISNEY 스타일 설정
    DISNEY_STYLE_PROMPT="disney/disney_v01.txt"
    DISNEY_STYLE_REFERENCE_IMAGE="disney/disney-reference.png"
    
    # PIXAR 스타일 설정
    PIXAR_STYLE_PROMPT="pixar/pixar_v01.txt"
    PIXAR_STYLE_REFERENCE_IMAGE="pixar/pixar-reference.png"
    
    # ILLUSTRATE 스타일 설정
    ILLUSTRATE_STYLE_PROMPT="illustrate/illustrate_v01.txt"
    ILLUSTRATE_STYLE_REFERENCE_IMAGE="illustrate/illustrate-reference.png"
    
    MAX_ATTEMPTS=50  # 최대 시도 횟수
    DELAY=3  # 시도 간격 (초)
    
    ######################################################## 
    # OPENAI CONFIG 
    ########################################################
    
    OPENAI_API_KEY=your_openai_api_key
    
    # OpenAI CONFIG - IMAGE PROMPT GENERATION
    IMAGE_PROMPT_GENERATION_MODEL=gpt-4o
    IMAGE_PROMPT_TEMPERATURE=0.3
    IMAGE_PROMPT_MAX_TOKENS=500
    # 기본 스타일 시스템 프롬프트 (스타일별 설정이 없을 경우 사용)
    SYSTEM_PROMPT_FOR_IMAGE_PROMPT="disney-pixar_style_v04.txt"
    
    # OpenAI CONFIG - SCENE INFO GENERATION
    SCENE_INFO_GENERATION_MODEL=gpt-4o-mini
    SCENE_INFO_TEMPERATURE=0.3
    SCENE_INFO_MAX_TOKENS=500
    SYSTEM_PROMPT_FOR_SCENE_INFO="sceneinfo_v04.txt"
    
    ######################################################## 
    # S3 CONFIG 
    ########################################################
    
    # 로컬 URL 사용 여부 (S3 업로드 실패 시 로컬 URL 사용)
    USE_LOCAL_URL_ON_S3_FAILURE=false
    
    # AWS S3 Dev Bucket
    RELEASE_S3_ACCESS_KEY=your_aws_access_key  # SpringBoot .env 의 AWS_ACCESS_KEY 와 동일해야 합니다.
    RELEASE_S3_SECRET_KEY=your_aws_secret_key # SpringBoot .env 의 AWS_SECRET_KEY 와 동일해야 합니다.
    RELEASE_S3_BUCKET_NAME=your_aws_s3_bucket_name  # SpringBoot .env 의 AWS_BUCKET와 동일해야 합니다.
    RELEASE_S3_REGION=your_aws_s3_region  # SpringBoot .env 의 AWS_REGION과 동일해야 합니다.
    
    # # AWS S3 DEV Bucket 개발 환경
    S3_ACCESS_KEY=your_dev_aws_access_key  
    S3_SECRET_KEY=your_dev_aws_access_key  
    S3_BUCKET_NAME=your_dev_aws_access_key  
    S3_REGION=your_dev_aws_access_key  
    ```
    
    ## Dockerfile & docker-compose.yml
    
    - Web : docker-compose.yml
    
    ```python
    services:
      frontend:                # ← 기존 nginx 서비스를 대체
        build:
          context: ./FE
          dockerfile: Dockerfile
        container_name: sss-frontend
        ports:
          - "80:80"
          - "443:443"
        volumes:
          - /etc/letsencrypt:/etc/letsencrypt:ro   # 인증서 읽기
        networks:
          - sss-network
        restart: unless-stopped
    
      backend:
        build:
          context: ./BE
          dockerfile: Dockerfile
        container_name: sss-backend
        ports:
          - "${BACKEND_PORT}:8080"
        env_file:
          - .env
        depends_on:
          - db
          - mongo
        networks:
          - sss-network
        restart: unless-stopped
    
      db:
        image: postgres:16-alpine
        container_name: sss-postgres
        ports:
          - "5432:5432"
        environment:
          - POSTGRES_DB=${POSTGRES_DB}
          - POSTGRES_USER=${POSTGRES_USER}
          - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
        volumes:
          - postgres-data:/var/lib/postgresql/data
        networks:
          - sss-network
        restart: unless-stopped
    
      mongo:
        image: mongo:7.0
        container_name: sss-mongo
        ports:
          - "27017:27017"
        environment:
          - MONGO_INITDB_DATABASE=${MONGO_DB}
          - MONGO_INITDB_ROOT_USERNAME=${MONGO_USER}
          - MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASSWORD}
        volumes:
          - mongo-data:/data/db
        networks:
          - sss-network
        restart: unless-stopped
    
      # Redis 서비스
      redis:
        image: redis:7.2-alpine
        container_name: sss-redis
        ports:
          - "6379:6379"
        volumes:
          - redis-data:/data
        networks:
          - sss-network
        restart: unless-stopped
    
    networks:
      sss-network:
        driver: bridge
    
    volumes:
      postgres-data:
      mongo-data:
      redis-data:
    ```
    
- Spring docker
    
    ```python
    # 빌드 스테이지
    FROM sss-backend-base:latest AS build
    WORKDIR /app
    
    # 그래들 파일 복사
    COPY build.gradle settings.gradle ./
    COPY gradle ./gradle
    
    # 의존성 다운로드
    RUN gradle dependencies --no-daemon
    
    # 소스 코드 복사
    COPY src ./src
    
    # 애플리케이션 빌드
    RUN gradle build --no-daemon -x test
    
    # 실행 스테이지
    FROM eclipse-temurin:21-jre-jammy
    WORKDIR /app
    
    # ffmpeg 및 한글 폰트 설치
    RUN apt-get update && \
        apt-get install -y --no-install-recommends \
        ffmpeg \
        fontconfig \
        wget \
        && rm -rf /var/lib/apt/lists/*
    
    # 나눔 고딕 폰트만 직접 다운로드 및 설치
    RUN mkdir -p /usr/share/fonts/truetype/nanum && \
        wget "https://github.com/google/fonts/raw/main/ofl/nanumgothic/NanumGothic-Regular.ttf" -O /usr/share/fonts/truetype/nanum/NanumGothic-Regular.ttf && \
        wget "https://github.com/google/fonts/raw/main/ofl/nanumgothic/NanumGothic-Bold.ttf" -O /usr/share/fonts/truetype/nanum/NanumGothic-Bold.ttf && \
        chmod 644 /usr/share/fonts/truetype/nanum/*.ttf
    
    # 폰트 캐시 갱신 및 확인
    RUN fc-cache -fv && fc-list | grep -i nanum
    
    WORKDIR /app
    
    # 빌드 스테이지에서 생성된 JAR 파일 복사
    COPY --from=build /app/build/libs/*.jar app.jar
    
    # 포트 노출
    EXPOSE 8080
    
    # 애플리케이션 실행
    ENTRYPOINT ["java", "-jar", "app.jar"] 
    ```
    
- React docker
    
    ```python
    # ---------- 1단계: build ----------
        FROM node:22-alpine AS build
    
        WORKDIR /app
        
        # 의존성 설치
        COPY package*.json ./
        RUN npm ci
        
        # 소스 + 환경변수
        COPY . .
        COPY .env.production .
        
        # Vite 빌드 (production mode)
        RUN npm run build
        
        # ---------- 2단계: nginx ----------
        FROM nginx:1.25-alpine
        
        # 빌드 산출물 복사
        COPY --from=build /app/dist /usr/share/nginx/html
        
        # 필요하면 커스텀 nginx conf 복사
        COPY nginx/default.conf /etc/nginx/conf.d/default.conf
        
        # 인증서 디렉터리 마운트용
        VOLUME ["/etc/letsencrypt"]
        
        EXPOSE 80 443
        CMD ["nginx", "-g", "daemon off;"]# ---------- 1단계: build ----------
        FROM node:22-alpine AS build
    
        WORKDIR /app
        
        # 의존성 설치
        COPY package*.json ./
        RUN npm ci
        
        # 소스 + 환경변수
        COPY . .
        COPY .env.production .
        
        # Vite 빌드 (production mode)
        RUN npm run build
        
        # ---------- 2단계: nginx ----------
        FROM nginx:1.25-alpine
        
        # 빌드 산출물 복사
        COPY --from=build /app/dist /usr/share/nginx/html
        
        # 필요하면 커스텀 nginx conf 복사
        COPY nginx/default.conf /etc/nginx/conf.d/default.conf
        
        # 인증서 디렉터리 마운트용
        VOLUME ["/etc/letsencrypt"]
        
        EXPOSE 80 443
        CMD ["nginx", "-g", "daemon off;"]
    ```
    

### AI Audio docker-compose.yml

```python
version: "3.8"

services:
  ai-api-cpu:
    build:
      context: .
      # CPU 전용 Dockerfile을 지정
      dockerfile: Dockerfile.cpu
    ports:
      - "8000:8000"
    volumes:
      # 로컬 소스 코드와 동기화
      - .:/app
    # 원한다면 --reload 옵션 유지
    command: python FastAPI/main.py --host 0.0.0.0 --port 8000 --reload
    # GPU 리소스 설정 제거 (deploy -> resources -> reservations -> devices)

```

dockerfile

```python
FROM python:3.10-slim

RUN pip install uv wheel setuptools

RUN apt update && \
    apt install -y espeak-ng build-essential && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# PyTorch CPU 버전 설치
RUN pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu

# .env 파일 복사 (환경 변수 설정을 위해)
COPY .env ./

# 종속성 파일만 먼저 복사하여 캐시 활용
COPY pyproject.toml ./
# compile 옵션 제외하고 기본 패키지만 설치
RUN uv pip install --system -e .

# 포트 노출
EXPOSE 8000

# 소스 코드 복사 (자주 변경되는 부분을 마지막에 배치)
COPY . ./

# 기본 실행 명령어 설정 (FastAPI/main.py로 변경)
CMD ["python", "FastAPI/main.py", "--host", "0.0.0.0", "--port", "8000", "--reload"] 
```

### AI Image docker-compose.yml

```python
services:
  ai-image:
    build: .
    container_name: ai-image-service
    ports:
      - "8001:8001"
    volumes:
      # - .:/app  # 개발환경 hot-reloading
      - ./images:/app/images
      - ./data:/app/data
    env_file:
      - .env
    restart: unless-stopped
```

dockerfile

```python
# Python 3.11 이미지를 기반으로 사용
FROM python:3.12-slim

# 작업 디렉토리 설정
WORKDIR /app

# 시스템 패키지 업데이트 및 필요한 패키지 설치
RUN apt-get update && apt-get install -y \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# requirements.txt 파일 복사 및 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 애플리케이션 코드 복사
COPY . .

# 포트 설정
EXPOSE 8001

# 애플리케이션 실행
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001", "--reload", "--timeout-keep-alive", "600"] 
```

## 5 시연 시나리오

```python
1. 서비스 메인 접속
- https://shoshoshorts.duckdns.org/ 접속하면, '메인페이지' 위치

2. 회원가입 및 로그인 진행
- 우측상단의 마이페이지 버튼 클릭
- Kakao, google, naver 선택 OAuth 회원 가입/로그인 진행

3. 캐릭터 목소리 등록(보이스 클로닝)
- 하단의 '캐릭터 목소리 저장소'의 '목소리 등록하기' 버튼 클릭
- 제목, 설명, 등록하고자 하는 MP3 파일 선택 후 '등록하기 버튼' 클릭

4. 쇼츠 영상 생성 파이프라인
- 메인페이지 이동 후 '지금 시작하기 버튼' 클릭

5. AI 모델 설정 
- 음성 모델과 이미지 모델 각각 선택

6. 스토리 및 캐릭터 정보 입력
- 제목과 스토리, 내레이터의 목소리 입력/설정 (필수)
- 캐릭터 : 캐릭터 이름, 설명, 목소리 입력/설정 (선택)

7. 영상 생성 요청
- 동영상 생성하기 버튼 클릭 후 DashBoard로 이동

8. 영상 시청 및 결과 확인
- 생성된 영상 재생 확인
- 영상 다운로드 버튼 클릭하여 다운로드 진행

9. YouTube 공유
- 유튜브에 공유 버튼 클릭하여 유튜브 공유하기 진행
- 구글 계정으로 로그인 클릭하여 업로드하고자하는 계정 로그인
- 쇼츠 제목, 설명 작성 및 동의 후 업로드 버튼 클릭
- 업로드 완료

```

---

---