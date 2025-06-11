import { useState } from "react";
import type { DropResult } from "@hello-pangea/dnd";
import { ScriptLineType, Speaker, EmotionSettings, defaultEmotions, Character } from "@/types/script-editor/script-editor";
import { createNewScriptLine } from "@/utils/script-editor/script-helpers";

const defaultCharacters: Character[] = [
  {
    name: "Narrator",
    gender: "male",
    properties: "내레이션"
  },
  {
    name: "Situation",
    gender: "male",
    properties: "상황 설명"
  }
];

const initialScriptLines: ScriptLineType[] = [
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
  }
];

export function useScript() {
  const [scriptLines, setScriptLines] = useState<ScriptLineType[]>(initialScriptLines);
  const [activeSettingsId, setActiveSettingsId] = useState<string | null>(null);
  const [characters, setCharacters] = useState<Character[]>(defaultCharacters);
  
  const handleDragEnd = (result: DropResult) => {
    if (!result.destination) return;

    const items = Array.from(scriptLines);
    const [reorderedItem] = items.splice(result.source.index, 1);
    items.splice(result.destination.index, 0, reorderedItem);

    setScriptLines(items);
  };

  const handleContentChange = (id: string, content: string) => {
    setScriptLines(scriptLines.map((line) => (line.id === id ? { ...line, content } : line)));
  };

  const handleSpeakerChange = (id: string, type: Speaker) => {
    setScriptLines(scriptLines.map((line) => (line.id === id ? { ...line, type } : line)));
  };

  const handleEmotionsChange = (id: string, emotions: EmotionSettings) => {
    setScriptLines(scriptLines.map((line) => (line.id === id ? { ...line, emotions } : line)));
  };

  const handleDeleteLine = (id: string) => {
    setScriptLines(scriptLines.filter((line) => line.id !== id));
    if (activeSettingsId === id) {
      setActiveSettingsId(null);
    }
  };

  const addNewLine = () => {
    setScriptLines([...scriptLines, createNewScriptLine("Situation", "새로운 상황")]);
  };

  const insertNewLine = (index: number) => {
    const newLines = [...scriptLines];
    newLines.splice(index, 0, createNewScriptLine("Situation", "새로운 상황"));
    setScriptLines(newLines);
  };

  const toggleSettings = (id: string) => {
    setActiveSettingsId(activeSettingsId === id ? null : id);
  };

  const addCustomSpeaker = (speakerName: string) => {
    if (speakerName.trim()) {
      const newCharacter: Character = {
        name: speakerName.trim(),
        gender: "male",
        properties: "새로운 캐릭터"
      };
      setCharacters([...characters, newCharacter]);
      return true;
    }
    return false;
  };

  const removeCustomSpeaker = (speakerName: string) => {
    const isSpeakerInUse = scriptLines.some(line => line.type === speakerName);
    if (isSpeakerInUse) {
      return false;
    }
    setCharacters(characters.filter((c) => c.name !== speakerName));
    return true;
  };

  const updateCharacters = (newCharacters: Character[]) => {
    const fixedCharacters = characters.filter(c => c.name === "Narrator" || c.name === "Situation");
    setCharacters([...fixedCharacters, ...newCharacters.filter(c => c.name !== "Narrator" && c.name !== "Situation")]);
  };

  return {
    scriptLines,
    activeSettingsId,
    characters,
    handleDragEnd,
    handleContentChange,
    handleSpeakerChange,
    handleEmotionsChange,
    handleDeleteLine,
    addNewLine,
    insertNewLine,
    toggleSettings,
    addCustomSpeaker,
    removeCustomSpeaker,
    setScriptLines,
    updateCharacters,
  };
}