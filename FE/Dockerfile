# FE/Dockerfile (수정 후)
FROM sss-frontend-base:latest AS build
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build