import { useState } from "react"
import { type DropResult } from "@hello-pangea/dnd"
import { ScriptLineType, Speaker, EmotionSettings, defaultEmotions } from "@/types/script-editor"

export default function useScript(initialLines?: ScriptLineType[]) {
  const [scriptLines, setScriptLines] = useState<ScriptLineType[]>(initialLines || [
    {
      id: "line-1",
      type: "Situation",
      content: "A quiet café on a rainy afternoon.",
      emotions: defaultEmotions,
    },
    {
      id: "line-2",
      type: "Narrator",
      content: "Two strangers sit at adjacent tables, occasionally glancing at each other.",
      emotions: defaultEmotions,
    },
    {
      id: "line-3",
      type: "Speaker A",
      content: "Excuse me, is this seat taken?",
      emotions: { ...defaultEmotions, happiness: 0.3, neutral: 0.7 },
    },
    {
      id: "line-4",
      type: "Speaker B",
      content: "No, please feel free to take it.",
      emotions: { ...defaultEmotions, surprise: 0.2, neutral: 0.8 },
    },
  ])

  const handleDragEnd = (result: DropResult) => {
    if (!result.destination) return

    const items = Array.from(scriptLines)
    const [reorderedItem] = items.splice(result.source.index, 1)
    items.splice(result.destination.index, 0, reorderedItem)

    setScriptLines(items)
  }

  const handleContentChange = (id: string, content: string) => {
    setScriptLines(scriptLines.map((line) => (line.id === id ? { ...line, content } : line)))
  }

  const handleSpeakerChange = (id: string, type: Speaker) => {
    setScriptLines(scriptLines.map((line) => (line.id === id ? { ...line, type } : line)))
  }

  const handleEmotionsChange = (id: string, emotions: EmotionSettings) => {
    setScriptLines(scriptLines.map((line) => (line.id === id ? { ...line, emotions } : line)))
  }

  const handleDeleteLine = (id: string) => {
    setScriptLines(scriptLines.filter((line) => line.id !== id))
  }

  const addNewLine = () => {
    const newId = `line-${Date.now()}`
    setScriptLines([
      ...scriptLines,
      {
        id: newId,
        type: "Speaker A",
        content: "New dialogue line",
        emotions: defaultEmotions,
      },
    ])
  }

  return {
    scriptLines,
    handleDragEnd,
    handleContentChange,
    handleSpeakerChange,
    handleEmotionsChange,
    handleDeleteLine,
    addNewLine
  }
}
