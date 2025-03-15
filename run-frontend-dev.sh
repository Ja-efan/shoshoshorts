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

# 프론트엔드 개발 이미지가 없으면 빌드
if ! docker image inspect sss-frontend-dev:latest &>/dev/null; then
  echo "프론트엔드 개발 이미지가 없습니다. 이미지를 빌드합니다..."
  ./build-images.sh
fi

# 프론트엔드 개발 컨테이너 실행
echo "프론트엔드 개발 컨테이너를 실행합니다..."
docker run -d \
  --name sss-frontend-dev \
  -p 3000:5173 \
  -v $(pwd)/FE:/app \
  -v /app/node_modules \
  --network sss-network \
  sss-frontend-dev:latest

echo "프론트엔드 개발 컨테이너가 시작되었습니다. 서버가 초기화되는 중입니다..."
echo "소스 코드를 수정하면 자동으로 반영됩니다."

# 15초 대기 후 접속 정보 표시
sleep 15
echo "프론트엔드는 http://localhost:3000 에서 접근 가능합니다." 