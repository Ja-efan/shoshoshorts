import torch

class StableDiffusionConfig:
    cache_dir = "app/.cache/diffusers"
    device = "cuda" if torch.cuda.is_available() else "cpu"
    base_model = "stabilityai/stable-diffusion-xl-base-1.0"

    # LoRA
    ghibli_lora = "artificialguybr/StudioGhibli.Redmond-V2"  # ghibli
    crayon_lora = "ostris/crayon_style_lora_sdxl"  # crayon
    
sd_config = StableDiffusionConfig()