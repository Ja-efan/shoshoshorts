"use client";

import { useState, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";
import { apiService } from "@/api/api";
import { useCharacter } from "@/hooks/useCharacter";
import { CharacterForm } from "@/components/create/CharacterForm";
import { ModelSelector } from "@/components/create/ModelSelector";
import { NarratorSettings, NarratorRef } from "@/components/create/NarratorSettings";
import { CurrentlyPlaying } from "@/types/character";
import shortLogo from "@/assets/short_logo.png";
import { ModelType } from "@/types/voice";
import { 
  defaultVoiceModels, 
  defaultImageModels, 
  voiceCodes
} from "@/constants/voiceData";
import { toast } from "react-hot-toast";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Navbar } from "@/components/common/Navbar";

export default function CreateVideoPage() {
  const { characters, addCharacter, updateCharacter, removeCharacter } =
    useCharacter();
  const [story, setStory] = useState("");
  const [title, setTitle] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const [validationErrors, setValidationErrors] = useState({
    title: false,
    story: false,
    characters: false
  });
  const [currentlyPlaying, setCurrentlyPlaying] = useState<CurrentlyPlaying>({
    voiceOption: null,
    characterId: null,
  });
  const [narratorGender, setNarratorGender] = useState<"male" | "female">("male");
  const [narratorVoice, setNarratorVoice] = useState<string>("male1");
  const [voiceModels, setVoiceModels] = useState<ModelType[]>(defaultVoiceModels);
  const [imageModels, setImageModels] = useState<ModelType[]>(defaultImageModels);
  const [showModelSelector, setShowModelSelector] = useState(false);
  const narratorRef = useRef<NarratorRef>(null);
  const navigate = useNavigate();
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  const validateCharacters = () => {
    return characters.every(character => 
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
    return voiceModels.find(model => model.isSelected)?.name || "ElevenLabs";
  };

  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    if (value.length <= 20) {
      setTitle(value);
    }
  };

  const handleStoryChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const value = e.target.value;
    if (value.length <= 500) {
      setStory(value);
    }
  };

  const validateForm = () => {
    const errors = {
      title: title.trim() === "" || title.length > 20,
      story: story.trim() === "" || story.length > 500,
      characters: characters.length > 0 && !validateCharacters()
    };
    setValidationErrors(errors);
    return !Object.values(errors).some(error => error);
  };

  const handleGenerateVideo = async () => {
    if (!validateForm()) {
      toast.error("모든 캐릭터의 이름과 설명을 입력해주세요.🫰");
      return;
    }

    setIsGenerating(true);

    const selectedVoiceModel = getSelectedVoiceModel();

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
      setShowSuccessModal(true);
    } catch (error) {
      console.error("API Error:", error);
      toast.error("비디오 생성에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsGenerating(false);
    }
  };

  const handleGoToDashboard = () => {
    setShowSuccessModal(false);
    navigate("/dashboard");
  };

  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className="flex-1 py-8">
        <div className="container mx-auto px-4">
          <div className="mx-auto max-w-3xl">
            <ModelSelector 
              showModelSelector={showModelSelector}
              setShowModelSelector={setShowModelSelector}
              voiceModels={voiceModels}
              setVoiceModels={setVoiceModels}
              imageModels={imageModels}
              setImageModels={setImageModels}
              onVoiceModelChange={handleVoiceModelChange}
            />

            <h1 className="text-3xl font-bold">동영상 만들기</h1>
            <p className="mt-2 text-gray-600">
              스토리를 입력하고 캐릭터를 추가하여 1분 길이의 동영상을
              만들어보세요
            </p>

            <div className="mt-8 space-y-6">
              <div className="space-y-4">
                <div>
                  <Label htmlFor="title">제목</Label>
                  <div className="relative">
                    <Input
                      id="title"
                      value={title}
                      onChange={handleTitleChange}
                      placeholder="비디오 제목을 입력하세요"
                      className={validationErrors.title ? "border-red-500" : ""}
                    />
                    <span className={`absolute right-2 top-2 text-sm ${title.length > 20 ? "text-red-500" : "text-gray-500"}`}>
                      {title.length}/20
                    </span>
                  </div>
                </div>

                <div>
                  <Label htmlFor="story">스토리</Label>
                  <div className="relative">
                    <Textarea
                      id="story"
                      value={story}
                      onChange={handleStoryChange}
                      placeholder="스토리를 입력하세요"
                      className={`min-h-[200px] ${validationErrors.story ? "border-red-500" : ""}`}
                    />
                    <span className={`absolute right-2 bottom-2 text-sm ${story.length > 500 ? "text-red-500" : "text-gray-500"}`}>
                      {story.length}/500
                    </span>
                  </div>
                </div>
              </div>

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

      <Dialog open={showSuccessModal} onOpenChange={setShowSuccessModal}>
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
