# README.dev.md

# 🚀 쇼쇼숏 - 개발 환경 설정 가이드

## 📌 프로젝트 개요
사용자가 입력한 스토리를 기반으로 다양한 AI를 활용하여 숏폼 콘텐츠를 제작하는 웹 플랫폼입니다.

## 🐳 개발 환경 설정

### 사전 요구사항
- [Docker](https://docs.docker.com/get-docker/) 최신 버전
- [Docker Compose](https://docs.docker.com/compose/install/) 최신 버전
- Git

### 환경 설정 단계
1. **프로젝트 클론**
   ```bash
   # 전체 프로젝트 클론
   git clone https://lab.ssafy.com/s12-ai-speech-sub1/S12P21B106.git
   cd S12P21B106

   # 또는 개발 브랜치만 클론
   git clone -b Dev/web --single-branch https://lab.ssafy.com/s12-ai-speech-sub1/S12P21B106.git
   cd S12P21B106
   ```

2. **환경 변수 파일 생성**
   ```bash
   cp .env.example .env
   ```

3. **.env 파일 설정**  
   `.env` 파일을 열고 다음 필수 환경 변수들을 확인하고 필요한 경우 수정하세요.
   ```
   # 포트 설정
   BACKEND_PORT=8080
   FRONTEND_PORT=3000
   
   # PostgreSQL 설정
   POSTGRES_DB=sss_pg_db
   POSTGRES_USER=b106
   POSTGRES_PASSWORD= ********
   POSTGRES_PORT=5432
   
   # MongoDB 설정
   MONGO_DB=sss_db
   MONGO_USER=b106
   MONGO_PASSWORD= ********
   MONGO_PORT=27017
   ```

4. **스크립트 실행 권한 부여**
   ```bash
   chmod +x *.sh
   ```

## 🏗 개발 환경 실행 방법

### 1. Docker 이미지 빌드
```bash
# 개발에 필요한 기본 이미지 및 개발 환경 이미지 빌드
./build-images.sh base dev

# 또는 대화형 모드로 실행하여 필요한 이미지 선택
./build-images.sh
```

### 2. 개발 환경 실행
```bash
# 프론트엔드 개발 환경 실행 (백엔드, 데이터베이스 포함)
./run-frontend-dev.sh

# 또는 백엔드 개발 환경만 실행 (데이터베이스 포함)
./run-backend-dev.sh
```

> **참고**: `run-frontend-dev.sh` 실행 시 종속성으로 인해 다음 컨테이너가 모두 실행됩니다:
> - 프론트엔드 (frontend)
> - 백엔드 (backend)
> - PostgreSQL 데이터베이스 (db)
> - MongoDB 데이터베이스 (mongo)

### 3. 환경 변수 변경 후 재시작
.env 파일의 환경 변수를 변경한 후에는 변경 사항을 적용하기 위해 컨테이너를 재시작해야 합니다.
```bash
# 환경 변수 변경 후 개발 환경 재시작
./reload-dev.sh

# 데이터를 유지하면서 재시작 (대화식 프롬프트 없이)
./reload-dev.sh --preserve-data
```

이 스크립트는 다음 작업을 수행합니다:
- 현재 실행 중인 컨테이너 상태 확인
- 모든 개발 환경 컨테이너 중지
- 볼륨 유지 여부 확인 (데이터 보존 여부)
- 변경된 환경 변수로 컨테이너 재시작
- 이전에 실행 중이던 서비스 구성 유지 (프론트엔드 또는 백엔드)

### 4. 서비스 접속 정보
- **백엔드 API**: http://localhost:8080 (또는 `.env`에 설정된 `BACKEND_PORT`)
- **프론트엔드**: http://localhost:3000
- **PostgreSQL**: localhost:5432
- **MongoDB**: localhost:27017

### 5. 서비스 중지 및 관리
```bash
# 모든 개발 환경 컨테이너 중지 (볼륨 삭제 여부 선택)
./stop-containers.sh all

# 특정 서비스만 중지 및 삭제
./stop-containers.sh backend  # 백엔드만 중지
./stop-containers.sh frontend  # 프론트엔드만 중지
./stop-containers.sh postgres  # PostgreSQL만 중지 (볼륨 삭제 여부 선택)
./stop-containers.sh mongo  # MongoDB만 중지 (볼륨 삭제 여부 선택)
./stop-containers.sh gradle  # Gradle 캐시 볼륨 삭제
```

## 💻 개발 워크플로우

1. **코드 수정**
   - `BE/` 디렉토리: 백엔드 소스 코드 (Spring Boot)
   - `FE/` 디렉토리: 프론트엔드 소스 코드 (React/Vue 등)

2. **변경사항 자동 반영**
   - 로컬에서 코드를 수정하면 볼륨 마운트를 통해 컨테이너에 자동으로 반영됩니다.
   - 백엔드 코드 변경 시 Spring Boot DevTools이 자동으로 서버를 재시작합니다.
   - 프론트엔드 코드 변경 시 Vite의 핫 리로드 기능이 자동으로 변경사항을 반영합니다.

3. **환경 변수 변경 시**
   - `.env` 파일에 환경 변수를 추가하거나 수정한 경우 `./reload-dev.sh` 명령으로 재시작해야 합니다.
   - 코드만 수정한 경우에는 재시작이 필요 없습니다 (자동 반영).

4. **개발 완료 후 정리**
   ```bash
   ./stop-containers.sh all
   ```

## 📋 Docker 컨테이너 관리

### 컨테이너 상태 확인
```bash
# 실행 중인 컨테이너 목록 확인
docker ps

# 모든 컨테이너 목록 확인 (중지된 것 포함)
docker ps -a
```

### 컨테이너 로그 확인
```bash
# 백엔드 로그 확인
docker logs sss-backend-dev

# 프론트엔드 로그 확인
docker logs sss-frontend-dev

# 데이터베이스 로그 확인
docker logs sss-postgres-dev
docker logs sss-mongo-dev

# 실시간 로그 확인 (로그 추적)
docker logs -f sss-frontend-dev
```

### 볼륨 관리
```bash
# 볼륨 목록 확인
docker volume ls

# 특정 볼륨 삭제
docker volume rm postgres-data-dev
docker volume rm mongo-data-dev
docker volume rm gradle-cache
```

## 🔄 시스템 의존성 관리

새로운 시스템 의존성 추가가 필요한 경우:

1. **Dockerfile 수정**
   ```bash
   # 백엔드 의존성 추가
   vi BE/Dockerfile.base
   
   # 프론트엔드 의존성 추가
   vi FE/Dockerfile.base
   ```

2. **이미지 재빌드 및 컨테이너 재시작**
   ```bash
   # 이미지 재빌드
   ./build-images.sh base dev
   
   # 컨테이너 재시작
   ./stop-containers.sh all
   ./run-frontend-dev.sh
   ```

## ⚠️ 주의사항

- **포트 충돌**: 이미 사용 중인 포트가 있을 경우 충돌이 발생할 수 있습니다. `.env` 파일에서 포트를 변경하여 해결할 수 있습니다.

- **Docker 네트워크**: 모든 컨테이너는 `sss-network`라는 Docker 네트워크에 연결됩니다. 컨테이너 간 통신은 이 네트워크를 통해 이루어집니다.

- **데이터 보존**: 데이터베이스 데이터는 Docker 볼륨(`postgres-data-dev`, `mongo-data-dev`)에 저장됩니다. `./stop-containers.sh all` 명령 실행 시 볼륨 삭제 여부를 선택할 수 있습니다.

- **Gradle 캐시**: 백엔드 빌드 성능 향상을 위해 Gradle 캐시가 `gradle-cache` 볼륨에 저장됩니다. 빌드 문제 발생 시 `./stop-containers.sh gradle` 명령으로 캐시를 삭제할 수 있습니다.

- **환경 변수 변경**: `.env` 파일의 환경 변수를 변경한 후에는 반드시 `./reload-dev.sh` 명령을 실행하여 변경 사항을 컨테이너에 적용해야 합니다.

## 🧹 개발 환경 완전 정리

더 이상 개발 환경이 필요하지 않을 때 다음 명령으로 모든 리소스를 정리할 수 있습니다:

```bash
# 모든 컨테이너 중지 및 볼륨 삭제 (볼륨 삭제 여부 질문에 'y' 선택)
./stop-containers.sh all

# 남은 볼륨 삭제
docker volume rm postgres-data-dev mongo-data-dev gradle-cache

# 이미지 삭제
docker rmi sss-backend-base sss-frontend-base sss-backend-dev sss-frontend-dev
```

## 🔧 문제 해결

- **PostgreSQL 연결 오류**: 환경 변수가 올바르게 설정되었는지 확인하세요. 볼륨 문제가 의심되면 `./stop-containers.sh postgres` 명령을 실행하고 볼륨 삭제를 선택하여 초기화할 수 있습니다.

- **MongoDB 연결 오류**: 환경 변수를 확인하세요. 문제가 지속되면 `./stop-containers.sh mongo` 명령을 실행하고 볼륨 삭제를 선택하세요.

- **이미지 빌드 실패**: 오류 메시지를 확인하고 필요한 시스템 의존성이 추가되었는지 확인하세요. Docker가 최신 버전인지 확인하세요.

- **핫 리로드 작동 안 함**: 볼륨 마운트가 올바르게 설정되었는지 확인하세요. `docker compose -f docker-compose.dev.yml config` 명령으로 설정을 확인할 수 있습니다.

- **환경 변수가 적용되지 않음**: 환경 변수를 변경한 후 `./reload-dev.sh` 명령을 실행했는지 확인하세요.