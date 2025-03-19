from fastapi import APIRouter, HTTPException
from app.schemas.novel import NovelInput, NovelResponse, ScriptOutput, AudioOutput
from app.services.novel_service import NovelService
import os
import uuid

router = APIRouter()
novel_service = NovelService()

@router.post("/convert-to-script", response_model=NovelResponse)
async def convert_to_script(novel: NovelInput):
    try:
        script = await novel_service.convert_to_script(novel.content)
        return NovelResponse(
            status="success",
            message="스크립트 변환이 완료되었습니다.",
            data={"script": script, "title": novel.title or "제목 없음"}
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/generate-audio", response_model=NovelResponse)
async def generate_audio(script_data: ScriptOutput):
    try:
        # 고유한 작업 ID 생성
        job_id = str(uuid.uuid4())
        output_dir = f"output/{job_id}"
        os.makedirs(output_dir, exist_ok=True)
        
        # 오디오 생성
        audio_path = await novel_service.generate_audio(script_data.script, output_dir)
        
        return NovelResponse(
            status="success",
            message="오디오 생성이 완료되었습니다.",
            data={
                "audio_url": f"/audio/{job_id}",
                "title": script_data.title
            }
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e)) 