"""
애플리케이션 로깅 설정
"""

import logging
import colorlog
import datetime
import pytz


def setup_logger(name: str):
    """FastAPI 기본 로그와 충돌하지 않는 색상으로 설정된 로거"""
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)

    # ✅ 기존 핸들러가 있다면 중복 추가 방지
    if logger.hasHandlers():
        return logger  # 기존 로거를 그대로 반환하여 중복 방지

    # 컬러 로그 설정
    handler = logging.StreamHandler()

    # 한국 시간(KST) 설정을 위한 Formatter 커스텀 클래스
    class KSTFormatter(colorlog.ColoredFormatter):
        def formatTime(self, record, datefmt=None):
            # UTC 시간을 KST(한국 표준시)로 변환
            dt = datetime.datetime.fromtimestamp(record.created, tz=pytz.UTC)
            kst_dt = dt.astimezone(pytz.timezone('Asia/Seoul'))
            if datefmt:
                return kst_dt.strftime(datefmt)
            else:
                return kst_dt.strftime("%Y-%m-%d %H:%M:%S")

    formatter = KSTFormatter(
        "%(log_color)s%(levelname)-8s%(reset)s - %(asctime)s - %(filename)s:%(lineno)d - %(name)s -  %(message)s",
        log_colors={
            "DEBUG": "blue",
            "INFO": "green",
            "WARNING": "yellow",
            "ERROR": "red",
            "CRITICAL": "bold_red",
        },
        datefmt="%Y-%m-%d %H:%M:%S"
    )

    handler.setFormatter(formatter)
    logger.addHandler(handler)

    return logger


# 애플리케이션에서 공통적으로 사용할 전역 로거
app_logger = setup_logger("app")
