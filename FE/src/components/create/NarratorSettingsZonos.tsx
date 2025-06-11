import { useState, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Play, Pause, Loader2 } from "lucide-react";
import { ISpeakerInfoGet } from "@/types/speakerInfo";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface NarratorSettingsZonosProps {
  zonosList: ISpeakerInfoGet[] | null;
  zonosLoading: boolean;
  setNarratorZonosId: (id: number | null) => void;
  narratorZonosId: number | null;
}

export function NarratorSettingsZonos({
  zonosList,
  zonosLoading,
  setNarratorZonosId,
  narratorZonosId,
}: NarratorSettingsZonosProps) {
  const [isNarratorPlaying, setIsNarratorPlaying] = useState(false);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [selectedVoice, setSelectedVoice] = useState<ISpeakerInfoGet | null>(
    null
  );

  useEffect(() => {
    // 초기 선택값 설정 (zonosList가 로드되면)
    if (zonosList && zonosList.length > 0 && !narratorZonosId) {
      setNarratorZonosId(zonosList[0].id);
      setSelectedVoice(zonosList[0]);
    }

    // 이미 선택된 ID가 있으면 해당 음성 객체 찾기
    if (narratorZonosId && zonosList) {
      const voice = zonosList.find((v) => v.id === narratorZonosId);
      if (voice) setSelectedVoice(voice);
    }
  }, [zonosList, narratorZonosId, setNarratorZonosId]);

  const stopAudio = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
      audioRef.current = null;
      setIsNarratorPlaying(false);
    }
  };

  const handleSelectVoice = (id: string) => {
    stopAudio(); // 현재 재생 중인 오디오 중지

    const numId = parseInt(id);
    setNarratorZonosId(numId);

    if (zonosList) {
      const voice = zonosList.find((v) => v.id === numId);
      setSelectedVoice(voice || null);
    }
  };

  const handlePlayVoice = (voice: ISpeakerInfoGet) => {
    // 이미 재생 중이면 중지
    if (isNarratorPlaying) {
      stopAudio();
      return;
    }

    // 새 오디오 생성 및 재생
    try {
      const audio = new Audio(voice.voiceSampleUrl);
      audio.addEventListener("ended", () => {
        setIsNarratorPlaying(false);
      });
      audio
        .play()
        .catch((error) => console.error("음성 샘플 재생 중 오류:", error));
      audioRef.current = audio;
      setIsNarratorPlaying(true);
    } catch (error) {
      console.error("오디오 객체 생성 중 오류:", error);
    }
  };

  // 컴포넌트 언마운트 시 오디오 정지
  useEffect(() => {
    return () => {
      stopAudio();
    };
  }, []);

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">내레이터 설정 (Zonos)</h2>
      <div className="space-y-4">
        {zonosLoading ? (
          <div className="flex items-center justify-center p-4">
            <Loader2 className="h-6 w-6 animate-spin text-gray-500 mr-2" />
            <span>음성 목록 로딩 중...</span>
          </div>
        ) : zonosList && zonosList.length > 0 ? (
          <>
            <div className="space-y-2">
              <label htmlFor="narrator-voice" className="text-sm font-medium">
                내레이터 음성 선택
              </label>
              <Select
                value={selectedVoice ? String(selectedVoice.id) : ""}
                onValueChange={handleSelectVoice}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="내레이터 음성을 선택하세요" />
                </SelectTrigger>
                <SelectContent>
                  {zonosList.map((voice) => (
                    <SelectItem key={voice.id} value={String(voice.id)}>
                      {voice.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {selectedVoice && (
              <div className="bg-gray-50 p-4 rounded-md">
                <div className="flex justify-between items-center">
                  <div>
                    <h3 className="font-medium">{selectedVoice.title}</h3>
                    <p className="text-sm text-gray-600">
                      {selectedVoice.description}
                    </p>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-8 min-w-8"
                    onClick={() => handlePlayVoice(selectedVoice)}
                  >
                    {isNarratorPlaying ? (
                      <Pause className="h-4 w-4" />
                    ) : (
                      <Play className="h-4 w-4" />
                    )}
                  </Button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="text-center p-4 bg-gray-50 rounded-md">
            <p>등록된 Zonos 음성이 없습니다.</p>
            <Button
              variant="link"
              className="p-0 h-auto text-red-600"
              onClick={() => window.open("/mypage", "_blank")}
            >
              Zonos 음성 생성하러 가기
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
