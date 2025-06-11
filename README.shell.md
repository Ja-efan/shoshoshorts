# 🛠 Shell 스크립트 가이드

1. [스크립트 목록](#스크립트-목록)
2. [시작하기 전에](#-시작하기-전에)
3. [기본 사용법](#-기본-사용법)
4. [개발 워크플로우](#-개발-워크플로우)
5. [주의사항](#️-주의사항)
6. [정리](#-정리)

프로젝트 루트 디렉토리에 있는 shell 스크립트들에 대한 요약 설명입니다.

## 스크립트 목록

### 📜 build-images.sh
Docker 이미지 빌드를 위한 스크립트입니다.
```bash
./build-images.sh [base|dev|prod|all]
```

### 📜 run-backend-dev.sh
개발 환경에서 백엔드 서비스와 필요한 데이터베이스를 실행합니다.
- 백엔드, PostgreSQL, MongoDB 컨테이너를 실행
- 소스 코드 변경 시 자동 반영

### 📜 run-frontend-dev.sh
개발 환경에서 프론트엔드 서비스를 실행합니다.
- 프론트엔드 컨테이너 실행 (종속성으로 백엔드, 데이터베이스도 함께 실행)
- 소스 코드 변경 시 자동 반영

### 📜 reload-compose-dev.sh
환경 변수 변경 후 개발 환경을 재시작하는 스크립트입니다.
```bash
# 환경 변수 변경 후 컨테이너 재시작
./reload-dev.sh

# 데이터를 유지하면서 재시작 (프롬프트 없음)
./reload-dev.sh --preserve-data
```
- 현재 실행 중인 컨테이너 구성을 기억하고 동일한 구성으로 재시작
- 선택적으로 볼륨 데이터 유지 가능

### 📜 stop-containers.sh
실행 중인 컨테이너를 중지하고 삭제합니다.
```bash
# 모든 개발 환경 컨테이너 중지
./stop-containers.sh all

# 특정 서비스 중지
./stop-containers.sh [backend|frontend|postgres|mongo|gradle]
```

## 🚀 시작하기 전에

### 환경 설정
환경 변수 파일을 생성하고 필요한 변수를 설정하세요:
```bash
cp .env.example .env
```

### 실행 권한 부여
스크립트에 실행 권한을 부여하세요:
```bash
chmod +x *.sh
```

## 📝 기본 사용법

### 1. Docker 이미지 빌드
```bash
# 모든 이미지 빌드
./build-images.sh all

# 개발 환경 이미지만 빌드
./build-images.sh base dev
```

### 2. 개발 환경 실행
```bash
# 백엔드 개발 환경 실행
./run-backend-dev.sh

# 프론트엔드 개발 환경 실행 (백엔드 포함)
./run-frontend-dev.sh
```

### 3. 환경 변수 변경 후 재시작
```bash
# 환경 변수 변경 후 개발 환경 재시작
./reload-dev.sh
```

### 4. 컨테이너 관리
```bash
# 모든 컨테이너 중지
./stop-containers.sh all

# 특정 서비스만 중지
./stop-containers.sh backend
./stop-containers.sh frontend
```

### 5. 컨테이너 상태 확인
```bash
# 실행 중인 컨테이너 확인
docker ps

# 로그 확인
docker logs sss-backend-dev
docker logs sss-frontend-dev
```

## 🔄 개발 워크플로우

1. 개발 환경 이미지 빌드:
```bash
./build-images.sh base dev
```

2. 개발 환경 실행:
```bash
./run-frontend-dev.sh  # 프론트엔드와 백엔드 모두 실행
```

3. 코드 수정 (BE 또는 FE 디렉토리)
   - 소스 코드 변경 시 자동으로 반영됩니다.

4. 환경 변수 수정 (.env 파일)
   - 환경 변수 변경 후 다음 명령어로 재시작:
   ```bash
   ./reload-dev.sh
   ```

5. 접근 URL:
   - 백엔드: `http://localhost:${BACKEND_PORT}`
   - 프론트엔드: `http://localhost:${FRONTEND_PORT}`

6. 개발 완료 후 정리:
```bash
./stop-containers.sh all
```

## ⚠️ 주의사항

- 포트 충돌: 이미 사용 중인 포트와 충돌할 수 있습니다.
- 데이터 손실: 볼륨 삭제 시 저장된 데이터가 모두 삭제됩니다.
- 볼륨 관리: PostgreSQL, MongoDB 볼륨 삭제 시 특히 주의하세요.
- 환경 변수: `.env` 파일 변경 후에는 반드시 `./reload-dev.sh`로 재시작해야 합니다.

## 🧹 정리

개발 환경 완전 정리:
```bash
# 모든 컨테이너 중지 및 볼륨 삭제
./stop-containers.sh all  # 볼륨 삭제 여부 질문에 'y' 선택

# 남은 볼륨 확인 및 삭제
docker volume ls
docker volume rm postgres-data-dev mongo-data-dev gradle-cache

# 이미지 삭제
docker rmi sss-backend-base sss-frontend-base sss-backend-dev sss-frontend-dev
``` 