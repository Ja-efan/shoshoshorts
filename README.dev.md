
# README.dev.md

# 🚀 쇼쇼숓 - 개발 환경 설정 가이드

## 📌 프로젝트 개요
사용자가 입력한 스토리를 기반으로 다양한 AI를 활용하여 숏폼 콘텐츠를 제작하는 웹 플랫폼입니다.

## 🐳 개발 환경 설정

### 사전 요구사항
- Docker 및 Docker Compose 설치
- Git

### 환경 설정
1. 프로젝트 클론
```bash
# 전체 프로젝트 클론
git clone https://lab.ssafy.com/s12-ai-speech-sub1/S12P21B106.git
cd S12P21B106

# 또는 개발 브랜치만 클론
git clone -b Dev/web --single-branch https://lab.ssafy.com/s12-ai-speech-sub1/S12P21B106.git
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
chmod +x build-images.sh run-backend-dev.sh run-frontend-dev.sh stop-containers.sh
```

## 🏗 개발 환경 실행 방법

### 1. Docker 이미지 빌드
```bash
# 대화형 모드로 실행
./build-images.sh

# 또는 필요한 이미지만 선택적으로 빌드
./build-images.sh base dev
```

### 2. 개발 환경 전체 실행 (Docker Compose 사용)
```bash
docker compose -f docker-compose.dev.yml up -d
```

### 3. 개별 서비스 실행 스크립트 사용 (선택사항)
```bash
# 백엔드 개발 서비스 실행 (데이터베이스 포함)
./run-backend-dev.sh

# 프론트엔드 개발 서비스 실행
./run-frontend-dev.sh
```

### 4. 서비스 중지 및 삭제
```bash
# 개발 환경 컨테이너만 중지 및 삭제
./stop-containers.sh dev

# 특정 서비스 중지 및 삭제 (예: 백엔드)
./stop-containers.sh backend dev

# 특정 서비스 중지 및 삭제 (예: 프론트엔드)
./stop-containers.sh frontend dev
```

## 💻 개발 워크플로우

1. 개발 환경 컨테이너 실행
2. 로컬에서 코드 수정 (BE 또는 FE 디렉토리)
3. 변경 사항 확인:
   - 백엔드: `http://localhost:8080`
   - 프론트엔드: `http://localhost:3000` (서버 초기화에 약 20초 소요)
4. 개발 완료 후 컨테이너 중지
5. 커밋 후 푸시

## 📝 포트 정보
- 백엔드: http://localhost:8080
- 프론트엔드: http://localhost:3000
- 개발용 PostgreSQL: localhost:5432
- 개발용 MongoDB: localhost:27017

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
./build-images.sh base dev
```

3. 컨테이너 재시작:
```bash
docker compose -f docker-compose.dev.yml down
docker compose -f docker-compose.dev.yml up -d
```

## ✅ 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너 목록 확인
docker ps

# 컨테이너 로그 확인
docker logs sss-backend-dev
docker logs sss-frontend-dev
docker logs sss-postgres-dev
docker logs sss-mongodb-dev
```

## ⚠️ 주의사항

- 동일한 포트를 사용하는 서비스가 이미 실행 중인 경우 포트 충돌이 발생할 수 있습니다.
- 개발 환경과 배포 환경을 동시에 실행할 경우 포트 충돌이 발생할 수 있으므로 주의하세요.
- 데이터베이스 데이터는 Docker 볼륨에 저장되므로, 볼륨을 삭제하면 데이터가 손실됩니다.
- 프론트엔드 개발 서버는 초기화에 약 20초 정도 소요됩니다. 스크립트 실행 후 잠시 기다려주세요.