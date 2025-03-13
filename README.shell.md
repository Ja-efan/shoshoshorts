# 🛠️ 쉘 스크립트 사용 가이드

이 문서는 프로젝트에서 제공하는 쉘 스크립트의 사용법을 설명합니다. 이 스크립트들은 Docker 컨테이너를 통해 백엔드, 프론트엔드, 데이터베이스 서비스를 쉽게 실행하고 관리할 수 있도록 도와줍니다.

## 📋 스크립트 목록

| 스크립트 | 설명 |
|---------|------|
| `run-backend.sh` | 배포 환경용 백엔드 서비스 실행 (데이터베이스 포함) |
| `run-frontend.sh` | 배포 환경용 프론트엔드 서비스 실행 |
| `run-backend-dev.sh` | 개발 환경용 백엔드 서비스 실행 (데이터베이스 포함) |
| `run-frontend-dev.sh` | 개발 환경용 프론트엔드 서비스 실행 |
| `stop-containers.sh` | 컨테이너 중지 및 삭제 |

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
chmod +x run-backend.sh run-frontend.sh run-backend-dev.sh run-frontend-dev.sh stop-containers.sh
```

## 📝 스크립트 사용법

### 1. 배포 환경 스크립트

#### 백엔드 서비스 실행
```bash
./run-backend.sh
```

이 스크립트는 다음 작업을 수행합니다:
- PostgreSQL 데이터베이스 컨테이너(`sss-postgres`) 실행
- 백엔드 애플리케이션 빌드 및 컨테이너(`sss-backend`) 실행
- 백엔드 API는 `http://localhost:8080`에서 접근 가능

#### 프론트엔드 서비스 실행
```bash
./run-frontend.sh
```

이 스크립트는 다음 작업을 수행합니다:
- 프론트엔드 애플리케이션 빌드 및 컨테이너(`sss-frontend`) 실행
- 프론트엔드는 `http://localhost:80` 또는 `.env`에 설정된 `FRONTEND_PORT`에서 접근 가능

### 2. 개발 환경 스크립트

#### 백엔드 개발 서비스 실행
```bash
./run-backend-dev.sh
```

이 스크립트는 다음 작업을 수행합니다:
- 개발용 PostgreSQL 데이터베이스 컨테이너(`sss-postgres-dev`) 실행
- 백엔드 개발 컨테이너(`sss-backend-dev`) 실행
- 로컬 소스 코드를 볼륨으로 마운트하여 코드 변경 시 자동 반영
- 백엔드 API는 `http://localhost:8080`에서 접근 가능

#### 프론트엔드 개발 서비스 실행
```bash
./run-frontend-dev.sh
```

이 스크립트는 다음 작업을 수행합니다:
- 프론트엔드 개발 컨테이너(`sss-frontend-dev`) 실행
- 로컬 소스 코드를 볼륨으로 마운트하여 코드 변경 시 자동 반영
- Vite 개발 서버를 실행하여 핫 리로딩 지원
- 서버 초기화를 위해 10초간 대기 후 접속 정보 표시
- 프론트엔드는 `http://localhost:3000`에서 접근 가능 (내부 5173 포트를 3000으로 매핑)

### 3. 컨테이너 관리 스크립트

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

## 🔍 컨테이너 상태 확인

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

1. 개발 환경 컨테이너 실행:
```bash
./run-backend-dev.sh
./run-frontend-dev.sh
```

2. 로컬에서 코드 수정 (BE 또는 FE 디렉토리)

3. 변경 사항 확인:
   - 백엔드: `http://localhost:8080`
   - 프론트엔드: `http://localhost:3000` (서버 초기화에 약 10초 소요)

4. 개발 완료 후 컨테이너 중지:
```bash
./stop-containers.sh dev
```

## ⚠️ 주의사항

- 동일한 포트를 사용하는 서비스가 이미 실행 중인 경우 포트 충돌이 발생할 수 있습니다.
- 개발 환경과 배포 환경을 동시에 실행할 경우 포트 충돌이 발생할 수 있으므로 주의하세요.
- 데이터베이스 데이터는 Docker 볼륨에 저장되므로, 볼륨을 삭제하면 데이터가 손실됩니다.
- 프론트엔드 개발 서버는 초기화에 약 10초 정도 소요됩니다. 스크립트 실행 후 잠시 기다려주세요.

## 🧹 정리

모든 컨테이너, 네트워크, 볼륨 삭제:
```bash
# 모든 컨테이너 중지 및 삭제
./stop-containers.sh all

# 네트워크 삭제
docker network rm sss-network

# 볼륨 삭제 (주의: 데이터가 손실됩니다)
docker volume rm postgres-data postgres-data-dev gradle-cache
``` 