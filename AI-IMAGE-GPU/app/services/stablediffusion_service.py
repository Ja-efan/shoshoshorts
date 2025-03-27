import os
import time 
import pytz
from datetime import datetime
from diffusers import DiffusionPipeline
from app.core.config import settings
from app.core.sd_config import sd_config

class StableDiffusionService:
    
    # TODO: 이미지 생성 및 로컬에 저장 
    @staticmethod
    def generate_image(
        story_id: int,
        scene_id: int,
        bucket_name: str,
        style: str,
        prompt: str,
        negative_prompt: str | None = None,
        sampler: str | None = None,
        cfg_scale: float | None = None,
        steps: int | None = None,
        clip_skip: int | None = None
    ) -> str:
        """이미지 생성하고 로컬에 저장 -> 저장된 이미지의 경로 반환 

        Args:
            prompt (str): 이미지 생성 프롬프트
            negative_prompt (str | None): 이미지 생성 부정 프롬프트 
        Returns:
            str: 이미지 로컬 저장 경로 
        """
        
        """
        # crayon_style_lora_sdxl 사용 시 
        pipe = DiffusionPipeline.from_pretrained("stabilityai/stable-diffusion-xl-base-1.0")
        pipe.load_lora_weights("ostris/crayon_style_lora_sdxl")

        prompt = "an ant holding up a sign that says crayons"
        image = pipe(prompt).images[0]
        """
        
        if style == "ghibli":
            pipe = DiffusionPipeline.from_pretrained(sd_config.base_model, cache_dir=sd_config.cache_dir)
            pipe.load_lora_weights(sd_config.ghibli_lora, cache_dir=sd_config.cache_dir)
            pipe = pipe.to(sd_config.device)
            image = pipe(
                prompt=prompt + "StdGBRedmAF, Studio Ghibli",  
                negative_prompt=negative_prompt,
                sampler=sampler,
                cfg_scale=cfg_scale,
                steps=steps,
                clip_skip=clip_skip
            ).images[0]
            
        elif style == "crayon":
            pipe = DiffusionPipeline.from_pretrained(sd_config.base_model, cache_dir=sd_config.cache_dir)
            pipe.load_lora_weights(sd_config.crayon_lora, cache_dir=sd_config.cache_dir)
            pipe = pipe.to(sd_config.device)
            image = pipe(
                prompt=prompt + "crayon",  
                negative_prompt=negative_prompt,
                sampler=sampler,
                cfg_scale=cfg_scale,
                steps=steps,
                clip_skip=clip_skip
            ).images[0]
        
        # 이미지 저장 경로 생성 
        formatted_story_id = f"{story_id:08d}"  # 8자리 (예: 00000001)
        formatted_scene_id = f"{scene_id:04d}"  # 4자리 (예: 0001)

        # 기본 디렉토리 설정 (AI-IMAGE/{bucket_name})
        s3_bucket_name = bucket_name

        # 버킷 이름에서 프로토콜과 슬래시 제거
        if s3_bucket_name and s3_bucket_name.startswith("s3://"):
            s3_bucket_name = s3_bucket_name.replace("s3://", "")
        # 끝의 슬래시 제거
        if s3_bucket_name and s3_bucket_name.endswith("/"):
            s3_bucket_name = s3_bucket_name.rstrip("/")

        base_dir = os.path.join(settings.IMAGE_SAVE_PATH, s3_bucket_name)
        os.makedirs(base_dir, exist_ok=True)

        # 스토리 ID 기준 디렉토리
        story_dir = os.path.join(base_dir, formatted_story_id)
        os.makedirs(story_dir, exist_ok=True)
        
        # 스토리 내 이미지 디렉토리 생성
        images_dir = os.path.join(story_dir, "images")
        os.makedirs(images_dir, exist_ok=True)  
        
        # 현재 타임스탬프 (한국 시간, KST)
        kst = pytz.timezone('Asia/Seoul')
        now = datetime.now(kst)
        timestamp = now.strftime("%Y%m%d_%H%M%S")
        
        # 저장할 파일명 생성 (scene_id와 타임스탬프 사용)
        filename = f"{formatted_scene_id}_{timestamp}.jpg"
        save_path = os.path.join(images_dir, filename)

        # 이미지 저장
        image.save(save_path)

        return save_path


        
stablediffusion_service = StableDiffusionService()