class KlingAIConfig:
    MODEL = "kling-v1-5"
    ASPECT_RATIO = "1:1"
    N = 1

class OpenAIConfig:
    MODEL = "gpt-4o"  # "gpt-4o-mini"
    MAX_TOKENS = 500
    TEMPERATURE = 0.5

klingai_config = KlingAIConfig()
openai_config = OpenAIConfig()