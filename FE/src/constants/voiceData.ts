// Zonos 음성 파일 import
import zonosMale1 from "@/assets/voices/zonos/male/male1.mp3"
import zonosMale2 from "@/assets/voices/zonos/male/male2.mp3"
import zonosMale3 from "@/assets/voices/zonos/male/male3.mp3"
import zonosMale4 from "@/assets/voices/zonos/male/male4.mp3"
import zonosFemale1 from "@/assets/voices/zonos/female/female1.mp3"
import zonosFemale2 from "@/assets/voices/zonos/female/female2.mp3"
import zonosFemale3 from "@/assets/voices/zonos/female/female3.mp3"
import zonosFemale4 from "@/assets/voices/zonos/female/female4.mp3"

// ElevenLabs 음성 파일 import
import elevenLabsMale1 from "@/assets/voices/elevenlabs/male/male1.mp3"
import elevenLabsMale2 from "@/assets/voices/elevenlabs/male/male2.mp3"
import elevenLabsMale3 from "@/assets/voices/elevenlabs/male/male3.mp3"
import elevenLabsMale4 from "@/assets/voices/elevenlabs/male/male4.mp3"
import elevenLabsFemale1 from "@/assets/voices/elevenlabs/female/female1.mp3"
import elevenLabsFemale2 from "@/assets/voices/elevenlabs/female/female2.mp3"
import elevenLabsFemale3 from "@/assets/voices/elevenlabs/female/female3.mp3"
import elevenLabsFemale4 from "@/assets/voices/elevenlabs/female/female4.mp3"

// 모델 로고 import
import zonosLogo from "@/assets/models/zonos_logo.svg";
import elevenLabsLogo from "@/assets/models/elevenlabs_logo.png";
import klingLogo from "@/assets/models/kling_logo.png";
import stableDiffusionLogo from "@/assets/models/stableDiffuson_logo.png";

import { VoiceFileKey, ModelType } from "@/types/voice";

// 음성 파일 매핑
export const voiceFiles: Record<string, Record<"male" | "female", Record<VoiceFileKey, string>>> = {
  "Zonos": {
    male: {
      male1: zonosMale1,
      male2: zonosMale2,
      male3: zonosMale3,
      male4: zonosMale4,
      female1: "",
      female2: "",
      female3: "",
      female4: ""
    },
    female: {
      female1: zonosFemale1,
      female2: zonosFemale2,
      female3: zonosFemale3,
      female4: zonosFemale4,
      male1: "",
      male2: "",
      male3: "",
      male4: ""
    }
  },
  "ElevenLabs": {
    male: {
      male1: elevenLabsMale1,
      male2: elevenLabsMale2,
      male3: elevenLabsMale3,
      male4: elevenLabsMale4,
      female1: "",
      female2: "",
      female3: "",
      female4: ""
    },
    female: {
      female1: elevenLabsFemale1,
      female2: elevenLabsFemale2,
      female3: elevenLabsFemale3,
      female4: elevenLabsFemale4,
      male1: "",
      male2: "",
      male3: "",
      male4: ""
    }
  }
};

// 기본 음성 모델
export const defaultVoiceModels: ModelType[] = [
  {name: "ElevenLabs", logo: elevenLabsLogo, isSelected: true },
  { name: "Zonos", logo: zonosLogo, isSelected: false }
];

// 기본 이미지 모델
export const defaultImageModels: ModelType[] = [
  { name: "Kling", logo: klingLogo, isSelected: true },
  { name: "Stable Diffusion", logo: stableDiffusionLogo, isSelected: false },
];

// 모델 설명
export const modelDescriptions = {
  "Zonos": "Zonos는 자연스러운 한국어 음성을 생성하는 모델입니다. 다양한 감정과 톤을 표현할 수 있어 캐릭터의 성격을 잘 살릴 수 있습니다.",
  "ElevenLabs": "ElevenLabs는 고품질 음성 합성 모델로, 더 자연스럽고 감정이 풍부한 음성을 제공합니다. 긴 문장에서도 일관된 톤을 유지합니다."
};

// 음성 코드 매핑
export const voiceCodes: Record<string, Record<"male" | "female", string[]>> = {
  Zonos: {
    male: [
      "pNInz6obpgDQGcFmaJgB",
      "ThT5KcBeYPX3keUQqHPh",
      "yoZ06aMxZJJ28mfd3POQ",
      "VR6AewLTigWG4xSOukaG",
    ],
    female: [
      "EXAVITQu4vr4xnSDxMaL",
      "21m00Tcm4TlvDq8ikWAM",
      "AZnzlk1XvdvUeBnXmlld",
      "D38z5RcWu1voky8WS1ja",
    ],
  },
  ElevenLabs: {
    male: [
      "pNInz6obpgDQGcFmaJgB",
      "ThT5KcBeYPX3keUQqHPh",
      "yoZ06aMxZJJ28mfd3POQ",
      "VR6AewLTigWG4xSOukaG",
    ],
    female: [
      "EXAVITQu4vr4xnSDxMaL",
      "21m00Tcm4TlvDq8ikWAM",
      "AZnzlk1XvdvUeBnXmlld",
      "D38z5RcWu1voky8WS1ja",
    ],
  },
}; 