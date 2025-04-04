import { Button } from "@/components/ui/button"
import { Play, Pause } from "lucide-react"
import { useEffect, useRef } from "react"
import { VoiceButtonsProps, VoiceFileKey } from "@/types/voice"
import { voiceFiles } from "@/constants/voiceData"

export function VoiceButtons({ 
  character, 
  updateCharacter, 
  currentlyPlaying, 
  setCurrentlyPlaying,
  voiceModel,
  narratorRef 
}: VoiceButtonsProps) {
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const voiceOptions = character.gender === "male" 
    ? ["male1", "male2", "male3", "male4"]
    : ["female1", "female2", "female3", "female4"]

  const handleVoiceSelect = (voiceOption: string) => {
    updateCharacter(character.id, "voice", voiceOption)
  }

  const stopAudio = () => {
    if (audioRef.current) {
      audioRef.current.pause()
      audioRef.current.currentTime = 0
      audioRef.current = null
    }
  }

  const handlePlayVoice = (voiceOption: string) => {
    // 현재 재생 중인 소리 중지
    stopAudio()

    // 현재 재생 중인 것이 이 캐릭터의 같은 목소리라면, 정지 상태로 전환
    if (currentlyPlaying.voiceOption === voiceOption && currentlyPlaying.characterId === character.id) {
      setCurrentlyPlaying({ voiceOption: null, characterId: null })
      return
    }

    // 나레이터 음성 중지
    if (narratorRef && narratorRef.current) {
      narratorRef.current.stopAudio()
    }

    // 다른 캐릭터 음성 중지를 위해 currentlyPlaying 상태 업데이트
    setCurrentlyPlaying({ voiceOption, characterId: character.id })

    try {
      const audio = new Audio(voiceFiles[voiceModel][character.gender!][voiceOption as VoiceFileKey])
      audio.addEventListener('ended', () => {
        setCurrentlyPlaying({ voiceOption: null, characterId: null })
      })
      audio.play().catch(error => console.error('Error playing audio:', error))
      audioRef.current = audio
    } catch (error) {
      console.error('Error creating audio:', error)
    }
  }

  // currentlyPlaying 상태가 변경될 때마다 오디오 상태 체크
  useEffect(() => {
    // 현재 캐릭터의 오디오가 재생 중이고, currentlyPlaying이 다른 상태로 변경된 경우
    if (audioRef.current && 
        (currentlyPlaying.characterId !== character.id || 
         currentlyPlaying.voiceOption === null)) {
      stopAudio()
    }
  }, [currentlyPlaying, character.id])

  useEffect(() => {
    if (character.gender && !character.voice) {
      const defaultVoice = character.gender === "male" ? "male1" : "female1"
      updateCharacter(character.id, "voice", defaultVoice)
    }
  }, [character.gender, character.voice, character.id, updateCharacter])

  useEffect(() => {
    return () => {
      stopAudio()
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