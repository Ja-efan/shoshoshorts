#!/bin/bash

# 환경 변수 파일 로드
if [ -f .env ]; then
  export $(cat .env | grep -v '#' | awk '/=/ {print $1}')
else
  echo "환경 변수 파일(.env)이 없습니다. .env.example을 복사하여 사용하세요."
  echo "cp .env.example .env"
  exit 1
fi

# 네트워크가 없으면 생성
if ! docker network inspect sss-network &>/dev/null; then
  echo "Docker 네트워크 'sss-network'를 생성합니다..."
  docker network create sss-network
fi

# 프론트엔드 컨테이너 실행
echo "프론트엔드 컨테이너를 빌드하고 실행합니다..."
docker build -t sss-frontend:latest ./FE
docker run -d \
  --name sss-frontend \
  -p ${FRONTEND_PORT:-80}:80 \
  --network sss-network \
  sss-frontend:latest

echo "프론트엔드 컨테이너가 실행되었습니다."
echo "프론트엔드는 http://localhost:${FRONTEND_PORT:-80} 에서 접근 가능합니다." 