import { Button } from "@/components/ui/button"
import { Play, Pause } from "lucide-react"
import { Character, CurrentlyPlaying } from "@/types/character"
import { useEffect, useRef } from "react"

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

interface VoiceButtonsProps {
  character: Character
  updateCharacter: (id: string, field: keyof Character, value: any) => void
  currentlyPlaying: CurrentlyPlaying
  setCurrentlyPlaying: (value: CurrentlyPlaying) => void
  voiceModel: string // 추가: 현재 선택된 음성 모델
}

type VoiceFileKey = "male1" | "male2" | "male3" | "male4" | "female1" | "female2" | "female3" | "female4"

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
}

export function VoiceButtons({ 
  character, 
  updateCharacter, 
  currentlyPlaying, 
  setCurrentlyPlaying,
  voiceModel 
}: VoiceButtonsProps) {
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const voiceOptions = character.gender === "male" 
    ? ["male1", "male2", "male3", "male4"]
    : ["female1", "female2", "female3", "female4"]

  const handleVoiceSelect = (voiceOption: string) => {
    updateCharacter(character.id, "voice", voiceOption)
  }

  const handlePlayVoice = (voiceOption: string) => {
    if (audioRef.current) {
      audioRef.current.pause()
      audioRef.current.currentTime = 0
      if (currentlyPlaying.voiceOption === voiceOption && currentlyPlaying.characterId === character.id) {
        setCurrentlyPlaying({ voiceOption: null, characterId: null })
        return
      }
    }

    try {
      const audio = new Audio(voiceFiles[voiceModel][character.gender!][voiceOption as VoiceFileKey])
      audio.addEventListener('ended', () => {
        setCurrentlyPlaying({ voiceOption: null, characterId: null })
      })
      audio.play().catch(error => console.error('Error playing audio:', error))
      audioRef.current = audio
      setCurrentlyPlaying({ voiceOption, characterId: character.id })
    } catch (error) {
      console.error('Error creating audio:', error)
    }
  }

  useEffect(() => {
    if (character.gender && !character.voice) {
      const defaultVoice = character.gender === "male" ? "male1" : "female1"
      updateCharacter(character.id, "voice", defaultVoice)
    }
  }, [character.gender, character.voice, character.id, updateCharacter])

  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current.currentTime = 0
      }
    }
  }, [voiceModel])

  if (!character.gender) {
    return (
      <div className="flex items-center justify-center">
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled
          className="text-gray-500"
        >
          기본 목소리
        </Button>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-2 gap-2">
      {voiceOptions.map((voiceOption) => (
        <div key={voiceOption} className="flex items-center gap-2">
          <Button
            type="button"
            variant={character.voice === voiceOption ? "default" : "outline"}
            size="sm"
            onClick={() => handleVoiceSelect(voiceOption)}
            className="flex-1 text-sm"
          >
            {voiceOption.charAt(0).toUpperCase() + voiceOption.slice(1)}
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-8 w-8 p-0"
            onClick={() => handlePlayVoice(voiceOption)}
          >
            {currentlyPlaying.voiceOption === voiceOption && currentlyPlaying.characterId === character.id ? (
              <Pause className="h-4 w-4" />
            ) : (
              <Play className="h-4 w-4" />
            )}
          </Button>
        </div>
      ))}
    </div>
  )
} 