"use client";

import { useState, useRef, useEffect } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Loader2, Play, Pause } from "lucide-react";
import { apiService } from "@/api/api";
import { useCharacter } from "@/hooks/useCharacter";
import { CharacterForm } from "@/components/create/CharacterForm";
import { StoryForm } from "@/components/create/StoryForm";
import { CurrentlyPlaying } from "@/types/character";
import shortLogo from "@/assets/short_logo.png";
import zonosLogo from "@/assets/models/zonos_logo.svg";
import elevenLabsLogo from "@/assets/models/elevenlabs_logo.png";
import klingLogo from "@/assets/models/kling_logo.png";
import stableDiffusionLogo from "@/assets/models/stableDiffuson_logo.png"
import { toast } from "react-hot-toast";
import { HelpTooltip } from "@/components/ui/help-tooltip";

// 음성 파일 import
import zonosMale1 from "@/assets/voices/zonos/male/male1.mp3"
import zonosMale2 from "@/assets/voices/zonos/male/male2.mp3"
import zonosMale3 from "@/assets/voices/zonos/male/male3.mp3"
import zonosMale4 from "@/assets/voices/zonos/male/male4.mp3"
import zonosFemale1 from "@/assets/voices/zonos/female/female1.mp3"
import zonosFemale2 from "@/assets/voices/zonos/female/female2.mp3"
import zonosFemale3 from "@/assets/voices/zonos/female/female3.mp3"
import zonosFemale4 from "@/assets/voices/zonos/female/female4.mp3"

import elevenLabsMale1 from "@/assets/voices/elevenlabs/male/male1.mp3"
import elevenLabsMale2 from "@/assets/voices/elevenlabs/male/male2.mp3"
import elevenLabsMale3 from "@/assets/voices/elevenlabs/male/male3.mp3"
import elevenLabsMale4 from "@/assets/voices/elevenlabs/male/male4.mp3"
import elevenLabsFemale1 from "@/assets/voices/elevenlabs/female/female1.mp3"
import elevenLabsFemale2 from "@/assets/voices/elevenlabs/female/female2.mp3"
import elevenLabsFemale3 from "@/assets/voices/elevenlabs/female/female3.mp3"
import elevenLabsFemale4 from "@/assets/voices/elevenlabs/female/female4.mp3"

type ModelType = {
  name: string;
  logo: string;
  isSelected: boolean;
};

type VoiceFileKey = "male1" | "male2" | "male3" | "male4" | "female1" | "female2" | "female3" | "female4";

// 음성 파일 매핑
const voiceFiles: Record<string, Record<"male" | "female", Record<VoiceFileKey, string>>> = {
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

// 모델 설명 추가
const modelDescriptions = {
  "Zonos": "Zonos는 자연스러운 한국어 음성을 생성하는 모델입니다. 다양한 감정과 톤을 표현할 수 있어 캐릭터의 성격을 잘 살릴 수 있습니다.",
  "ElevenLabs": "ElevenLabs는 고품질 음성 합성 모델로, 더 자연스럽고 감정이 풍부한 음성을 제공합니다. 긴 문장에서도 일관된 톤을 유지합니다."
};

export default function CreateVideoPage() {
  const { characters, addCharacter, updateCharacter, removeCharacter } =
    useCharacter();
  const [story, setStory] = useState("");
  const [title, setTitle] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const [currentlyPlaying, setCurrentlyPlaying] = useState<CurrentlyPlaying>({
    voiceOption: null,
    characterId: null,
  });
  const [narratorGender, setNarratorGender] = useState<"male" | "female">("male");
  const [narratorVoice, setNarratorVoice] = useState<string>("male1");
  const [voiceModels, setVoiceModels] = useState<ModelType[]>([
    {name: "ElevenLabs", logo: elevenLabsLogo, isSelected: true },
    { name: "Zonos", logo: zonosLogo, isSelected: false }
  ]);
  const [imageModels, setImageModels] = useState<ModelType[]>([
    { name: "Kling", logo: klingLogo, isSelected: true },
    { name: "Stable Diffusion", logo: stableDiffusionLogo, isSelected: false },
  ]);
  const [showModelSelector, setShowModelSelector] = useState(false);
  const modelSelectorRef = useRef<HTMLDivElement>(null);
  const [isNarratorPlaying, setIsNarratorPlaying] = useState(false);
  const narratorAudioRef = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (modelSelectorRef.current && !modelSelectorRef.current.contains(event.target as Node)) {
        setShowModelSelector(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const toggleModelSelector = () => {
    setShowModelSelector(!showModelSelector);
  };

  const selectVoiceModel = (index: number) => {
    if (currentlyPlaying.voiceOption && currentlyPlaying.characterId) {
      setCurrentlyPlaying({ voiceOption: null, characterId: null });
    }
    
    if (narratorAudioRef.current) {
      narratorAudioRef.current.pause();
      narratorAudioRef.current.currentTime = 0;
      setIsNarratorPlaying(false);
    }
    
    setVoiceModels(
      voiceModels.map((model, i) => ({
        ...model,
        isSelected: i === index,
      }))
    );
    
    toast("모델별로 목소리의 차이가 있을 수 있습니다.", {
      icon: "⚠️",
      duration: 3000,
    });
  };

  const selectImageModel = (index: number) => {
    const model = imageModels[index];
    if (model.name === "Stable Diffusion") {
      toast("아직 개발 중에 있습니다.", {
        icon: "🔨",
        duration: 2000,
      });
      return;
    }
    setImageModels(
      imageModels.map((model, i) => ({
        ...model,
        isSelected: i === index,
      }))
    );
  };

  const handleNarratorGenderChange = (gender: "male" | "female") => {
    setNarratorGender(gender);
    setNarratorVoice(`${gender}1`);
  };

  const handleNarratorVoicePlay = () => {
    if (narratorAudioRef.current) {
      narratorAudioRef.current.pause();
      narratorAudioRef.current.currentTime = 0;
      if (isNarratorPlaying) {
        setIsNarratorPlaying(false);
        return;
      }
    }

    try {
      const selectedVoiceModel = voiceModels.find(model => model.isSelected)?.name || "ElevenLabs";
      const audio = new Audio(voiceFiles[selectedVoiceModel][narratorGender][narratorVoice as VoiceFileKey]);
      audio.addEventListener('ended', () => {
        setIsNarratorPlaying(false);
      });
      audio.play().catch(error => console.error('Error playing audio:', error));
      narratorAudioRef.current = audio;
      setIsNarratorPlaying(true);
    } catch (error) {
      console.error('Error creating audio:', error);
    }
  };

  const handleGenerateVideo = async () => {
    setIsGenerating(true);

    const voiceCodes: Record<string, Record<"male" | "female", string[]>> = {
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

    const selectedVoiceModel = voiceModels.find(model => model.isSelected)?.name || "Zonos";

    const requestData: any = {
      title,
      story,
      narVoiceCode: voiceCodes[selectedVoiceModel][narratorGender][parseInt(narratorVoice.slice(-1)) - 1],
    };

    if (characters.length > 0) {
      requestData.characterArr = characters.map((character) => ({
        name: character.name,
        gender: character.gender
          ? character.gender === "male"
            ? "1"
            : "2"
          : null,
        properties: character.description || "이미지 생성을 위한 설명...",
        voiceCode:
          character.voice && character.gender
            ? voiceCodes[selectedVoiceModel][character.gender][
                parseInt(character.voice.slice(-1)) - 1
              ]
            : null,
      }));
    }

    try {
      console.log(requestData);
      const response = await apiService.createVideo(requestData);
      console.log("API Response:", response);
    } catch (error) {
      console.error("API Error:", error);
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-10 bg-white border-b">
        <div className="container flex h-16 items-center justify-between px-4">
          <div className="flex items-center gap-2">
            <Link to="/" className="flex items-center gap-2">
              <img src={shortLogo} alt="쇼쇼숓 로고" className="h-8 w-8" />
              <span className="text-xl font-bold">쇼쇼숓</span>
            </Link>
          </div>
        </div>
      </header>

      <main className="flex-1 py-8">
        <div className="container mx-auto px-4">
          <div className="mx-auto max-w-3xl">
            <div className="relative mb-6" ref={modelSelectorRef}>
              <Button
                onClick={toggleModelSelector}
                variant="outline"
                className="w-full opacity-50 hover:opacity-100 transition-opacity"
              >
                AI 모델 설정
              </Button>

              {showModelSelector && (
                <div className="absolute z-10 w-full mt-2 p-4 bg-white border rounded-lg shadow-lg">
                  <div className="space-y-4">
                    <div>
                      <h4 className="text-sm font-medium mb-2 flex items-center gap-2">
                        음성 모델
                        <HelpTooltip content="각 모델은 서로 다른 음성 특성을 가지고 있습니다. 모델을 변경하면 현재 선택된 음성 설정이 초기화될 수 있습니다." />
                      </h4>
                      <div className="flex gap-2">
                        {voiceModels.map((model, index) => (
                          <div key={model.name} className="relative flex-1">
                            <Button
                              onClick={() => selectVoiceModel(index)}
                              variant={model.isSelected ? "default" : "outline"}
                              className="w-full"
                            >
                              <img
                                src={model.logo}
                                alt={model.name}
                                className="h-4 w-4 mr-2"
                              />
                              {model.name}
                            </Button>
                            <div className="absolute -right-1 -top-1">
                              <HelpTooltip content={modelDescriptions[model.name as keyof typeof modelDescriptions]} />
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                    <div>
                      <h4 className="text-sm font-medium mb-2 flex items-center gap-2">
                        이미지 모델
                        <HelpTooltip content="각 모델은 서로 다른 이미지 생성 스타일을 가지고 있습니다." />
                      </h4>
                      <div className="flex gap-2">
                        {imageModels.map((model, index) => (
                          <div key={model.name} className="relative flex-1">
                            <Button
                              onClick={() => selectImageModel(index)}
                              variant={model.isSelected ? "default" : "outline"}
                              className="w-full"
                            >
                              <img
                                src={model.logo}
                                alt={model.name}
                                className="h-4 w-4 mr-2"
                              />
                              {model.name}
                            </Button>
                            <div className="absolute -right-1 -top-1">
                              <HelpTooltip content={model.name === "Stable Diffusion" 
                                ? "더 저렴한 비용으로 안정적인 이미지 생성이 가능한 모델입니다. 다양한 스타일의 이미지를 효율적으로 생성할 수 있습니다." 
                                : "고품질 이미지 생성이 가능한 모델입니다. 세부적인 디테일을 잘 표현합니다."} 
                              />
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <h1 className="text-3xl font-bold">동영상 만들기</h1>
            <p className="mt-2 text-gray-600">
              스토리를 입력하고 캐릭터를 추가하여 1분 길이의 동영상을
              만들어보세요
            </p>

            <div className="mt-8 space-y-6">
              <StoryForm
                title={title}
                story={story}
                onTitleChange={setTitle}
                onStoryChange={setStory}
              />

              <div className="border-t pt-6">
                <h2 className="text-xl font-semibold mb-4">내레이터 설정</h2>
                <div className="space-y-4">
                  <div className="flex gap-3">
                    <Button
                      type="button"
                      variant={narratorGender === "male" ? "default" : "outline"}
                      size="sm"
                      onClick={() => handleNarratorGenderChange("male")}
                    >
                      남성
                    </Button>
                    <Button
                      type="button"
                      variant={narratorGender === "female" ? "default" : "outline"}
                      size="sm"
                      onClick={() => handleNarratorGenderChange("female")}
                    >
                      여성
                    </Button>
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    {narratorGender === "male" 
                      ? ["male1", "male2", "male3", "male4"].map((voice) => (
                        <div key={voice} className="flex items-center gap-2">
                          <Button
                            type="button"
                            variant={narratorVoice === voice ? "default" : "outline"}
                            size="sm"
                            onClick={() => setNarratorVoice(voice)}
                            className="flex-1 text-sm"
                          >
                            {voice.charAt(0).toUpperCase() + voice.slice(1)}
                          </Button>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-0"
                            onClick={() => {
                              setNarratorVoice(voice);
                              handleNarratorVoicePlay();
                            }}
                          >
                            {isNarratorPlaying && narratorVoice === voice ? (
                              <Pause className="h-4 w-4" />
                            ) : (
                              <Play className="h-4 w-4" />
                            )}
                          </Button>
                        </div>
                      ))
                      : ["female1", "female2", "female3", "female4"].map((voice) => (
                        <div key={voice} className="flex items-center gap-2">
                          <Button
                            type="button"
                            variant={narratorVoice === voice ? "default" : "outline"}
                            size="sm"
                            onClick={() => setNarratorVoice(voice)}
                            className="flex-1 text-sm"
                          >
                            {voice.charAt(0).toUpperCase() + voice.slice(1)}
                          </Button>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-0"
                            onClick={() => {
                              setNarratorVoice(voice);
                              handleNarratorVoicePlay();
                            }}
                          >
                            {isNarratorPlaying && narratorVoice === voice ? (
                              <Pause className="h-4 w-4" />
                            ) : (
                              <Play className="h-4 w-4" />
                            )}
                          </Button>
                        </div>
                      ))
                    }
                  </div>
                </div>
              </div>

              <CharacterForm
                characters={characters}
                addCharacter={addCharacter}
                updateCharacter={updateCharacter}
                removeCharacter={removeCharacter}
                currentlyPlaying={currentlyPlaying}
                setCurrentlyPlaying={setCurrentlyPlaying}
                voiceModel={voiceModels.find(model => model.isSelected)?.name || "ElevenLabs"}
              />

              <Button
                onClick={handleGenerateVideo}
                disabled={isGenerating || !story.trim() || !title.trim()}
                className="w-full bg-red-600 hover:bg-red-700"
                size="lg"
              >
                {isGenerating ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    동영상 생성 중...
                  </>
                ) : (
                  "동영상 생성하기"
                )}
              </Button>
            </div>
          </div>
        </div>
      </main>

      {isGenerating && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="rounded-lg bg-white p-6 text-center">
            <Loader2 className="mx-auto h-12 w-12 animate-spin text-red-600" />
            <h3 className="mt-4 text-xl font-semibold">동영상 생성 중</h3>
            <p className="mt-2 text-gray-600">
              잠시만 기다려주세요. 페이지를 닫지 마세요.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
