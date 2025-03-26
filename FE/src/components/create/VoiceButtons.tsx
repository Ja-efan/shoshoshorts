import { Button } from "@/components/ui/button"
import { Play, Pause } from "lucide-react"
import { Character, CurrentlyPlaying } from "@/types/character"
import { useEffect } from "react"

// 음성 파일 import
import male1 from "@/assets/voices/male/male1.mp3"
import male2 from "@/assets/voices/male/male2.mp3"
import male3 from "@/assets/voices/male/male3.mp3"
import male4 from "@/assets/voices/male/male4.mp3"
import female1 from "@/assets/voices/female/female1.mp3"
import female2 from "@/assets/voices/female/female2.mp3"
import female3 from "@/assets/voices/female/female3.mp3"
import female4 from "@/assets/voices/female/female4.mp3"

interface VoiceButtonsProps {
  character: Character
  updateCharacter: (id: string, field: keyof Character, value: any) => void
  currentlyPlaying: CurrentlyPlaying
  setCurrentlyPlaying: (value: CurrentlyPlaying) => void
}

// 전역 오디오 참조
const globalAudioRef = { current: null as HTMLAudioElement | null }

type VoiceFileKey = "male1" | "male2" | "male3" | "male4" | "female1" | "female2" | "female3" | "female4"

// 음성 파일 매핑
const voiceFiles: Record<"male" | "female", Record<VoiceFileKey, string>> = {
  male: {
    male1,
    male2,
    male3,
    male4,
    female1: "",
    female2: "",
    female3: "",
    female4: ""
  },
  female: {
    female1,
    female2,
    female3,
    female4,
    male1: "",
    male2: "",
    male3: "",
    male4: ""
  }
}

export function VoiceButtons({ character, updateCharacter, currentlyPlaying, setCurrentlyPlaying }: VoiceButtonsProps) {
  const voiceOptions = character.gender === "male" 
    ? ["male1", "male2", "male3", "male4"]
    : ["female1", "female2", "female3", "female4"]

  const handleVoiceSelect = (voiceOption: string) => {
    updateCharacter(character.id, "voice", voiceOption)
  }

  const handlePlayVoice = (voiceOption: string) => {
    if (globalAudioRef.current) {
      globalAudioRef.current.pause()
      globalAudioRef.current.currentTime = 0
      if (currentlyPlaying.voiceOption === voiceOption && currentlyPlaying.characterId === character.id) {
        setCurrentlyPlaying({ voiceOption: null, characterId: null })
        return
      }
    }

    try {
      const audio = new Audio(voiceFiles[character.gender!][voiceOption as VoiceFileKey])
      audio.addEventListener('ended', () => {
        setCurrentlyPlaying({ voiceOption: null, characterId: null })
      })
      audio.play().catch(error => console.error('Error playing audio:', error))
      globalAudioRef.current = audio
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