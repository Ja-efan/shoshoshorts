import uvicorn 
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routes import novel_routes
from app.core.config import settings

app = FastAPI(
    title="Novel to Audio API",
    description="소설을 음성으로 변환하는 API 서비스",
    version="1.0.0"
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(novel_routes.router, prefix="/api/v1", tags=["novel"])

@app.get("/")
async def root():
    return {"message": "Novel to Audio API 서비스에 오신 것을 환영합니다."}
