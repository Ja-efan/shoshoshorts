"""
애플리케이션 실행 파일
"""
import os
from dotenv import load_dotenv

# 환경 변수 로드
load_dotenv()

from fastapi import FastAPI
from fastapi.responses import HTMLResponse
from fastapi.middleware.cors import CORSMiddleware

# app 패키지에서 라우터 가져오기
from app.api.routes import image_routes

app = FastAPI(title="Kling AI 이미지 생성 API")

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 생성 이미지 저장을 위한 디렉토리 생성
os.makedirs("images", exist_ok=True)

# 이미지 라우터 등록
app.include_router(image_routes.router, prefix="/api/v1")

@app.get("/", response_class=HTMLResponse)
async def read_root():
    return(f"Image Generation API")