# ---------- 1단계: build ----------
    FROM node:22-alpine AS build

    WORKDIR /app
    
    # 의존성 설치
    COPY package*.json ./
    RUN npm ci
    
    # 소스 + 환경변수
    COPY . .
    COPY .env.production .
    
    # Vite 빌드 (production mode)
    RUN npm run build
    
    # ---------- 2단계: nginx ----------
    FROM nginx:1.25-alpine
    
    # 빌드 산출물 복사
    COPY --from=build /app/dist /usr/share/nginx/html
    
    # 필요하면 커스텀 nginx conf 복사
    COPY nginx/default.conf /etc/nginx/conf.d/default.conf
    
    # 인증서 디렉터리 마운트용
    VOLUME ["/etc/letsencrypt"]
    
    EXPOSE 80 443
    CMD ["nginx", "-g", "daemon off;"]