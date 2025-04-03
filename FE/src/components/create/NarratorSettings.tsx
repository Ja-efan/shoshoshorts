import { useState, useRef, useImperativeHandle, forwardRef } from "react";
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
}

export interface NarratorRef {
  stopAudio: () => void;
}

export const NarratorSettings = forwardRef<NarratorRef, NarratorSettingsProps>(({
  narratorGender,
  setNarratorGender,
  narratorVoice,
  setNarratorVoice,
  selectedVoiceModel
}, ref) => {
  const [isNarratorPlaying, setIsNarratorPlaying] = useState(false);
  const narratorAudioRef = useRef<HTMLAudioElement | null>(null);

  const handleNarratorGenderChange = (gender: "male" | "female") => {
    setNarratorGender(gender);
    setNarratorVoice(`${gender}1`);
  };

  const stopNarratorAudio = () => {
    if (narratorAudioRef.current) {
      narratorAudioRef.current.pause();
      narratorAudioRef.current.currentTime = 0;
      setIsNarratorPlaying(false);
    }
  };

  // 컴포넌트 외부에서 오디오 정지 메소드에 접근할 수 있게 함
  useImperativeHandle(ref, () => ({
    stopAudio: stopNarratorAudio
  }));

  const handleNarratorVoicePlay = (voice: string) => {
    if (narratorAudioRef.current) {
      narratorAudioRef.current.pause();
      narratorAudioRef.current.currentTime = 0;
      if (isNarratorPlaying && narratorVoice === voice) {
        setIsNarratorPlaying(false);
        return;
      }
    }

    try {
      const audio = new Audio(voiceFiles[selectedVoiceModel][narratorGender][voice as VoiceFileKey]);
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

  return (
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
                  {voice.charAt(0).toUpperCase() + voice.slice(1)}
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
          }
        </div>
      </div>
    </div>
  );
}); 