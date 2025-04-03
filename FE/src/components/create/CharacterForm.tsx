import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Card } from "@/components/ui/card"
import { Plus, X } from "lucide-react"
import { Character, CurrentlyPlaying } from "@/types/character"
import { VoiceButtons } from "./VoiceButtons"

interface CharacterFormProps {
  characters: Character[]
  addCharacter: () => void
  updateCharacter: (id: string, field: keyof Character, value: any) => void
  removeCharacter: (id: string) => void
  currentlyPlaying: CurrentlyPlaying
  setCurrentlyPlaying: (value: CurrentlyPlaying) => void
  voiceModel: string
  validationErrors?: {
    characters: boolean
  }
}

export function CharacterForm({
  characters,
  addCharacter,
  updateCharacter,
  removeCharacter,
  currentlyPlaying,
  setCurrentlyPlaying,
  voiceModel,
  validationErrors = { characters: false }
}: CharacterFormProps) {
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
          <p className="text-gray-500">아직 캐릭터가 없습니다. 스토리를 더 풍부하게 만들기 위해 캐릭터를 추가해보세요.</p>
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
                    <Label htmlFor="name">
                      캐릭터 이름 <span className="text-red-500">*</span>
                    </Label>
                    <Input
                      id="name"
                      value={character.name}
                      onChange={(e) => updateCharacter(character.id, "name", e.target.value)}
                      placeholder="홍길동"
                      className={`${character.name.trim() === "" && validationErrors.characters ? "border-red-500" : ""}`}
                    />
                  </div>
                  <div>
                    <Label htmlFor="description">
                      캐릭터 설명 <span className="text-red-500">*</span>
                    </Label>
                    <Textarea
                      id="description"
                      value={character.description || ""}
                      onChange={(e) => updateCharacter(character.id, "description", e.target.value)}
                      placeholder="빨간 머리, 초록색 가디건, 25세, 이쁨"
                      className={`min-h-[100px] ${(character.description || "").trim() === "" && validationErrors.characters ? "border-red-500" : ""}`}
                    />
                  </div>
                </div>
                <div>
                  <Label>성별</Label>
                  <div className="mt-1 flex gap-3">
                    <Button
                      type="button"
                      variant={character.gender === "male" ? "default" : "outline"}
                      size="sm"
                      onClick={() =>
                        updateCharacter(character.id, "gender", character.gender === "male" ? null : "male")
                      }
                    >
                      남성
                    </Button>
                    <Button
                      type="button"
                      variant={character.gender === "female" ? "default" : "outline"}
                      size="sm"
                      onClick={() =>
                        updateCharacter(
                          character.id,
                          "gender",
                          character.gender === "female" ? null : "female",
                        )
                      }
                    >
                      여성
                    </Button>
                  </div>
                </div>
                <div className="mt-3">
                  <Label>목소리</Label>
                  <div className="mt-1">
                    <VoiceButtons 
                      character={character} 
                      updateCharacter={updateCharacter}
                      currentlyPlaying={currentlyPlaying}
                      setCurrentlyPlaying={setCurrentlyPlaying}
                      voiceModel={voiceModel}
                    />
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
} 