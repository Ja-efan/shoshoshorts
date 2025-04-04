import { useState } from 'react'
import { Character } from '@/types/character'

export const useCharacter = () => {
  const [characters, setCharacters] = useState<Character[]>([])

  const addCharacter = () => {
    if (characters.length >= 4) return

    const newCharacter: Character = {
      id: Date.now().toString(),
      name: "",
      gender: "male",
      voice: "male1",
      description: null,
    }
    setCharacters([...characters, newCharacter])
  }

  const updateCharacter = (id: string, field: keyof Character, value: any) => {
    setCharacters(characters.map((char) => {
      if (char.id === id) {
        // 성별이 변경될 때 해당 성별의 1번 목소리로 자동 설정
        if (field === "gender" && value) {
          return { ...char, [field]: value, voice: `${value}1` }
        }
        return { ...char, [field]: value }
      }
      return char
    }))
  }

  const removeCharacter = (id: string) => {
    setCharacters(characters.filter((char) => char.id !== id))
  }

  return {
    characters,
    addCharacter,
    updateCharacter,
    removeCharacter
  }
} 