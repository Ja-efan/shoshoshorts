# 🛠 Shell 스크립트 가이드

1. [build-images.sh](#-build-imagessh)
   - [사용법](#사용법)
   - [기능](#기능)
2. [run-backend.sh](#-run-backendsh)
   - [기능](#기능-1)
3. [run-backend-dev.sh](#-run-backend-devsh)
   - [기능](#기능-2)
4. [run-frontend.sh](#-run-frontendsh)
   - [기능](#기능-3)
5. [run-frontend-dev.sh](#-run-frontend-devsh)
   - [기능](#기능-4)
6. [stop-containers.sh](#-stop-containerssh)
   - [사용법](#사용법-1)
   - [기능](#기능-5)
7. [시작하기 전에](#-시작하기-전에)
   - [환경 설정](#환경-설정)
   - [실행 권한 부여](#실행-권한-부여)
8. [스크립트 사용법](#-스크립트-사용법)
   - [Docker 이미지 빌드](#1-docker-이미지-빌드)
   - [배포 환경 스크립트](#2-배포-환경-스크립트)
   - [개발 환경 스크립트](#3-개발-환경-스크립트)
   - [Docker Compose 사용](#4-docker-compose-사용)
   - [컨테이너 관리 스크립트](#5-컨테이너-관리-스크립트)
9. [컨테이너 상태 확인](#-컨테이너-상태-확인)
10. [개발 워크플로우](#-개발-워크플로우)
11. [시스템 의존성 관리](#-시스템-의존성-관리)
12. [주의사항](#️-주의사항)
13. [정리](#-정리)

프로젝트 루트 디렉토리에 있는 shell 스크립트들에 대한 설명입니다.

## 📜 build-images.sh
Docker 이미지 빌드를 위한 스크립트입니다.

### 사용법
```bash
# 대화형 모드로 실행 (각 단계별로 빌드 여부를 선택할 수 있음)
./build-images.sh

# 특정 환경의 이미지만 빌드
./build-images.sh base  # 기본 이미지만 빌드
./build-images.sh dev   # 개발 환경 이미지만 빌드
./build-images.sh prod  # 배포 환경 이미지만 빌드

# 모든 이미지 빌드
./build-images.sh all
```

### 기능
- 기본 이미지 (base): 시스템 의존성을 포함한 기본 이미지
- 개발 환경 이미지 (dev): 개발 환경에서 사용할 이미지
- 배포 환경 이미지 (prod): 실제 배포에서 사용할 이미지

## 📜 run-backend.sh
백엔드 서비스와 필요한 데이터베이스를 실행하는 스크립트입니다.

### 기능
- PostgreSQL 데이터베이스 컨테이너 실행
- MongoDB 데이터베이스 컨테이너 실행
- 백엔드 서비스 컨테이너 실행
- Docker 네트워크 생성 및 연결

## 📜 run-backend-dev.sh
개발 환경에서 백엔드 서비스를 실행하는 스크립트입니다.

### 기능
- 개발용 PostgreSQL 데이터베이스 컨테이너 실행
- 개발용 MongoDB 데이터베이스 컨테이너 실행
- 개발 모드로 백엔드 서비스 컨테이너 실행
- 핫 리로드 지원을 위한 볼륨 마운트 설정

## 📜 run-frontend.sh
프론트엔드 서비스를 실행하는 스크립트입니다.

### 기능
- 프로덕션 모드로 프론트엔드 서비스 컨테이너 실행
- Nginx를 통한 정적 파일 서빙 설정

## 📜 run-frontend-dev.sh
개발 환경에서 프론트엔드 서비스를 실행하는 스크립트입니다.

### 기능
- Vite 개발 서버를 통한 프론트엔드 서비스 실행
- 핫 리로드 지원을 위한 볼륨 마운트 설정
- 소스 코드 변경 감지 및 자동 리로드

## 📜 stop-containers.sh
실행 중인 컨테이너를 중지하고 삭제하는 스크립트입니다.

### 사용법
```bash
# 모든 컨테이너 중지 및 삭제
./stop-containers.sh all

# 개발 환경 컨테이너만 중지 및 삭제
./stop-containers.sh dev

# 배포 환경 컨테이너만 중지 및 삭제
./stop-containers.sh prod
```

### 기능
- 지정된 환경의 컨테이너 중지
- 중지된 컨테이너 삭제
- 컨테이너 로그 출력 (오류 발생 시)

## 🚀 시작하기 전에

### 환경 설정
모든 스크립트는 `.env` 파일에서 환경 변수를 로드합니다. 처음 실행하기 전에 다음 명령어로 환경 변수 파일을 생성하세요:

```bash
cp .env.example .env
```

그리고 `.env` 파일을 열어 필요한 환경 변수 값을 설정하세요.

### 실행 권한 부여
스크립트를 실행하기 전에 실행 권한을 부여해야 합니다:

```bash
chmod +x build-images.sh run-backend.sh run-frontend.sh run-backend-dev.sh run-frontend-dev.sh stop-containers.sh
```

## 📝 스크립트 사용법

### 1. Docker 이미지 빌드

프로젝트는 개발 환경과 배포 환경 간의 일관성을 유지하기 위해 공통 기본 이미지를 사용합니다. 이미지 빌드 스크립트를 통해 필요한 이미지를 빌드할 수 있습니다.

#### 대화형 모드로 이미지 빌드
```bash
./build-images.sh
```

위 명령은 기본 이미지를 빌드한 후, 개발 및 배포 환경 이미지 빌드 여부를 사용자에게 물어봅니다.

#### 비대화형 모드로 이미지 빌드
```bash
# 기본 이미지만 빌드
./build-images.sh base

# 기본 이미지와 개발 환경 이미지 빌드
./build-images.sh base dev

# 기본 이미지와 배포 환경 이미지 빌드
./build-images.sh base prod

# 모든 이미지 빌드
./build-images.sh all
```

### 2. 배포 환경 스크립트

#### 백엔드 서비스 실행
```bash
./run-backend.sh
```

이 스크립트는 다음 작업을 수행합니다:
- PostgreSQL 데이터베이스 컨테이너(`sss-postgres`) 실행
- 필요한 경우 백엔드 이미지 빌드
- 백엔드 컨테이너(`sss-backend`) 실행
- 백엔드 API는 `http://localhost:8080`에서 접근 가능

#### 프론트엔드 서비스 실행
```bash
./run-frontend.sh
```

이 스크립트는 다음 작업을 수행합니다:
- 필요한 경우 프론트엔드 이미지 빌드
- 프론트엔드 컨테이너(`sss-frontend`) 실행
- 프론트엔드는 `http://localhost:80` 또는 `.env`에 설정된 `FRONTEND_PORT`에서 접근 가능

### 3. 개발 환경 스크립트

#### 백엔드 개발 서비스 실행
```bash
./run-backend-dev.sh
```

이 스크립트는 다음 작업을 수행합니다:
- 개발용 PostgreSQL 데이터베이스 컨테이너(`sss-postgres-dev`) 실행
- 필요한 경우 백엔드 개발 이미지 빌드
- 백엔드 개발 컨테이너(`sss-backend-dev`) 실행
- 로컬 소스 코드를 볼륨으로 마운트하여 코드 변경 시 자동 반영
- 백엔드 API는 `http://localhost:8080`에서 접근 가능

#### 프론트엔드 개발 서비스 실행
```bash
./run-frontend-dev.sh
```

이 스크립트는 다음 작업을 수행합니다:
- 필요한 경우 프론트엔드 개발 이미지 빌드
- 프론트엔드 개발 컨테이너(`sss-frontend-dev`) 실행
- 로컬 소스 코드를 볼륨으로 마운트하여 코드 변경 시 자동 반영
- Vite 개발 서버를 실행하여 핫 리로딩 지원
- 서버 초기화를 위해 15초간 대기 후 접속 정보 표시
- 프론트엔드는 `http://localhost:3000`에서 접근 가능 (내부 5173 포트를 3000으로 매핑)

### 4. Docker Compose 사용

#### 개발 환경 실행
```bash
docker compose -f docker-compose.dev.yml up -d
```

#### 배포 환경 실행
```bash
docker compose up -d
```

### 5. 컨테이너 관리 스크립트

#### 모든 컨테이너 중지 및 삭제
```bash
./stop-containers.sh all
```

#### 배포 환경 컨테이너만 중지 및 삭제
```bash
./stop-containers.sh prod
```

#### 개발 환경 컨테이너만 중지 및 삭제
```bash
./stop-containers.sh dev
```

#### 특정 서비스 중지 및 삭제
```bash
# 백엔드 컨테이너 중지 및 삭제
./stop-containers.sh backend

# 개발 환경의 백엔드 컨테이너 중지 및 삭제
./stop-containers.sh backend dev

# 프론트엔드 컨테이너 중지 및 삭제
./stop-containers.sh frontend

# 개발 환경의 프론트엔드 컨테이너 중지 및 삭제
./stop-containers.sh frontend dev

# 데이터베이스 컨테이너 중지 및 삭제
./stop-containers.sh db

# 개발 환경의 데이터베이스 컨테이너 중지 및 삭제
./stop-containers.sh db dev
```

##  ✅ 컨테이너 상태 확인

실행 중인 컨테이너 목록 확인:
```bash
docker ps
```

모든 컨테이너 목록 확인(중지된 컨테이너 포함):
```bash
docker ps -a
```

컨테이너 로그 확인:
```bash
# 백엔드 로그 확인
docker logs sss-backend
docker logs sss-backend-dev

# 프론트엔드 로그 확인
docker logs sss-frontend
docker logs sss-frontend-dev

# 데이터베이스 로그 확인
docker logs sss-postgres
docker logs sss-postgres-dev
```

## 🔄 개발 워크플로우

1. 기본 이미지 및 개발 환경 이미지 빌드:
```bash
./build-images.sh base dev
```

2. 개발 환경 컨테이너 실행:
```bash
./run-backend-dev.sh
./run-frontend-dev.sh
```

또는 Docker Compose 사용:
```bash
docker compose -f docker-compose.dev.yml up -d
```

3. 로컬에서 코드 수정 (BE 또는 FE 디렉토리)

4. 변경 사항 확인:
   - 백엔드: `http://localhost:8080`
   - 프론트엔드: `http://localhost:3000` (서버 초기화에 약 15초 소요)

5. 개발 완료 후 컨테이너 중지:
```bash
./stop-containers.sh dev
```

## 🔄 시스템 의존성 관리

프로젝트는 개발 환경과 배포 환경 간의 시스템 의존성(예: ffmpeg)을 일관되게 관리하기 위해 공통 기본 이미지를 사용합니다.

### 새로운 시스템 의존성 추가 방법

1. 기본 Dockerfile 수정:
```bash
# 백엔드 의존성 추가
vi BE/Dockerfile.base
# 프론트엔드 의존성 추가
vi FE/Dockerfile.base
```

2. 이미지 재빌드:
```bash
./build-images.sh all
```

3. 컨테이너 재시작:
```bash
# 개발 환경
docker compose -f docker-compose.dev.yml down
docker compose -f docker-compose.dev.yml up -d

# 또는 배포 환경
docker compose down
docker compose up -d
```

이 방식을 통해 개발 환경에서 추가한 시스템 의존성이 배포 환경에도 자동으로 적용됩니다.

## ⚠️ 주의사항

- 동일한 포트를 사용하는 서비스가 이미 실행 중인 경우 포트 충돌이 발생할 수 있습니다.
- 개발 환경과 배포 환경을 동시에 실행할 경우 포트 충돌이 발생할 수 있으므로 주의하세요.
- 데이터베이스 데이터는 Docker 볼륨에 저장되므로, 볼륨을 삭제하면 데이터가 손실됩니다.
- 프론트엔드 개발 서버는 초기화에 약 20초 정도 소요됩니다. 스크립트 실행 후 잠시 기다려주세요.

## 🧹 정리

모든 컨테이너, 네트워크, 볼륨 삭제:
```bash
# 모든 컨테이너 중지 및 삭제
./stop-containers.sh all

# 네트워크 삭제
docker network rm sss-network

# 볼륨 삭제 (주의: 데이터가 손실됩니다)
docker volume rm postgres-data postgres-data-dev gradle-cache

# 이미지 삭제
docker rmi sss-backend-base sss-frontend-base sss-backend-dev sss-frontend-dev sss-backend sss-frontend
``` 