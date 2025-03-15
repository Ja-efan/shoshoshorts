#!/bin/bash

# 스크립트 실행 디렉토리를 프로젝트 루트로 변경
cd "$(dirname "$0")"

# 색상 설정
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 명령줄 인수 처리
BUILD_BASE=false
BUILD_DEV=false
BUILD_PROD=false
NON_INTERACTIVE=false

# 인수가 없으면 대화형 모드
if [ $# -eq 0 ]; then
  NON_INTERACTIVE=false
else
  NON_INTERACTIVE=true
  for arg in "$@"; do
    case $arg in
      base) BUILD_BASE=true ;;
      dev) BUILD_DEV=true ;;
      prod) BUILD_PROD=true ;;
      all) BUILD_BASE=true; BUILD_DEV=true; BUILD_PROD=true ;;
    esac
  done
fi

# 환경 변수 파일 로드
if [ -f .env ]; then
  export $(cat .env | grep -v '#' | awk '/=/ {print $1}')
else
  echo -e "${YELLOW}환경 변수 파일(.env)이 없습니다. .env.example을 복사하여 사용하세요.${NC}"
  echo "cp .env.example .env"
  exit 1
fi

# 기본 이미지 빌드 여부 확인
if [ "$NON_INTERACTIVE" = false ]; then
  BUILD_BASE=true
fi

# 백엔드 기본 이미지 빌드
if [ "$BUILD_BASE" = true ]; then
  echo -e "${GREEN}백엔드 기본 이미지를 빌드합니다...${NC}"
  docker build -t sss-backend-base:latest -f BE/Dockerfile.base ./BE

  # 프론트엔드 기본 이미지 빌드
  echo -e "${GREEN}프론트엔드 기본 이미지를 빌드합니다...${NC}"
  docker build -t sss-frontend-base:latest -f FE/Dockerfile.base ./FE
fi

# 개발 환경 이미지 빌드 여부 확인
if [ "$NON_INTERACTIVE" = false ]; then
  read -p "개발 환경 이미지를 빌드하시겠습니까? (y/n): " build_dev_input
  if [[ $build_dev_input == "y" || $build_dev_input == "Y" ]]; then
    BUILD_DEV=true
  fi
fi

# 개발 환경 이미지 빌드
if [ "$BUILD_DEV" = true ]; then
  # 백엔드 개발 이미지 빌드
  echo -e "${GREEN}백엔드 개발 이미지를 빌드합니다...${NC}"
  docker build -t sss-backend-dev:latest -f BE/Dockerfile.dev ./BE
  
  # 프론트엔드 개발 이미지 빌드
  echo -e "${GREEN}프론트엔드 개발 이미지를 빌드합니다...${NC}"
  docker build -t sss-frontend-dev:latest -f FE/Dockerfile.dev ./FE
fi

# 배포 환경 이미지 빌드 여부 확인
if [ "$NON_INTERACTIVE" = false ]; then
  read -p "배포 환경 이미지를 빌드하시겠습니까? (y/n): " build_prod_input
  if [[ $build_prod_input == "y" || $build_prod_input == "Y" ]]; then
    BUILD_PROD=true
  fi
fi

# 배포 환경 이미지 빌드
if [ "$BUILD_PROD" = true ]; then
  # 백엔드 배포 이미지 빌드
  echo -e "${GREEN}백엔드 배포 이미지를 빌드합니다...${NC}"
  docker build -t sss-backend:latest -f BE/Dockerfile ./BE
  
  # 프론트엔드 배포 이미지 빌드
  echo -e "${GREEN}프론트엔드 배포 이미지를 빌드합니다...${NC}"
  docker build -t sss-frontend:latest -f FE/Dockerfile ./FE
fi

echo -e "${GREEN}이미지 빌드가 완료되었습니다.${NC}"
echo "빌드된 이미지 목록:"
docker images | grep sss 