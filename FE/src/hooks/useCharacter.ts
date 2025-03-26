import { useState } from 'react'
import { Character } from '@/types/character'

export const useCharacter = () => {
  const [characters, setCharacters] = useState<Character[]>([])

  const addCharacter = () => {
    if (characters.length >= 4) return

    const newCharacter: Character = {
      id: Date.now().toString(),
      name: "",
      gender: null,
      voice: null,
      description: null,
    }
    setCharacters([...characters, newCharacter])
  }

  const updateCharacter = (id: string, field: keyof Character, value: any) => {
    setCharacters(characters.map((char) => (char.id === id ? { ...char, [field]: value } : char)))
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