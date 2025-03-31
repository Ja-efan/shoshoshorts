class KlingAIConfig:
    MODEL = "kling-v1-5"
    ASPECT_RATIO = "1:1"
    N = 1
    KLING_TO_HTTP = {
        # 200 OK
        0: 200,
        
        # 401 Unauthorized
        1000: 401, 1001: 401, 1002: 401, 1003: 401, 1004: 401,
        
        # 429 Too Many Requests
        1100: 429, 1101: 429, 1102: 429, 1302: 429, 1303: 429, 1304: 429,
        
        # 403 Forbidden
        1103: 403,
        
        # 400 Bad Request
        1200: 400, 1201: 400, 1300: 400, 1301: 400,
        
        # 404 Not Found
        1202: 404, 1203: 404,
        
        # 500 Internal Server Error
        5000: 500,
        
        # 503 Service Unavailable
        5001: 503,
        
        # 504 Gateway Timeout
        5002: 504
    }

class OpenAIConfig:
    MODEL = "gpt-4o"  # "gpt-4o-mini"
    MAX_TOKENS = 500
    TEMPERATURE = 0.5
    KLINGAI_SYSTEM_PROMPT = "klingai-v02.txt"

klingai_config = KlingAIConfig()
openai_config = OpenAIConfig()