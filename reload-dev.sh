#!/bin/bash

# 색상 설정
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}개발 환경을 재시작합니다...${NC}"
echo -e "${YELLOW}이 작업은 환경 변수 변경 사항을 적용하기 위해 모든 컨테이너를 재시작합니다.${NC}"

# 환경 변수 파일 로드
if [ -f .env ]; then
  export $(cat .env | grep -v '#' | awk '/=/ {print $1}')
else
  echo -e "${YELLOW}환경 변수 파일(.env)이 없습니다. .env.example을 복사하여 사용하세요.${NC}"
  echo "cp .env.example .env"
  exit 1
fi

# 현재 실행 중인 컨테이너 확인
RUNNING_CONTAINERS=$(docker ps --filter "name=sss-" --format "{{.Names}}")

# 컨테이너 중지 및 삭제
echo -e "${YELLOW}모든 개발 환경 컨테이너를 중지합니다...${NC}"
docker compose -f docker-compose.dev.yml down

# 데이터 유지 여부 확인
if [ -z "$1" ] || [ "$1" != "--preserve-data" ]; then
  read -p "볼륨을 유지하시겠습니까? (데이터 보존) [Y/n]: " preserve_data
  if [[ "$preserve_data" =~ ^[Nn]$ ]]; then
    echo -e "${YELLOW}모든 볼륨을 삭제합니다. 데이터가 초기화됩니다...${NC}"
    docker compose -f docker-compose.dev.yml down -v
  fi
else
  echo -e "${YELLOW}데이터를 보존하고 컨테이너만 재시작합니다...${NC}"
fi

# 환경 변수 다시 로드
echo -e "${GREEN}.env 파일의 변경된 환경 변수를 다시 로드합니다...${NC}"
export $(cat .env | grep -v '#' | awk '/=/ {print $1}')

# 이전에 실행 중이던 서비스 파악 및 재시작
echo -e "${GREEN}개발 환경을 다시 시작합니다...${NC}"

if echo "$RUNNING_CONTAINERS" | grep -q "sss-frontend-dev"; then
  echo -e "${GREEN}프론트엔드 개발 환경을 시작합니다...${NC}"
  ./run-frontend-dev.sh
elif echo "$RUNNING_CONTAINERS" | grep -q "sss-backend-dev"; then
  echo -e "${GREEN}백엔드 개발 환경을 시작합니다...${NC}"
  ./run-backend-dev.sh
else
  # 기본적으로 백엔드만 시작
  echo -e "${GREEN}기본 백엔드 개발 환경을 시작합니다...${NC}"
  ./run-backend-dev.sh
fi

echo -e "${GREEN}환경 변수가 적용된 개발 환경이 재시작되었습니다.${NC}"
echo -e "${GREEN}다음 명령어로 컨테이너 상태를 확인할 수 있습니다: docker ps${NC}"