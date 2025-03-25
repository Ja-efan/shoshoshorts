# 빌드 스테이지
FROM sss-frontend-base:latest AS build
WORKDIR /app

# 패키지 파일 복사 및 의존성 설치
COPY package*.json ./
RUN npm install

# 소스 코드 복사
COPY . .

# 애플리케이션 빌드
RUN npm run build

# 실행 스테이지
FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

# 빌드 결과물 복사
COPY --from=build /app/dist .

# Nginx 설정 파일 복사
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 포트 노출
EXPOSE 80

# 환경 변수 설정
ENV NODE_ENV=production

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"] 