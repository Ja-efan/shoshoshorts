import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card } from "@/components/ui/card";
import { Play, Pause, Plus, X, Loader2 } from "lucide-react";
import { Character } from "@/types/character";
import { ISpeakerInfoGet } from "@/types/speakerInfo";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface CharacterFormZonosProps {
  characters: Character[];
  addCharacter: () => void;
  updateCharacter: (id: string, field: keyof Character, value: any) => void;
  removeCharacter: (id: string) => void;
  zonosList: ISpeakerInfoGet[] | null;
  zonosLoading: boolean;
  validationErrors?: {
    characters: boolean;
  };
}

export function CharacterFormZonos({
  characters,
  addCharacter,
  updateCharacter,
  removeCharacter,
  zonosList,
  zonosLoading,
  validationErrors = { characters: false },
}: CharacterFormZonosProps) {
  // 각 캐릭터에 대한 오디오 재생 상태 관리
  const [playingAudio, setPlayingAudio] = useState<{
    [key: string]: { isPlaying: boolean; audioRef: HTMLAudioElement | null };
  }>({});

  // Zonos 음성 선택 추적을 위한 state
  const [selectedVoices, setSelectedVoices] = useState<{
    [characterId: string]: number | null;
  }>({});

  // 컴포넌트가 마운트될 때 기존 캐릭터의 voice 정보 로드
  useEffect(() => {
    const initialVoices: { [characterId: string]: number | null } = {};

    characters.forEach((character) => {
      // character.voice가 number이면 Zonos ID로 간주
      if (character.voice && typeof character.voice === "number") {
        initialVoices[character.id] = character.voice;
      } else {
        initialVoices[character.id] = null;
      }
    });

    setSelectedVoices(initialVoices);
  }, [characters]);

  // 오디오 재생/정지 처리 함수
  const handlePlayVoice = (characterId: string, voice: ISpeakerInfoGet) => {
    // 다른 캐릭터의 오디오 먼저 정지
    Object.keys(playingAudio).forEach((id) => {
      if (id !== characterId && playingAudio[id]?.isPlaying) {
        playingAudio[id].audioRef?.pause();
        setPlayingAudio((prev) => ({
          ...prev,
          [id]: { ...prev[id], isPlaying: false, audioRef: null },
        }));
      }
    });

    // 현재 재생 중이면 정지
    if (playingAudio[characterId]?.isPlaying) {
      playingAudio[characterId].audioRef?.pause();
      setPlayingAudio((prev) => ({
        ...prev,
        [characterId]: { isPlaying: false, audioRef: null },
      }));
      return;
    }

    // 새 오디오 생성 및 재생
    try {
      const audio = new Audio(voice.voiceSampleUrl);
      audio.addEventListener("ended", () => {
        setPlayingAudio((prev) => ({
          ...prev,
          [characterId]: { ...prev[characterId], isPlaying: false },
        }));
      });

      audio
        .play()
        .catch((error) => console.error("음성 샘플 재생 중 오류:", error));

      setPlayingAudio((prev) => ({
        ...prev,
        [characterId]: { isPlaying: true, audioRef: audio },
      }));
    } catch (error) {
      console.error("오디오 객체 생성 중 오류:", error);
    }
  };

  // 목소리 선택 처리 함수
  const handleSelectVoice = (characterId: string, voiceId: string) => {
    // 이미 재생 중인 오디오 정지
    if (playingAudio[characterId]?.isPlaying) {
      playingAudio[characterId].audioRef?.pause();
      setPlayingAudio((prev) => ({
        ...prev,
        [characterId]: { isPlaying: false, audioRef: null },
      }));
    }

    // voiceId가 문자열로 전달되므로 숫자로 변환
    const numericVoiceId = parseInt(voiceId);

    // 로컬 상태 업데이트
    setSelectedVoices((prev) => ({
      ...prev,
      [characterId]: numericVoiceId,
    }));

    // 캐릭터 객체 업데이트 - voice 필드를 사용하여 Zonos ID 저장
    updateCharacter(characterId, "voice", numericVoiceId);

    // 상태 변화 로깅
    console.log(`캐릭터 ${characterId}에 음성 ID ${numericVoiceId} 선택됨`);
  };

  // 선택된 음성 정보 가져오기
  const getSelectedVoice = (characterId: string): ISpeakerInfoGet | null => {
    if (!zonosList) return null;

    // 선택된 음성 ID 가져오기
    const voiceId = selectedVoices[characterId];

    if (voiceId === undefined || voiceId === null) return null;

    // 해당 ID를 가진 음성 정보 반환
    return zonosList.find((voice) => voice.id === voiceId) || null;
  };

  return (
    <div>
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">캐릭터</h2>
        <Button
          onClick={addCharacter}
          variant="outline"
          className="flex items-center gap-2"
          disabled={characters.length >= 4}
        >
          <Plus className="h-4 w-4" />
          캐릭터 추가 {characters.length >= 4 && "(최대 4명)"}
        </Button>
      </div>

      {characters.length === 0 ? (
        <div className="mt-4 rounded-lg border border-dashed p-6 text-center">
          <p className="text-gray-500">
            아직 캐릭터가 없습니다. 스토리를 더 풍부하게 만들기 위해 캐릭터를
            추가해보세요.
          </p>
        </div>
      ) : (
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          {characters.map((character) => (
            <Card key={character.id} className="p-4">
              <div className="flex justify-between">
                <h3 className="font-medium">캐릭터</h3>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 w-8 p-0 text-gray-500"
                  onClick={() => removeCharacter(character.id)}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
              <div className="mt-3 space-y-3">
                <div className="space-y-4">
                  <div>
                    <Label className="mb-2" htmlFor={`name-${character.id}`}>
                      캐릭터 이름 <span className="text-red-500">*</span>
                    </Label>
                    <Input
                      id={`name-${character.id}`}
                      value={character.name}
                      onChange={(e) =>
                        updateCharacter(character.id, "name", e.target.value)
                      }
                      placeholder="홍길동"
                      className={`${
                        character.name.trim() === "" &&
                        validationErrors.characters
                          ? "border-red-500"
                          : ""
                      }`}
                    />
                  </div>
                  <div>
                    <Label
                      className="mb-2"
                      htmlFor={`description-${character.id}`}
                    >
                      캐릭터 설명 <span className="text-red-500">*</span>
                    </Label>
                    <Textarea
                      id={`description-${character.id}`}
                      value={character.description || ""}
                      onChange={(e) =>
                        updateCharacter(
                          character.id,
                          "description",
                          e.target.value
                        )
                      }
                      placeholder="빨간 머리, 초록색 가디건, 25세, 이쁨"
                      className={`min-h-[100px] ${
                        (character.description || "").trim() === "" &&
                        validationErrors.characters
                          ? "border-red-500"
                          : ""
                      }`}
                    />
                  </div>
                </div>
                <div>
                  <Label>성별</Label>
                  <div className="mt-1 flex gap-3">
                    <Button
                      type="button"
                      variant={
                        character.gender === "male" ? "default" : "outline"
                      }
                      size="sm"
                      onClick={() =>
                        updateCharacter(
                          character.id,
                          "gender",
                          character.gender === "male" ? null : "male"
                        )
                      }
                    >
                      남성
                    </Button>
                    <Button
                      type="button"
                      variant={
                        character.gender === "female" ? "default" : "outline"
                      }
                      size="sm"
                      onClick={() =>
                        updateCharacter(
                          character.id,
                          "gender",
                          character.gender === "female" ? null : "female"
                        )
                      }
                    >
                      여성
                    </Button>
                  </div>
                </div>

                {/* Zonos 음성 선택 UI */}
                <div className="mt-3">
                  <Label>Zonos 목소리</Label>
                  <div className="mt-2 space-y-3">
                    {zonosLoading ? (
                      <div className="flex items-center justify-center p-4">
                        <Loader2 className="h-5 w-5 animate-spin text-gray-500 mr-2" />
                        <span className="text-sm">음성 목록 로딩 중...</span>
                      </div>
                    ) : zonosList && zonosList.length > 0 ? (
                      <>
                        <Select
                          value={selectedVoices[character.id]?.toString() || ""}
                          onValueChange={(value) =>
                            handleSelectVoice(character.id, value)
                          }
                        >
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="캐릭터 음성을 선택하세요" />
                          </SelectTrigger>
                          <SelectContent>
                            {zonosList.map((voice) => (
                              <SelectItem
                                key={voice.id}
                                value={String(voice.id)}
                              >
                                {voice.title}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>

                        {/* 선택된 음성 정보 표시 */}
                        {getSelectedVoice(character.id) && (
                          <div className="bg-gray-50 p-3 rounded-md">
                            <div className="flex justify-between items-center">
                              <div>
                                <h4 className="font-medium text-sm">
                                  {getSelectedVoice(character.id)?.title}
                                </h4>
                                <p className="text-xs text-gray-600">
                                  {getSelectedVoice(character.id)?.description}
                                </p>
                              </div>
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                className="h-7 min-w-7 p-0"
                                onClick={() => {
                                  const selectedVoice = getSelectedVoice(
                                    character.id
                                  );
                                  if (selectedVoice) {
                                    handlePlayVoice(
                                      character.id,
                                      selectedVoice
                                    );
                                  }
                                }}
                              >
                                {playingAudio[character.id]?.isPlaying ? (
                                  <Pause className="h-3 w-3" />
                                ) : (
                                  <Play className="h-3 w-3" />
                                )}
                              </Button>
                            </div>
                          </div>
                        )}
                      </>
                    ) : (
                      <div className="text-center p-3 bg-gray-50 rounded-md">
                        <p className="text-sm">등록된 Zonos 음성이 없습니다.</p>
                        <Button
                          variant="link"
                          className="p-0 h-auto text-red-600 text-sm"
                          onClick={() => window.open("/mypage", "_blank")}
                        >
                          Zonos 음성 생성하러 가기
                        </Button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
