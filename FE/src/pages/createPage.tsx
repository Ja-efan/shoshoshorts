"use client";

import { useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";
import { apiService } from "@/api/api";
import { useCharacter } from "@/hooks/useCharacter";
import { CharacterForm } from "@/components/create/CharacterForm";
import { StoryForm } from "@/components/create/StoryForm";
import { CurrentlyPlaying } from "@/types/character";
import shortLogo from "@/assets/short_logo.png";
import zonosLogo from "@/assets/models/zonos_logo.svg";
import elevenLabsLogo from "@/assets/models/elevenlabs_logo.png";
import klingLogo from "@/assets/models/kling_logo.png";
import dalleLogo from "@/assets/models/dalle_logo.jpg";

type ModelType = {
  name: string;
  logo: string;
  isSelected: boolean;
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
  const [voiceModels, setVoiceModels] = useState<ModelType[]>([
    { name: "Zonos", logo: zonosLogo, isSelected: true },
    { name: "ElevenLabs", logo: elevenLabsLogo, isSelected: false },
  ]);
  const [imageModels, setImageModels] = useState<ModelType[]>([
    { name: "Kling", logo: klingLogo, isSelected: true },
    { name: "DALL-E", logo: dalleLogo, isSelected: false },
  ]);
  const [showModelSelector, setShowModelSelector] = useState(false);

  const toggleModelSelector = () => {
    setShowModelSelector(!showModelSelector);
  };

  const selectVoiceModel = (index: number) => {
    setVoiceModels(
      voiceModels.map((model, i) => ({
        ...model,
        isSelected: i === index,
      }))
    );
  };

  const selectImageModel = (index: number) => {
    setImageModels(
      imageModels.map((model, i) => ({
        ...model,
        isSelected: i === index,
      }))
    );
  };

  const handleGenerateVideo = async () => {
    setIsGenerating(true);

    const voiceCodes = {
      male: [
        "4JJwo477JUAx3HV0T7n7",
        "PLfpgtLkFW07fDYbUiRJ",
        "v1jVu1Ky28piIPEJqRrm",
        "WqVy7827vjE2r3jWvbnP",
      ],
      female: [
        "uyVNoMrnUku1dZyVEXwD",
        "xi3rF0t7dg7uN2M0WUhr",
        "z6Kj0hecH20CdetSElRT",
        "DMkRitQrfpiddSQT5adl",
      ],
    };

    const requestData: any = {
      title,
      story,
      narVoiceCode: "uyVNoMrnUku1dZyVEXwD",
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
            ? voiceCodes[character.gender][
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

              <CharacterForm
                characters={characters}
                addCharacter={addCharacter}
                updateCharacter={updateCharacter}
                removeCharacter={removeCharacter}
                currentlyPlaying={currentlyPlaying}
                setCurrentlyPlaying={setCurrentlyPlaying}
              />

              <div className="relative">
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
                        <h4 className="text-sm font-medium mb-2">음성 모델</h4>
                        <div className="flex gap-2">
                          {voiceModels.map((model, index) => (
                            <Button
                              key={model.name}
                              variant={model.isSelected ? "default" : "outline"}
                              className="flex-1"
                              onClick={() => selectVoiceModel(index)}
                            >
                              <img
                                src={model.logo}
                                alt={model.name}
                                className="h-4 w-4 mr-2"
                              />
                              {model.name}
                            </Button>
                          ))}
                        </div>
                      </div>
                      <div>
                        <h4 className="text-sm font-medium mb-2">
                          이미지 모델
                        </h4>
                        <div className="flex gap-2">
                          {imageModels.map((model, index) => (
                            <Button
                              key={model.name}
                              variant={model.isSelected ? "default" : "outline"}
                              className="flex-1"
                              onClick={() => selectImageModel(index)}
                            >
                              <img
                                src={model.logo}
                                alt={model.name}
                                className="h-4 w-4 mr-2"
                              />
                              {model.name}
                            </Button>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>

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
