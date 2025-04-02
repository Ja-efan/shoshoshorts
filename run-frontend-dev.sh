#!/bin/bash

# 환경 변수 파일 로드
if [ -f .env ]; then
  export $(cat .env | grep -v '#' | awk '/=/ {print $1}')
else
  echo "환경 변수 파일(.env)이 없습니다. .env.example을 복사하여 사용하세요."
  echo "cp .env.example .env"
  exit 1
fi

# 프론트엔드 개발 이미지가 없으면 빌드
if ! docker image inspect sss-frontend-dev:latest &>/dev/null; then
  echo "프론트엔드 개발 이미지가 없습니다. 이미지를 빌드합니다..."
  ./build-images.sh
fi

# 종속성 안내 메시지 출력
echo "─────────────────────────────────────────────────────────────────"
echo "📌 프론트엔드 개발 환경은 종속성 관계로 다음 컨테이너를 함께 실행합니다:"
echo "   - 프론트엔드 (frontend)"
echo "   - 백엔드 (backend)"
echo "   - PostgreSQL 데이터베이스 (db)"
echo "   - MongoDB 데이터베이스 (mongo)"
echo "─────────────────────────────────────────────────────────────────"

# Docker Compose를 사용하여 프론트엔드 서비스 실행
echo "Docker Compose를 사용하여 프론트엔드 개발 환경을 실행합니다..."
docker compose -f docker-compose.dev.yml up -d frontend redis

echo "프론트엔드 개발 환경이 실행되었습니다."
echo "프론트엔드는 http://localhost:3000 에서 접근 가능합니다."
echo "소스 코드를 수정하면 자동으로 반영됩니다."