from fastapi import APIRouter, HTTPException
from app.schema.script import ScriptRequest, ScriptResponse
from app.service.script_service import ScriptService
import traceback

router = APIRouter(tags=["스크립트"], prefix="/script")

script_service = ScriptService()

@router.post("/convert", response_model=ScriptResponse)
async def convert_script(request: ScriptRequest):
    """
    스토리 내용을 JSON 형식으로 변환하는 API 엔드포인트
    
    Args:
        request (ScriptRequest): 스토리 요청 객체
        
    Returns:
        ScriptResponse: 변환된 스토리 JSON
    """
    try:
        # 요청 데이터 로깅
        print("원본 요청 데이터:")
        print(request)
        
        # 요청 객체 필드 로깅
        print("요청 객체 필드:")
        print(f"storyId: {request.storyId}, 타입: {type(request.storyId)}")
        print(f"storyTitle: {request.storyTitle}")
        print(f"characterArr 길이: {len(request.characterArr)}")
        print(f"story 길이: {len(request.story)}")
        
        # 스크립트 변환 함수 호출
        response = await script_service.generate_script_json(request)
        return response
    
    except Exception as e:
        print("스크립트 변환 중 오류 발생:")
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"스크립트 변환 중 오류 발생: {str(e)}") 