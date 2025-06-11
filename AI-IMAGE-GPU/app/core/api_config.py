class KlingAIConfig:
    MODEL = "kling-v1-5"
    ASPECT_RATIO = "1:1"
    N = 1

class OpenAIKlingAIConfig:
    MODEL = "gpt-4o"  # "gpt-4o-mini"
    MAX_TOKENS = 500
    TEMPERATURE = 0.5
    
class OpenAIStableDiffusionConfig:
    MODEL = "gpt-4o"  # "gpt-4o-mini"
    MAX_COMPLETION_TOKENS = 2000
    MAX_TOKENS = 2000
    TEMPERATURE = 0.5
    SYSTEM_PROMPT_PATH = "app/prompts/system_prompt/stable_diffusion-v02.txt"

klingai_config = KlingAIConfig()
openai_klingai_config = OpenAIKlingAIConfig()
openai_stablediffusion_config = OpenAIStableDiffusionConfig()