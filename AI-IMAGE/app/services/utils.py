import base64
from app.schemas.models import Scene

def encode_image_to_base64(image_path: str) -> str:
    """
    이미지 파일을 Base64로 인코딩하여 반환합니다.
    """
    with open(image_path, "rb") as image_file:
        return base64.b64encode(image_file.read()).decode('utf-8')


def check_scene(scene: Scene) -> dict:
    """
    장면 정보가 유효한지 검사하고, 유효하지 않은 경우 오류 메시지를 반환합니다.
    
    Args:
        scene: 검증할 Scene 객체
        
    Returns:
        dict: 검증 결과와 오류 메시지를 포함하는 딕셔너리
            - result: 검증 결과 (True/False)
            - validation_errors: 오류 메시지 딕셔너리
    """
    result = True 
    validation_errors = {}
    
    # Scene.scene_id 검증
    if not scene.scene_id:
        validation_errors["scene_id"] = "장면 ID가 누락되었습니다."
        result = False
    
    # Story 메타데이터 관련 검증
    if not scene.story_metadata:
        validation_errors["story_metadata"] = "스토리 메타데이터가 누락되었습니다."
        result = False
    else:
        # StoryMetadata.story_id 검증
        if not scene.story_metadata.story_id:
            validation_errors["story_id"] = "스토리 ID가 누락되었습니다."
            result = False
        
        # StoryMetadata.title 검증
        if not scene.story_metadata.title:
            validation_errors["story_title"] = "스토리 제목이 누락되었습니다."
            result = False
            
        # StoryMetadata.original_story 검증
        if not scene.story_metadata.original_story:
            validation_errors["original_story"] = "원본 스토리가 누락되었습니다."
            result = False
            
        # StoryMetadata.characters 검증
        if not scene.story_metadata.characters:
            validation_errors["characters"] = "등장 인물 정보가 누락되었습니다."
            result = False
        else:
            # 각 캐릭터 정보 검증
            for idx, character in enumerate(scene.story_metadata.characters):
                if not character.name:
                    validation_errors[f"character_{idx}_name"] = f"{idx+1}번째 등장인물의 이름이 누락되었습니다."
                    result = False
                if character.gender not in [0, 1]:
                    validation_errors[f"character_{idx}_gender"] = f"{idx+1}번째 등장인물의 성별이 유효하지 않습니다. (0: 남자, 1: 여자)"
                    result = False
    
    # Scene.audios 검증
    if not scene.audios or len(scene.audios) == 0:
        validation_errors["audios"] = "장면 오디오 정보가 누락되었습니다."
        result = False
    else:
        # 각 오디오 정보 검증
        for idx, audio in enumerate(scene.audios):
            if not audio.type:
                validation_errors[f"audio_{idx}_type"] = f"{idx+1}번째 오디오의 유형이 누락되었습니다."
                result = False
            if not audio.text:
                validation_errors[f"audio_{idx}_text"] = f"{idx+1}번째 오디오의 텍스트가 누락되었습니다."
                result = False
            
            # 대화 유형일 경우 character 필드 검증
            if audio.type == "dialogue" and not audio.character:
                validation_errors[f"audio_{idx}_character"] = f"{idx+1}번째 대화 오디오의 캐릭터 정보가 누락되었습니다."
                result = False

    return {"result": result, "validation_errors": validation_errors}
