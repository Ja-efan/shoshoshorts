import {
  useState,
  useRef,
  useImperativeHandle,
  forwardRef,
  useEffect,
} from "react";
import { Button } from "@/components/ui/button";
import { Play, Pause } from "lucide-react";
import { VoiceFileKey } from "@/types/voice";
import { voiceFiles } from "@/constants/voiceData";

interface NarratorSettingsProps {
  narratorGender: "male" | "female";
  setNarratorGender: (gender: "male" | "female") => void;
  narratorVoice: string;
  setNarratorVoice: (voice: string) => void;
  selectedVoiceModel: string;
  setCurrentlyPlaying?: (value: {
    voiceOption: string | null;
    characterId: string | null;
  }) => void;
  currentlyPlaying?: { voiceOption: string | null; characterId: string | null };
}

export interface NarratorRef {
  stopAudio: () => void;
}

export const NarratorSettings = forwardRef<NarratorRef, NarratorSettingsProps>(
  (
    {
      narratorGender,
      setNarratorGender,
      narratorVoice,
      setNarratorVoice,
      selectedVoiceModel,
      setCurrentlyPlaying,
      currentlyPlaying,
    },
    ref
  ) => {
    const [isNarratorPlaying, setIsNarratorPlaying] = useState(false);
    const narratorAudioRef = useRef<HTMLAudioElement | null>(null);

    const getVoiceDisplayName = (voiceOption: string) => {
      const voiceNames = {
        male1: "젊은 남성",
        male2: "나이든 남성",
        male3: "진지한 남성",
        male4: "평범한 남성",
        female1: "젊은 여성",
        female2: "귀여운 여성",
        female3: "진지한 여성",
        female4: "조용한 여성"
      }
      return voiceNames[voiceOption as keyof typeof voiceNames] || voiceOption
    }

    const handleNarratorGenderChange = (gender: "male" | "female") => {
      // 기존 나레이터 음성 중지
      stopNarratorAudio();

      // 캐릭터 목소리 정지를 위한 상태 업데이트
      if (setCurrentlyPlaying) {
        setCurrentlyPlaying({ voiceOption: null, characterId: null });
      }

      setNarratorGender(gender);
      setNarratorVoice(`${gender}1`);
    };

    const stopNarratorAudio = () => {
      if (narratorAudioRef.current) {
        narratorAudioRef.current.pause();
        narratorAudioRef.current.currentTime = 0;
        narratorAudioRef.current = null;
        setIsNarratorPlaying(false);
      }
    };

    // 컴포넌트 외부에서 오디오 정지 메소드에 접근할 수 있게 함
    useImperativeHandle(ref, () => ({
      stopAudio: stopNarratorAudio,
    }));

    // 캐릭터 목소리가 재생될 때 나레이터 음성 중지
    useEffect(() => {
      if (currentlyPlaying && currentlyPlaying.characterId !== null) {
        stopNarratorAudio();
      }
    }, [currentlyPlaying]);

    const handleNarratorVoicePlay = (voice: string) => {
      // 기존 나레이터 음성 중지
      stopNarratorAudio();

      // 같은 목소리를 다시 클릭했을 때 재생 상태만 토글하고 종료
      if (isNarratorPlaying && narratorVoice === voice) {
        setIsNarratorPlaying(false);
        return;
      }

      // 캐릭터 목소리 정지를 위한 상태 업데이트
      if (setCurrentlyPlaying) {
        setCurrentlyPlaying({ voiceOption: null, characterId: null });
      }

      // 새 오디오 생성 및 재생
      try {
        const audio = new Audio(
          voiceFiles[selectedVoiceModel][narratorGender][voice as VoiceFileKey]
        );
        audio.addEventListener("ended", () => {
          setIsNarratorPlaying(false);
        });
        audio
          .play()
          .catch((error) => console.error("Error playing audio:", error));
        narratorAudioRef.current = audio;
        setIsNarratorPlaying(true);
      } catch (error) {
        console.error("Error creating audio:", error);
      }
    };

    // 모델 변경 시 오디오 정지
    useEffect(() => {
      return () => {
        stopNarratorAudio();
      };
    }, [selectedVoiceModel]);

    return (
      <div>
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
                      {getVoiceDisplayName(voice)}
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 p-0"
                      onClick={() => {
                        setNarratorVoice(voice);
                        handleNarratorVoicePlay(voice);
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
                      {getVoiceDisplayName(voice)}
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 p-0"
                      onClick={() => {
                        setNarratorVoice(voice);
                        handleNarratorVoicePlay(voice);
                      }}
                    >
                      {isNarratorPlaying && narratorVoice === voice ? (
                        <Pause className="h-4 w-4" />
                      ) : (
                        <Play className="h-4 w-4" />
                      )}
                    </Button>
                  </div>
                ))}
          </div>
        </div>
      </div>
    );
  }
);
