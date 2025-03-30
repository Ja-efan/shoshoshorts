#base 64

import os
import uvicorn
import argparse
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from starlette.responses import JSONResponse

# 라우터 임포트
from app.routes.script_routes import router as script_router
from app.routes.elevenlabs_routes import router as elevenlabs_router

# S3 환경 설정정
from app.service.s3 import set_environment

load_dotenv()
ENV = os.getenv("ENV", "development")  # 기본값은 development
API_PWD = os.getenv("API_PWD")

#화이트 리스트 IP 정의
WHITELIST = {
    "127.0.0.1", #개발용 로컬
    "172.26.9.106", #EC2 서버
}

if ENV == "prod":
    docs_url = None
    redoc_url = None
    openapi_url = None
else:
    docs_url = "/docs"
    redoc_url = "/redoc"
    openapi_url = "/openapi.json"

app = FastAPI(
    title="elevenlabs TTS 및 script 생성 API",
    description="텍스트를 음성으로 변환하는 TTS 모델 및 script 생성 API",
    version="1.0.0",
    docs_url = docs_url,
    redoc_url = redoc_url,
    openapi_url = openapi_url
)

# 시작 이벤트 핸들러 등록
# app.add_event_handler("startup", startup_event)

# 화이트리스트 관련 미들웨어
# @app.middleware("http")
# async def ip_whitelist_middleware(request: Request, call_next):
#     client_ip = request.client.host
#     if client_ip not in WHITELIST:
#         raise HTTPException(status_code=403, detail="Forbidden: IP not allowed")
#     response = await call_next(request)
#     return response

# 비밀번호 관련 middleware
# @app.middleware("http")
# async def check_pwd_middleware(request: Request, call_next):
#     # POST 요청에만 적용
#     if request.method == "POST":
#         api_pwd = request.headers.get("apiPwd")
        
#         # 비밀번호가 없거나 유효하지 않은 경우
#         if not api_pwd:
#             return JSONResponse(
#                 status_code=401,
#                 content={"message": "Missing API pwd"}
#             )
        
#         # 개발 환경 비밀번호 확인 (dev로 시작하는 비밀번호는 개발 환경으로 인식)
#         if api_pwd.startswith("dev"):
#             # 개발 환경으로 설정
#             set_environment(is_dev_environment=True)
#             # 유효한 개발 환경 비밀번호인지 확인
#             if api_pwd != "dev"+API_PWD:
#                 return JSONResponse(
#                     status_code=401,
#                     content={"message": "Invalid development API pwd"}
#                 )
#         elif api_pwd.startswith("prod"):
#             # 프로덕션 환경으로 설정
#             set_environment(is_dev_environment=False)
#             # 유효한 프로덕션 비밀번호인지 확인
#             if api_pwd != "prod"+API_PWD:
#                 return JSONResponse(
#                     status_code=401,
#                     content={"message": "Invalid production API pwd"}
#                 )
#         else:
#             return JSONResponse(
#                     status_code=401,
#                     content={"message": "API pwd 앞에 dev 또는 prod가 없습니다."}
#                 )

#     response = await call_next(request)
#     return response

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 모든 오리진 허용 (프로덕션에서는 특정 오리진만 허용하는 것이 좋음)
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(script_router)
app.include_router(elevenlabs_router)

if __name__ == "__main__":
    # 명령행 인자 파싱
    parser = argparse.ArgumentParser(description="API 서버")
    parser.add_argument(
        "--model", 
        type=str, 
        # default="Zyphra/Zonos-v0.1-transformer",
        # choices=["Zyphra/Zonos-v0.1-transformer", "Zyphra/Zonos-v0.1-hybrid"],
        help="사용할 모델 타입 (transformer 또는 hybrid)"
    )
    parser.add_argument("--host", type=str, default="0.0.0.0", help="서버 호스트 주소")
    parser.add_argument("--port", type=int, default=8000, help="서버 포트 번호")
    parser.add_argument("--reload", action="store_true", help="코드 변경 시 서버 자동 재시작")
    
    args = parser.parse_args()
    
    # 명령행에서 지정한 모델을 환경 변수로 설정
    # os.environ["ZONOS_DEFAULT_MODEL"] = args.model

    # 서버 실행
    uvicorn.run(
        "__main__:app",  # main:app 대신 __main__:app 사용
        host=args.host, 
        port=args.port, 
        reload=args.reload
    )
