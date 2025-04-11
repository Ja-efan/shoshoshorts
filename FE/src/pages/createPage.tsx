"use client";

import { useState, useRef, useEffect } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";
import { apiService } from "@/api/api";
import { useCharacter } from "@/hooks/useCharacter";
import { CharacterForm } from "@/components/create/CharacterForm";
import { CharacterFormZonos } from "@/components/create/CharacterFormZonos";
import { ModelSelector } from "@/components/create/ModelSelector";
import { NarratorSettingsZonos } from "@/components/create/NarratorSettingsZonos";
import {
  NarratorSettings,
  NarratorRef,
} from "@/components/create/NarratorSettings";
import { CurrentlyPlaying } from "@/types/character";
import { ModelType } from "@/types/voice";
import {
  defaultVoiceModels,
  defaultImageModels,
  voiceCodes,
} from "@/constants/voiceData";
import { toast } from "react-hot-toast";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { ISpeakerInfoGet } from "@/types/speakerInfo";
import shortLogo from "@/assets/short_logo.png";

export default function CreateVideoPage() {
  const { characters, addCharacter, updateCharacter, removeCharacter } =
    useCharacter();
  const [searchParams] = useSearchParams();
  const audioModelName = searchParams.get("audioModelName");
  const imageModelName = searchParams.get("imageModelName");
  const [story, setStory] = useState("");
  const [title, setTitle] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const [validationErrors, setValidationErrors] = useState({
    title: false,
    story: false,
    characters: false,
  });
  const [currentlyPlaying, setCurrentlyPlaying] = useState<CurrentlyPlaying>({
    voiceOption: null,
    characterId: null,
  });
  const [narratorGender, setNarratorGender] = useState<"male" | "female">(
    "male"
  );
  const [narratorVoice, setNarratorVoice] = useState<string>("male1");
  const [voiceModels, setVoiceModels] =
    useState<ModelType[]>(defaultVoiceModels);
  const [imageModels, setImageModels] =
    useState<ModelType[]>(defaultImageModels);
  const [showModelSelector, setShowModelSelector] = useState(false);
  const narratorRef = useRef<NarratorRef>(null);
  const navigate = useNavigate();
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [isZonosSelected, setIsZonosSelected] = useState(false);

  // 사용자의 Zonos 음성 목록 가져오기
  const [zonosList, setZonosList] = useState<ISpeakerInfoGet[] | null>(null);
  const [zonosLoading, setZonosLoading] = useState(false);

  // Zonos 내레이터 ID 추적
  const [narratorZonosId, setNarratorZonosId] = useState<number | null>(null);

  // 스크롤 위치에 따른 텍스트 상태 관리

  // 글자 변경을 위한 상태와 트랜지션을 관리하는 상태 추가
  const [headerTextVisible, setHeaderTextVisible] = useState(true);
  const [queuedHeaderText, setQueuedHeaderText] = useState({
    title: "동영상 만들기",
    description:
      "스토리를 입력하고 캐릭터를 추가하여 1분 길이의 동영상을 만들어보세요.",
  });

  useEffect(() => {
    const fetchZonosList = async () => {
      try {
        const response = await apiService.getSpeakerLibrary();
        setZonosList(response.data);
      } catch (error) {
        console.error("사용자 정보를 가져오는 중 오류가 발생했습니다:", error);
      } finally {
        setZonosLoading(false);
      }
    };

    fetchZonosList();
  }, []);

  const validateCharacters = () => {
    return characters.every(
      (character) =>
        character.name.trim() !== "" &&
        (character.description?.trim() || "").length > 0
    );
  };

  // 모델 변경 시 재생 중인 모든 오디오 정지
  const handleVoiceModelChange = () => {
    // 캐릭터 음성 정지
    if (currentlyPlaying.voiceOption && currentlyPlaying.characterId) {
      setCurrentlyPlaying({ voiceOption: null, characterId: null });
    }

    // 나레이터 음성 정지
    if (narratorRef.current) {
      narratorRef.current.stopAudio();
    }
  };

  const getSelectedVoiceModel = () => {
    return voiceModels.find((model) => model.isSelected)?.name || "ElevenLabs";
  };

  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    if (value.length <= 15) {
      setTitle(value);
    }
  };

  const handleStoryChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const value = e.target.value;
    if (value.length <= 1000) {
      setStory(value);
    }
  };

  const validateForm = () => {
    const errors = {
      title: title.trim() === "" || title.length > 15,
      story: story.trim() === "" || story.length > 1000,
      characters: characters.length > 0 && !validateCharacters(),
    };
    setValidationErrors(errors);
    return !Object.values(errors).some((error) => error);
  };

  const handleGenerateVideo = async () => {
    if (!validateForm()) {
      toast.error("모든 캐릭터의 이름과 설명을 입력해주세요.🫰");
      return;
    }

    setIsGenerating(true);

    try {
      // 선택된 모델 가져오기
      const selectedVoiceModel =
        voiceModels.find((model) => model.isSelected)?.name || "Eleven Labs";
      const selectedImageModel =
        imageModels.find((model) => model.isSelected)?.name || "Kling";

      // URL 파라미터 값이 있으면 그것을 우선 사용, 없으면 선택된 모델 사용
      const finalAudioModel = audioModelName || selectedVoiceModel;
      const finalImageModel = imageModelName || selectedImageModel;

      const requestData: any = {
        title,
        story,
        // Zonos가 선택된 경우 내레이터 ID 사용, 아니면 기존 방식 사용
        narVoiceCode: isZonosSelected
          ? String(narratorZonosId) // Zonos ID를 직접 사용
          : voiceCodes[finalAudioModel][narratorGender][
              parseInt(narratorVoice.slice(-1)) - 1
            ],
        audioModelName: finalAudioModel,
        imageModelName: finalImageModel,
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
          // Zonos가 선택된 경우 캐릭터의 Zonos 음성 ID 사용, 아니면 기존 방식 사용
          voiceCode: isZonosSelected
            ? String(
                character.voice === "male1"
                  ? -1
                  : character.voice === "female1"
                  ? -2
                  : character.voice
              ) || null // Zonos ID 사용
            : character.voice && character.gender
            ? voiceCodes[finalAudioModel][character.gender][
                parseInt(character.voice.slice(-1)) - 1
              ]
            : null,
        }));
      }

      console.log("Request Data:", requestData); // 요청 데이터 로깅
      const response = await apiService.createVideo({
        data: requestData,
      });
      console.log("API Response:", response);
      setShowSuccessModal(true);
    } catch (error) {
      console.error("API Error:", error);
      if (
        error instanceof Error &&
        error.message === "토큰 갱신에 실패했습니다. 다시 로그인해주세요."
      ) {
        toast.error("세션이 만료되었습니다. 다시 로그인해주세요.");
        navigate("/login");
      } else {
        toast.error("비디오 생성에 실패했습니다. 다시 시도해주세요.");
      }
    } finally {
      setIsGenerating(false);
    }
  };

  const handleGoToDashboard = () => {
    setShowSuccessModal(false);
    navigate("/dashboard");
  };

  // Zonos 선택 여부를 업데이트하는 함수
  const handleZonosSelection = (isZonos: boolean) => {
    setIsZonosSelected(isZonos);
  };

  // 각 섹션에 hover될 때 호출될 함수
  const handleSectionHover = (section: string) => {
    // 트랜지션을 위해 텍스트 변경 예약
    setHeaderTextVisible(false); // 먼저 현재 텍스트를 숨김

    // 섹션에 따라 새 텍스트 설정
    let newHeaderText = {
      title: "",
      description: "",
    };

    switch (section) {
      case "model":
        newHeaderText = {
          title: "1단계: 음성 및 이미지 모델 선택",
          description: "원하는 음성 및 이미지 생성 모델을 선택하세요.",
        };
        break;
      case "title":
        newHeaderText = {
          title: "2단계: 비디오 제목 입력",
          description: "생성할 비디오의 제목을 15자 이하로 입력해주세요.",
        };
        break;
      case "story":
        newHeaderText = {
          title: "3단계: 스토리 작성",
          description: "비디오에 담길 스토리를 1000자 이내로 작성해주세요",
        };
        break;
      case "narrator":
        newHeaderText = {
          title: "4단계: 내레이터 설정",
          description:
            "스토리를 읽어줄 내레이터의 목소리를 선택해주세요. 인물의 대사가 아닌, 지문을 읽어줍니다.",
        };
        break;
      case "character":
        newHeaderText = {
          title: "5단계: 캐릭터 설정",
          description:
            "스토리에 등장할 캐릭터와 그 특성과 목소리를 설정해주세요. 이곳에 작성된 내용을 기반으로 인물 이미지를 생성하고, 인물의 대사를 읽어줍니다.",
        };
        break;
      default:
        newHeaderText = {
          title: "동영상 만들기",
          description:
            "스토리를 입력하고 캐릭터를 추가하여 1분 길이의 동영상을 만들어보세요",
        };
    }

    // 텍스트가 모두 숨겨진 후 새 텍스트를 설정하고 표시
    setTimeout(() => {
      setQueuedHeaderText(newHeaderText);
      setHeaderTextVisible(true);
    }, 200); // 페이드 아웃 시간
  };

  // 마우스가 영역을 벗어날 때 기본값으로 복원
  const handleMouseLeave = () => {
    setHeaderTextVisible(false);

    setTimeout(() => {
      const defaultText = {
        title: "동영상 만들기",
        description:
          "스토리를 입력하고 캐릭터를 추가하여 1분 길이의 동영상을 만들어보세요",
      };
      setQueuedHeaderText(defaultText);
      setHeaderTextVisible(true);
    }, 200); // 페이드 아웃 시간
  };

  return (
    <div className="flex min-h-screen flex-col">
      <main className="flex-1 py-4">
        <div className="sticky top-0 bg-white z-20">
          <div className="mx-auto max-w-3xl">
            <Link to="/" className="flex items-center gap-2 px-4 sm:px-0">
              <img src={shortLogo} alt="쇼쇼숏 로고" className="h-8 w-auto" />
            </Link>
          </div>
        </div>
        <div className="container mx-auto px-4">
          <div className="sticky top-0 bg-white py-4 z-20 mb-1">
            <div className="mx-auto max-w-3xl">
              <h1
                className={`text-3xl font-bold transition-opacity duration-200 ease-in-out ${
                  headerTextVisible ? "opacity-100" : "opacity-0"
                }`}
              >
                {queuedHeaderText.title}
              </h1>
              <p
                className={`my-4 text-lg transition-opacity duration-200 ease-in-out ${
                  headerTextVisible ? "opacity-100" : "opacity-0"
                }`}
              >
                {queuedHeaderText.description}
              </p>
            </div>
          </div>
          <div className="mx-auto max-w-3xl">
            <div className="relative">
              <div className="absolute -left-20 top-7 -translate-y-1/2 bg-red-100 px-3 py-2 rounded-lg font-medium text-red-700 transition-opacity duration-200 ease-in-out shadow-sm">
                1단계
              </div>
              <div
                className="rounded-lg border border-gray-200 p-4 transition-all hover:border-red-200 focus-within:border-gray-500 focus-within:ring-1 focus-within:ring-gray-500"
                onMouseEnter={() => handleSectionHover("model")}
                onMouseLeave={handleMouseLeave}
              >
                <ModelSelector
                  showModelSelector={showModelSelector}
                  setShowModelSelector={setShowModelSelector}
                  voiceModels={voiceModels}
                  setVoiceModels={setVoiceModels}
                  imageModels={imageModels}
                  setImageModels={setImageModels}
                  onVoiceModelChange={handleVoiceModelChange}
                  onSelectZonos={handleZonosSelection}
                />
              </div>
            </div>
            <div className="mt-8 space-y-6">
              <div className="space-y-4">
                <div className="relative">
                  <div className="absolute -left-20 top-7 -translate-y-1/2 bg-red-100 px-3 py-2 rounded-lg font-medium text-red-700 transition-opacity duration-200 ease-in-out shadow-sm">
                    2단계
                  </div>
                  <div
                    className="rounded-lg border border-gray-200 p-4 transition-all hover:border-red-200 focus-within:border-gray-500 focus-within:ring-1 focus-within:ring-gray-500"
                    onMouseEnter={() => handleSectionHover("title")}
                    onMouseLeave={handleMouseLeave}
                  >
                    <Label
                      className="mb-2 block font-semibold text-lg"
                      htmlFor="title"
                    >
                      제목
                    </Label>
                    <div className="relative">
                      <Input
                        id="title"
                        value={title}
                        onChange={handleTitleChange}
                        placeholder="비디오 제목을 입력하세요"
                        className={`border-0 p-0 shadow-none focus-visible:ring-0 ${
                          validationErrors.title ? "text-red-500" : ""
                        }`}
                      />
                      <span
                        className={`absolute right-2 top-2 text-sm ${
                          title.length > 20 ? "text-red-500" : "text-gray-500"
                        }`}
                      >
                        {title.length}/15
                      </span>
                    </div>
                  </div>
                </div>

                <div className="relative">
                  <div className="absolute -left-20 top-7 -translate-y-1/2 bg-red-100 px-3 py-2 rounded-lg font-medium text-red-700 transition-opacity duration-200 ease-in-out shadow-sm">
                    3단계
                  </div>
                  <div
                    className="rounded-lg border border-gray-200 p-4 transition-all hover:border-red-200 focus-within:border-gray-500 focus-within:ring-1 focus-within:ring-gray-500"
                    onMouseEnter={() => handleSectionHover("story")}
                    onMouseLeave={handleMouseLeave}
                  >
                    <Label
                      className="mb-3 block font-semibold text-lg"
                      htmlFor="story"
                    >
                      스토리
                    </Label>
                    <div className="relative">
                      <Textarea
                        id="story"
                        value={story}
                        onChange={handleStoryChange}
                        placeholder="스토리를 입력하세요"
                        className={`min-h-[200px] border-0 p-0 shadow-none focus-visible:ring-0 ${
                          validationErrors.story ? "text-red-500" : ""
                        }`}
                      />
                      <span
                        className={`absolute right-2 bottom-2 text-sm ${
                          story.length > 1000 ? "text-red-500" : "text-gray-500"
                        }`}
                      >
                        {story.length}/1000
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="relative">
                <div className="absolute -left-20 top-7 -translate-y-1/2 bg-red-100 px-3 py-2 rounded-lg font-medium text-red-700 transition-opacity duration-200 ease-in-out shadow-sm">
                  4단계
                </div>
                <div
                  className="rounded-lg border border-gray-200 p-4 transition-all hover:border-red-200 focus-within:border-gray-500 focus-within:ring-1 focus-within:ring-gray-500"
                  onMouseEnter={() => handleSectionHover("narrator")}
                  onMouseLeave={handleMouseLeave}
                >
                  {!isZonosSelected ? (
                    <NarratorSettings
                      ref={narratorRef}
                      narratorGender={narratorGender}
                      setNarratorGender={setNarratorGender}
                      narratorVoice={narratorVoice}
                      setNarratorVoice={setNarratorVoice}
                      selectedVoiceModel={getSelectedVoiceModel()}
                      setCurrentlyPlaying={setCurrentlyPlaying}
                      currentlyPlaying={currentlyPlaying}
                    />
                  ) : (
                    <NarratorSettingsZonos
                      zonosList={zonosList}
                      zonosLoading={zonosLoading}
                      setNarratorZonosId={setNarratorZonosId}
                      narratorZonosId={narratorZonosId}
                    />
                  )}
                </div>
              </div>

              <div className="relative">
                <div className="absolute -left-20 top-7 -translate-y-1/2 bg-red-100 px-3 py-2 rounded-lg font-medium text-red-700 transition-opacity duration-200 ease-in-out shadow-sm">
                  5단계
                </div>
                <div
                  className="rounded-lg border border-gray-200 p-4 transition-all hover:border-red-200 focus-within:border-gray-500 focus-within:ring-1 focus-within:ring-gray-500"
                  onMouseEnter={() => handleSectionHover("character")}
                  onMouseLeave={handleMouseLeave}
                >
                  {!isZonosSelected ? (
                    <CharacterForm
                      characters={characters}
                      addCharacter={addCharacter}
                      updateCharacter={updateCharacter}
                      removeCharacter={removeCharacter}
                      currentlyPlaying={currentlyPlaying}
                      setCurrentlyPlaying={setCurrentlyPlaying}
                      voiceModel={getSelectedVoiceModel()}
                      validationErrors={validationErrors}
                      narratorRef={narratorRef as React.RefObject<NarratorRef>}
                    />
                  ) : (
                    <CharacterFormZonos
                      characters={characters}
                      addCharacter={addCharacter}
                      updateCharacter={updateCharacter}
                      removeCharacter={removeCharacter}
                      zonosList={zonosList}
                      zonosLoading={zonosLoading}
                      validationErrors={validationErrors}
                    />
                  )}
                </div>
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

      <Dialog
        open={showSuccessModal}
        onOpenChange={(open) => {
          // 모달이 닫히려고 할 때 대시보드로 이동
          if (!open) {
            navigate("/dashboard");
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>비디오 생성 요청 완료</DialogTitle>
            <DialogDescription>
              비디오 생성이 정상적으로 요청되었습니다.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <p>대시보드에서 생성 진행 상황을 확인할 수 있습니다.</p>
          </div>
          <DialogFooter>
            <Button onClick={handleGoToDashboard}>대시보드로 이동</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
