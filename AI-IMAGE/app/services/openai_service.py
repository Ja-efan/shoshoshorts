"""
OpenAI 서비스
"""
import os
from enum import Enum
from openai import OpenAI
from app.schemas.models import Scene, SceneInfo
from app.core.config import settings    
from app.core.logger import app_logger
from app.core.api_config import openai_config
from typing import Union, Tuple

# OpenAI API 키 가져오기
api_key = settings.OPENAI_API_KEY
if not api_key:
    raise ValueError("OPENAI_API_KEY가 .env 파일에 설정되어 있지 않습니다.")

# OpenAI 클라이언트 초기화
client = OpenAI(api_key=api_key)


class ImageStyle(str, Enum):
    """이미지 스타일 열거형"""
    ANIME = "ANIME"
    REALISM = "REALISM"
    ARTISTIC = "ARTISTIC"
    CONCEPTUAL = "CONCEPTUAL"
    RETRO = "RETRO/LOW-FI"
    GENRE = "GENRE"
    LIGHTING = "LIGHTING"
    MOOD = "MOOD"
    GHIBLI = "GHIBLI"
    DISNEY = "DISNEY"
    
    @classmethod
    def _missing_(cls, value):
        """대소문자 구분 없이 Enum 값 찾기"""
        if isinstance(value, str):
            # 문자열을 대문자로 변환하여 비교
            for member in cls:
                if member.value.upper() == value.upper():
                    return member
        return None


class OpenAIService:
    """OpenAI API를 활용하여 이미지 프롬프트를 생성하는 서비스"""
    @staticmethod 
    def get_negative_prompt(style: ImageStyle) -> str:
        """
        이미지 스타일에 따른 부정 프롬프트를 반환합니다.
        """
        if style == ImageStyle.GHIBLI:
            return "Disney Style"
        elif style == ImageStyle.DISNEY:
            return "Studio Ghibli Style, anime-style"
        return "low quality, bad anatomy, blurry, pixelated, disfigured"
    
    @staticmethod
    async def generate_scene_info(scene: Scene, style: ImageStyle="DISNEY"):
        """
        장면 정보를 바탕으로 이미지 프롬프트 생성에 필요한 장면 정보(scene_info)를 생성합니다.
        """
        system_prompt ="""
            <system_prompt>
            YOU ARE AN EXPERT ANIME SCRIPT ANALYZER.

            YOUR TASK IS TO READ A KOREAN SCENE DESCRIPTION THAT INCLUDES:
            - SCENE TITLE, SCENE ID, STYLE  
            - CHARACTER LIST  
            - SCENE CONTENT (IN KOREAN)

            AND THEN YOU MUST:

            ### TASKS ###

            1. ANALYZE the scene content to IDENTIFY which characters ACTUALLY APPEAR (either through dialogue or narration).
            2. REMOVE any characters from the character list that DO NOT appear.
            3. INCLUDE the FULL SCENE CONTENT in the output.
            4. GENERATE a ONE-SENTENCE SCENE SUMMARY IN KOREAN.
            5. OUTPUT EVERYTHING IN KOREAN using the structure below.

            ---

            ### OUTPUT FORMAT (ALL OUTPUT IN KOREAN) ###

            **등장인물**:  
            - [이름] ([성별]): [설명]  
            - ...  

            **장면 내용**:  
            [원본 장면 내용 그대로 출력]

            **장면 요약**:  
            [한 문장 요약, 한국어로]

            **장면 메타데이터**:  
            - 제목: [장면 제목]  
            - ID: [장면 ID]  
            - 스타일: [장면 스타일]

            ---

            ### CHAIN OF THOUGHT ###

            1. UNDERSTAND the full input (metadata, characters, content).
            2. EXTRACT names of characters who are directly involved via narration or dialogue.
            3. REMOVE characters from the list who do not appear or speak.
            4. INTERPRET the emotion or key event in the scene.
            5. SUMMARIZE the scene in ONE SHORT SENTENCE in Korean.
            6. OUTPUT all data in Korean using the specified format.

            ---

            ### WHAT NOT TO DO ###

            - DO NOT INCLUDE characters who are not explicitly mentioned in narration or dialogue.  
            - DO NOT OUTPUT anything in English — final output MUST BE IN KOREAN ONLY.  
            - DO NOT OMIT the scene content, summary, or metadata.  
            - DO NOT HALLUCINATE character involvement based on the style or title.  
            - DO NOT MAKE THE SUMMARY LONGER THAN ONE SENTENCE.  

            ---

            ### 예시 입력 ###

            장면 제목: 빵타지아 입사시험  
            ID: 2  
            스타일: GHIBLI  
            등장인물:  
            - 감독관 (남자): 중년 남성, 검은 머리, 사나운 인상, 요리사 복장  
            - 여자 (여자): 10대 소녀, 핑크머리, 단발머리에 양갈래 묶은 머리, 요리사 복장  
            - 신태양 (남자): 10대 소년, 갈색머리, 올빽머리에 머리띠, 이마가 넓은 편, 요리사 복장을 함  

            장면 내용:  
            [내레이션] 그러자 여자는 반박했다.  
            [대사] 여자 (frustration): 감독관님, 제 메론빵은 먹어보지도 않았잖아요. 이 메론빵은 10만원짜리 메론으로 만든거라구요!

            ---

            ### 예시 출력 ###

            **등장인물**:  
            - 감독관 (남자): 중년 남성, 검은 머리, 사나운 인상, 요리사 복장  
            - 여자 (여자): 10대 소녀, 핑크머리, 단발머리에 양갈래 묶은 머리, 요리사 복장  

            **장면 내용**:  
            [내레이션] 그러자 여자는 반박했다.  
            [대사] 여자 (frustration): 감독관님, 제 메론빵은 먹어보지도 않았잖아요. 이 메론빵은 10만원짜리 메론으로 만든거라구요!

            **장면 요약**:  
            감독관이 메론빵을 평가하지 않자 여자가 억울한 마음으로 항의한다.

            **장면 메타데이터**:  
            - 제목: 빵타지아 입사시험  
            - ID: 2  
            - 스타일: GHIBLI
            </system_prompt>

        """
        # 장면 정보 생성
        scene_info = f"""
            장면 제목: {scene.story_metadata.title}
            장면 ID: {scene.scene_id}
            장면 스타일: {style}

            등장인물:
        """
        
        for char in scene.story_metadata.characters:
            gender = "남자" if char.gender == 0 else "여자"
            scene_info += f"- {char.name} ({gender}): {char.description}\n"
            
        scene_info += "\n장면 내용:\n"
        
        for audio in scene.audios:
            if audio.type == "narration":
                scene_info += f"[내레이션] {audio.text}\n"
            elif audio.type == "dialogue":
                emotion_text = f" ({audio.emotion})" if audio.emotion else ""
                scene_info += f"[대사] {audio.character}{emotion_text}: {audio.text}\n"
            elif audio.type == "sound":
                scene_info += f"[효과음] {audio.text}\n"
                
        # 장면 정보 생성 (gpt-4o-mini)
        response = client.beta.chat.completions.parse(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": scene_info}
            ], 
            response_format=SceneInfo
        )   

        return response.choices[0].message.parsed
    
    @staticmethod
    async def generate_image_prompt(scene: Scene, style: Union[str, ImageStyle]=ImageStyle.DISNEY) -> Tuple[str, str, SceneInfo]:
        """
        장면 정보를 바탕으로 이미지 생성(KLING AI)에 사용할 이미지 프롬프트를 생성합니다.
        
        Args:
            scene(Scene): 장면 정보
            style(Union[str, ImageStyle]): 이미지 스타일 (대소문자 구분 없이 입력 가능)
                - ANIME: anime-style, cartoon, cel-shaded, Studio Ghibli-style
                - REALISM: photorealistic, cinematic realism, ultra-realistic
                - ARTISTIC: watercolor, oil painting, pastel sketch, digital painting, hand-drawn
                - CONCEPTUAL: fantasy concept art, sci-fi illustration, character sheet
                - RETRO/LOW-FI: pixel art, 8-bit, VHS aesthetic, low-poly
                - GENRE: cyberpunk, steampunk, gothic fantasy, dark academia
                - LIGHTING: cinematic lighting, golden hour glow, soft ambient light, dramatic shadows
                - MOOD: minimalist, surreal, dreamy, vibrant, moody, textured
                - GHIBLI: Studio Ghibli-style, anime-style, cartoon, cel-shaded
                - DISNEY: Disney-style, cartoon, vibrant colors, expressive characters

        Returns(str):
            생성된 이미지 프롬프트
        """
        # 문자열로 입력된 경우 ImageStyle Enum으로 변환
        if isinstance(style, str) and not isinstance(style, ImageStyle):
            try:
                style = ImageStyle(style)
            except ValueError:
                app_logger.warning(f"유효하지 않은 이미지 스타일 '{style}'. 기본값 'DISNEY'를 사용합니다.")
                style = ImageStyle.DISNEY
        
        # 장면 정보 생성 (gpt-4o-mini)
        scene_info = await OpenAIService.generate_scene_info(scene, style)
        app_logger.debug(f"이미지 프롬프트 생성을 위해 생성된 장면 정보: \n{scene_info}")
                
        app_logger.debug(f"시스템 프롬프트 ver: {openai_config.KLINGAI_SYSTEM_PROMPT}")
        system_prompt = open(os.path.join(settings.SYSTEM_PROMPT_DIR, openai_config.KLINGAI_SYSTEM_PROMPT), "r").read()
        
        try:
            # scene_info 객체를 문자열로 변환
            scene_info_str = f"""
**등장인물**:  
{chr(10).join([f"- {char.name} ({char.gender}): {char.description}" for char in scene_info.characters])}

**장면 내용**:  
{scene_info.scene_content}

**장면 요약**:  
{scene_info.scene_summary}

**장면 메타데이터**:  
- 제목: {scene_info.scene_metadata.title}  
- ID: {scene_info.scene_metadata.scene_id}  
- 스타일: {scene_info.scene_metadata.style}
"""
            # OpenAI API 호출
            response = client.chat.completions.create(
                model=openai_config.MODEL,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": scene_info_str}
                ],
                max_tokens=openai_config.MAX_TOKENS,
                temperature=openai_config.TEMPERATURE
            )
            
            negative_prompt = OpenAIService.get_negative_prompt(style)
            image_prompt = response.choices[0].message.content.strip()
            app_logger.debug(f"Positive Prompt: \n{image_prompt}")
            # app_logger.debug(f"Positive Prompt 길이: {len(image_prompt)}")
            app_logger.debug(f"Negative Prompt: {negative_prompt}")
            
            # 생성된 프롬프트 반환
            return image_prompt, negative_prompt, scene_info
            
        except Exception as e:  
            # 오류 발생 시 기본 프롬프트 반환
            app_logger.error(f"OpenAI API 오류: {str(e)}")
            
            # 기본 프롬프트 생성
            default_prompt = f"A scene from the story '{scene.story_metadata.title}' in {style} style, showing "
            
            # 스타일별 추가 지시사항
            style_str = style.value if isinstance(style, ImageStyle) else str(style)
            if style_str.upper() == "ANIME":
                default_prompt += "with anime-style character features, cel-shaded, Studio Ghibli-style, "
            elif style_str.upper() == "REALISM":
                default_prompt += "with photorealistic details, cinematic realism, ultra-realistic, "
            elif style_str.upper() == "ARTISTIC":
                default_prompt += "with watercolor, digital painting, hand-drawn style, "
            elif style_str.upper() == "CONCEPTUAL":
                default_prompt += "with fantasy concept art, character sheet style, "
            elif style_str.upper() == "RETRO":
                default_prompt += "with pixel art, 8-bit style, low-poly, "
            elif style_str.upper() == "GENRE":
                default_prompt += "with cyberpunk, gothic fantasy elements, "
            elif style_str.upper() == "LIGHTING":
                default_prompt += "with cinematic lighting, golden hour glow, dramatic shadows, "
            elif style_str.upper() == "MOOD":
                default_prompt += "with dreamy, vibrant, textured style, "
            elif style_str.upper() == "GHIBLI":
                default_prompt += "with Studio Ghibli-style, anime-style, cartoon, cel-shaded, "
            elif style_str.upper() == "DISNEY":
                default_prompt += "with Disney-style, cartoon, vibrant colors, expressive characters, "
            
            # 등장인물 정보 추가
            characters_str = ", ".join([f"{c['name']}" for c in scene_info.characters])
            default_prompt += f"{characters_str}. "
            
            # 첫 번째 나레이션이나 대화 내용 추가
            for audio in scene.audios:
                if audio.type in ["narration", "dialogue"]:
                    default_prompt += f"{audio.text}"
                    break
                
            negative_prompt = None
            if style==ImageStyle.GHIBLI:
                negative_prompt = "Disney Style"
            elif style==ImageStyle.DISNEY:
                negative_prompt = "Studio Ghibli Style, anime-style"
            else:
                negative_prompt = "low quality, bad anatomy, blurry, pixelated, disfigured"
            
            return default_prompt, negative_prompt, scene_info

# 서비스 인스턴스 생성
openai_service = OpenAIService() 