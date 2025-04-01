import base64


def encode_image_to_base64(image_path: str) -> str:
    """
    이미지 파일을 Base64로 인코딩하여 반환합니다.
    """
    with open(image_path, "rb") as image_file:
        return base64.b64encode(image_file.read()).decode('utf-8')

