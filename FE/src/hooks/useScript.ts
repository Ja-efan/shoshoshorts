"use client"

import { useState } from "react"
import type { ScriptLineType, Speaker, EmotionSettings } from "@/types/script-editor"
import { createNewScriptLine } from "@/utils/script-helpers"

interface UseScriptProps {
  initialLines?: ScriptLineType[]
}

export function useScript({ initialLines = [] }: UseScriptProps = {}) {
  const [scriptLines, setScriptLines] = useState<ScriptLineType[]>(initialLines)
  const [activeSettingsId, setActiveSettingsId] = useState<string | null>(null)
  const [hoverIndex, setHoverIndex] = useState<number | null>(null)

  const handleDragEnd = (source: number, destination: number | null) => {
    if (destination === null) return

    const items = Array.from(scriptLines)
    const [reorderedItem] = items.splice(source, 1)
    items.splice(destination, 0, reorderedItem)

    setScriptLines(items)
  }

  const updateContent = (id: string, content: string) => {
    setScriptLines(scriptLines.map((line) => (line.id === id ? { ...line, content } : line)))
  }

  const updateSpeaker = (id: string, type: Speaker) => {
    setScriptLines(scriptLines.map((line) => (line.id === id ? { ...line, type } : line)))
  }

  const updateEmotions = (id: string, emotions: EmotionSettings) => {
    setScriptLines(scriptLines.map((line) => (line.id === id ? { ...line, emotions } : line)))
  }

  const deleteLine = (id: string) => {
    setScriptLines(scriptLines.filter((line) => line.id !== id))
    if (activeSettingsId === id) {
      setActiveSettingsId(null)
    }
  }

  const addLine = (emotions: EmotionSettings, type?: Speaker, content?: string) => {
    setScriptLines([
      ...scriptLines,
      createNewScriptLine(type, content, emotions),
    ])
  }

  const insertLine = (index: number, emotions: EmotionSettings, type?: Speaker, content?: string) => {
    const newLine = createNewScriptLine(type, content, emotions)
    const newLines = [...scriptLines]
    newLines.splice(index + 1, 0, newLine)
    setScriptLines(newLines)
  }

  const toggleSettings = (id: string) => {
    setActiveSettingsId(activeSettingsId === id ? null : id)
  }

  const clearHoverIndex = () => setHoverIndex(null)
  const setHover = (index: number) => setHoverIndex(index)

  return {
    scriptLines,
    activeSettingsId,
    hoverIndex,
    handleDragEnd,
    updateContent,
    updateSpeaker,
    updateEmotions,
    deleteLine,
    addLine,
    insertLine,
    toggleSettings,
    clearHoverIndex,
    setHover,
  }
}