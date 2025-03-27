"""
OpenAI 서비스
"""
import json
from openai import OpenAI
from app.schemas.models import Scene, GenerateImageStableDiffusionResponse
from app.core.config import settings    
from app.core.api_config import openai_stablediffusion_config
from app.core.logger import app_logger

# OpenAI API 키 가져오기
api_key = settings.OPENAI_API_KEY
if not api_key:
    raise ValueError("OPENAI_API_KEY가 .env 파일에 설정되어 있지 않습니다.")

# OpenAI 클라이언트 초기화
client = OpenAI(api_key=api_key)



class OpenAIService:
    """OpenAI API를 활용하여 이미지 프롬프트를 생성하는 서비스
    
    - 이미지 프롬프트 생성(Stable Diffusion)
    """
    
    @staticmethod
    async def generate_image_prompt_stable_diffusion(scene: Scene) -> GenerateImageStableDiffusionResponse:
        """
        장면 정보를 바탕으로 이미지 생성(Stable Diffusion)에 사용할 프롬프트를 생성합니다.
        """
        system_prompt = open(openai_stablediffusion_config.SYSTEM_PROMPT_PATH, "r").read()
        # app_logger.info(f"System Prompt: {system_prompt}")

        # 프롬프트 생성을 위한 컨텍스트 구성
        context = {
            "title": scene.story_metadata.title,
            "characters": [],
            "audios": []
        }
        
        # 등장인물 정보 추가
        for character in scene.story_metadata.characters:
            character_info = {
                "name": character.name,
                "gender": "남자" if character.gender == 0 else "여자",
                "description": character.description
            }
            context["characters"].append(character_info)
        
        # 오디오 정보 추가
        for audio in scene.audios:
            audio_info = {
                "type": audio.type,
                "text": audio.text
            }
            
            if audio.type == "dialogue" and audio.character:
                audio_info["character"] = audio.character
                audio_info["emotion"] = audio.emotion if audio.emotion else "neutral"
                
            context["audios"].append(audio_info)
        

        # OpenAI API로 보낼 프롬프트 구성 
        prompt = f"""
        등장인물: 
        """

        for char in context["characters"]:
            prompt += f"- {char['name']} ({char['gender']}): {char['description']}\n"
            
        prompt += "\n장면 내용:\n"
        
        for audio in context["audios"]:
            if audio["type"] == "narration":
                prompt += f"[내레이션] {audio['text']}\n"
            elif audio["type"] == "dialogue":
                emotion = audio.get("emotion", "")
                emotion_text = f" ({emotion})" if emotion else ""
                prompt += f"[대사] {audio.get('character', '알 수 없음')}{emotion_text}: {audio['text']}\n"
            elif audio["type"] == "sound":
                prompt += f"[효과음] {audio['text']}\n"
                
        # TODO  few-shot 예제 추가
        
        app_logger.debug(f"장면 정보: {prompt}")
        
        try:
            # OpenAI API 호출 - Structured Output 사용 
            response = client.beta.chat.completions.parse(
                model=openai_stablediffusion_config.MODEL,
                messages=[
                    {
                        "role": "system", 
                        "content": system_prompt
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                max_tokens=openai_stablediffusion_config.MAX_TOKENS,
                temperature=openai_stablediffusion_config.TEMPERATURE,
                response_format=GenerateImageStableDiffusionResponse
            )
            
            app_logger.debug(f"OpenAI API 응답: {response}")
            
            # JSON 문자열을 파싱하여 딕셔너리로 변환
            content_dict = json.loads(response.choices[0].message.content)

            prompt = content_dict["prompt"]
            negative_prompt = content_dict["negative_prompt"]
            sampler = content_dict["sampler"]
            cfg_scale = content_dict["cfg_scale"]
            steps = content_dict["steps"]
            clip_skip = content_dict["clip_skip"]

            app_logger.debug(f"생성된 프롬프트: {prompt}")
            app_logger.debug(f"생성된 네거티브 프롬프트: {negative_prompt}")
            app_logger.debug(f"생성된 샘플러: {sampler}")
            app_logger.debug(f"생성된 CFG 스케일: {cfg_scale}")
            app_logger.debug(f"생성된 스텝: {steps}")
            app_logger.debug(f"생성된 클립 스킵: {clip_skip}")
            
            result = GenerateImageStableDiffusionResponse(
                prompt=prompt, 
                negative_prompt=negative_prompt, 
                sampler=sampler, 
                cfg_scale=cfg_scale, 
                steps=steps, 
                clip_skip=clip_skip
            )
            
            return result
            
        
        except Exception as e:
            app_logger.error(f"OpenAI API 오류: {str(e)}")

            # TODO: 기본 프롬프트 생성 
            default_prompt = f"""

            """
            return default_prompt

# 서비스 인스턴스 생성
openai_service = OpenAIService() 