"use client"

import { useState } from "react"
import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Loader2 } from "lucide-react"
import { apiService } from "@/lib/api.ts"
import { useCharacter } from "@/hooks/useCharacter"
import { CharacterForm } from "@/components/create/CharacterForm"
import { StoryForm } from "@/components/create/StoryForm"
import { CurrentlyPlaying } from "@/types/character"
import shortLogo from "@/assets/short_logo.png";

export default function CreateVideoPage() {
  const { characters, addCharacter, updateCharacter, removeCharacter } = useCharacter()
  const [story, setStory] = useState("")
  const [title, setTitle] = useState("")
  const [isGenerating, setIsGenerating] = useState(false)
  const [currentlyPlaying, setCurrentlyPlaying] = useState<CurrentlyPlaying>({ 
    voiceOption: null, 
    characterId: null 
  })

  const handleGenerateVideo = async () => {
    setIsGenerating(true)

    const voiceCodes = {
      male: ["4JJwo477JUAx3HV0T7n7", "PLfpgtLkFW07fDYbUiRJ", "v1jVu1Ky28piIPEJqRrm", "WqVy7827vjE2r3jWvbnP"],
      female: ["uyVNoMrnUku1dZyVEXwD", "xi3rF0t7dg7uN2M0WUhr", "z6Kj0hecH20CdetSElRT", "DMkRitQrfpiddSQT5adl"]
    }

    const requestData: any = {
      title,
      story,
      narVoiceCode: "uyVNoMrnUku1dZyVEXwD"
    }

    if (characters.length > 0) {
      requestData.characterArr = characters.map(character => ({
        name: character.name,
        gender: character.gender ? (character.gender === "male" ? "1" : "2") : null,
        description: character.description || "이미지 생성을 위한 설명...",
        voice_code: character.voice && character.gender ? voiceCodes[character.gender][parseInt(character.voice.slice(-1)) - 1] : null
      }))
    }

    try {
      console.log(requestData)
      const response = await apiService.createVideo(requestData)
      console.log('API Response:', response)
    } catch (error) {
      console.error('API Error:', error)
    } finally {
      setIsGenerating(false)
    }
  }

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
        <div className="container px-4">
          <div className="mx-auto max-w-3xl">
            <h1 className="text-3xl font-bold">동영상 만들기</h1>
            <p className="mt-2 text-gray-600">스토리를 입력하고 캐릭터를 추가하여 1분 길이의 동영상을 만들어보세요</p>

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
            <p className="mt-2 text-gray-600">잠시만 기다려주세요. 페이지를 닫지 마세요.</p>
          </div>
        </div>
      )}
    </div>
  )
}

