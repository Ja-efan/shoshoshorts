from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
from TTS.api import TTS
from app.core.config import settings
import torch

class NovelService:
    def __init__(self):
        self.tokenizer = AutoTokenizer.from_pretrained(settings.LLM_MODEL_NAME)
        self.model = AutoModelForSeq2SeqLM.from_pretrained(settings.LLM_MODEL_NAME)
        self.tts = TTS(model_name=settings.TTS_MODEL_NAME)

    async def convert_to_script(self, content: str) -> list:
        """
        소설 내용을 스크립트 형태로 변환
        """
        inputs = self.tokenizer(content, return_tensors="pt", max_length=512, truncation=True)
        outputs = self.model.generate(**inputs, max_length=512)
        script = self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        return self._format_script(script)

    async def generate_audio(self, script: list, output_path: str) -> str:
        """
        스크립트를 음성으로 변환
        """
        for idx, scene in enumerate(script):
            audio_path = f"{output_path}/scene_{idx}.wav"
            self.tts.tts_to_file(text=scene["text"], file_path=audio_path)
        return output_path

    def _format_script(self, script: str) -> list:
        """
        스크립트를 적절한 형식으로 변환
        """
        # 여기에 스크립트 포맷팅 로직 구현
        scenes = script.split("\n")
        formatted_script = []
        for scene in scenes:
            if scene.strip():
                formatted_script.append({
                    "text": scene.strip(),
                    "type": "narration"
                })
        return formatted_script 