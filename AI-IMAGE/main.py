"""
애플리케이션 실행 파일
"""

import os

from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
from starlette.responses import JSONResponse
from app.services.s3_service import s3_service
from fastapi.staticfiles import StaticFiles

# app 패키지에서 라우터 가져오기
from app.api.routes import image_routes
from app.api.routes import tests
from app.core.config import settings
from app.core.logger import app_logger

# 배포 환경인가, 개발 환경인가에 따른 docs_url, redoc_url, openapi_url을 볼 수 없도록 설정
if settings.ENV == "prod":
    docs_url = None
    redoc_url = None
    openapi_url = None
else:
    docs_url = "/docs"
    redoc_url = "/redoc"
    openapi_url = "/openapi.json"

app = FastAPI(
    title="Kling AI 이미지 생성 API",
    docs_url=docs_url,
    redoc_url=redoc_url,
    openapi_url=openapi_url,
)

# # 시작 이벤트 핸들러 등록
# app.add_event_handler("startup", startup_event)

# #화이트 리스트 IP 정의
# WHITELIST = {
#     "127.0.0.1", #개발용 로컬
#     "172.26.9.106", #EC2 서버
# }

# # 화이트리스트 관련 미들웨어
# @app.middleware("http")
# async def ip_whitelist_middleware(request: Request, call_next):
#     client_ip = request.client.host
#     if client_ip not in WHITELIST:
#         raise HTTPException(status_code=403, detail="Forbidden: IP not allowed")
#     response = await call_next(request)
#     return response


@app.middleware("http")
async def check_pwd_middleware(request: Request, call_next):
    # POST 요청에만 적용
    if request.method == "POST":
        app_logger.info("post 요청 시작")
        # 요청 headers에서 apiPwd 확인
        api_pwd = request.headers.get("apiPwd")

        # 비밀번호가 없거나 유효하지 않은 경우
        if not api_pwd:
            return JSONResponse(status_code=401, content={"message": "Missing API pwd"})

        # 개발 환경 비밀번호 확인 (dev로 시작하는 비밀번호는 개발 환경으로 인식)
        if api_pwd.startswith("dev"):
            # 개발 환경으로 설정
            s3_service._set_environment(is_dev_environment=True)
            # 유효한 개발 환경 비밀번호인지 확인
            if api_pwd != "dev" + settings.API_PWD:
                return JSONResponse(
                    status_code=401, content={"message": "Invalid development API pwd"}
                )
        elif api_pwd.startswith("prod"):
            # 프로덕션 환경으로 설정
            s3_service._set_environment(is_dev_environment=False)
            # 유효한 프로덕션 비밀번호인지 확인
            if api_pwd != "prod" + settings.API_PWD:
                return JSONResponse(
                    status_code=401, content={"message": "Invalid production API pwd"}
                )
        else:
            return JSONResponse(
                status_code=401,
                content={"message": "API pwd 앞에 dev 또는 prod가 없습니다."},
            )
    app_logger.info("Next")
    response = await call_next(request)
    return response


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
app.include_router(image_routes.router, prefix=settings.API_V1_STR)

# 테스트 라우터 등록
app.include_router(tests.router, prefix=settings.API_V1_STR)

# 정적 파일 서빙 추가
app.mount("/static/images", StaticFiles(directory="images"), name="images")


@app.get("/", response_class=HTMLResponse)
async def read_root():
    return f"Image Generation API"
