// components/script/script-editor.tsx
"use client";

import { useState } from "react";
import { DragDropContext, Droppable } from "@hello-pangea/dnd";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import ScriptLine from "@/components/script-editor/ScriptLine";
import SettingsPanel from "@/components/script-editor/SettingsPanel";
import { useMediaQuery } from "@/hooks/use-media-query";
import { useScript } from "@/hooks/script-editor/useScript";
import { dummyScriptData, convertScriptDataToScriptLines, convertScriptLinesToScriptData } from "@/utils/script-editor/script-helpers";

export default function ScriptEditor() {
  const {
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
  } = useScript();

  const [hoverIndex, setHoverIndex] = useState<number | null>(null);
  const isMobile = useMediaQuery("(max-width: 768px)");
  const [showAddSpeaker, setShowAddSpeaker] = useState(false);
  const [newSpeakerName, setNewSpeakerName] = useState("");

  const handleAddCustomSpeaker = () => {
    if (addCustomSpeaker(newSpeakerName)) {
      setNewSpeakerName("");
    }
  };

  const handleRemoveCustomSpeaker = (speaker: string) => {
    if (!removeCustomSpeaker(speaker)) {
      alert("이 캐릭터는 현재 대사에 사용 중이어서 삭제할 수 없습니다.");
    }
  };

  const handleLoadDummyData = () => {
    const newScriptLines = convertScriptDataToScriptLines(dummyScriptData);
    setScriptLines(newScriptLines);
    updateCharacters(dummyScriptData.characters);
  };

  const handleExportScriptData = () => {
    const scriptData = convertScriptLinesToScriptData(scriptLines);
    const jsonData = JSON.stringify(scriptData, null, 2);
    const blob = new Blob([jsonData], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "script-data.json";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="grid md:grid-cols-3 gap-6">
      <div className="md:col-span-2 bg-white rounded-lg shadow-md p-6">
        <div className="mb-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-medium">캐릭터 관리</h3>
            <Button 
              onClick={() => setShowAddSpeaker(!showAddSpeaker)} 
              variant="outline" 
              size="sm"
            >
              {showAddSpeaker ? "닫기" : "캐릭터 추가"}
            </Button>
          </div>
          
          {showAddSpeaker && (
            <div className="flex gap-2 mb-4">
              <input
                type="text"
                value={newSpeakerName}
                onChange={(e) => setNewSpeakerName(e.target.value)}
                placeholder="새 캐릭터 이름"
                className="flex-1 px-3 py-2 border rounded-md"
              />
              <Button onClick={handleAddCustomSpeaker} variant="outline">
                추가
              </Button>
            </div>
          )}

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            {characters.map((character) => (
              <div 
                key={character.name} 
                className={`flex flex-col items-center p-4 border rounded-lg ${
                  character.name === "Narrator" || character.name === "Situation"
                    ? "bg-gray-50"
                    : "hover:bg-gray-50"
                }`}
              >
                <div className="w-16 h-16 rounded-full bg-gray-200 mb-2 flex items-center justify-center">
                  <span className="text-lg font-medium">
                    {character.name.charAt(0)}
                  </span>
                </div>
                <span className="text-sm font-medium mb-2">{character.name}</span>
                {character.name !== "Narrator" && character.name !== "Situation" && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-red-500 hover:text-red-600"
                    onClick={() => handleRemoveCustomSpeaker(character.name)}
                  >
                    삭제
                  </Button>
                )}
              </div>
            ))}
          </div>
        </div>

        <DragDropContext onDragEnd={handleDragEnd}>
          <Droppable droppableId="script-lines">
            {(provided) => (
              <div
                {...provided.droppableProps}
                ref={provided.innerRef}
                className="space-y-4"
                onMouseLeave={() => setHoverIndex(null)}
              >
                {scriptLines.map((line, index) => (
                  <div key={line.id} className="relative" onMouseEnter={() => setHoverIndex(index)}>
                    <ScriptLine
                      line={line}
                      index={index}
                      onContentChange={handleContentChange}
                      onSpeakerChange={handleSpeakerChange}
                      onEmotionsChange={handleEmotionsChange}
                      onDeleteLine={handleDeleteLine}
                      onToggleSettings={toggleSettings}
                      isSettingsOpen={activeSettingsId === line.id}
                      isMobile={isMobile}
                      characters={characters}
                      onInsertLine={insertNewLine}
                    />

                    {/* Insert line button - visible on hover for desktop, always visible on mobile */}
                    {(isMobile || hoverIndex === index) && (
                      <div className="absolute left-1/2 transform -translate-x-1/2 -top-3 z-10">
                        <Button
                          variant="outline"
                          size="icon"
                          className="h-6 w-6 rounded-full bg-white shadow-md border-gray-200 hover:bg-gray-100"
                          onClick={() => insertNewLine(index)}
                        >
                          <Plus className="h-3 w-3" />
                        </Button>
                      </div>
                    )}
                  </div>
                ))}
                {provided.placeholder}
              </div>
            )}
          </Droppable>
        </DragDropContext>

        <div className="mt-6 flex flex-col gap-4">
          <Button onClick={addNewLine} className="w-full" variant="outline">
            <Plus className="mr-2 h-4 w-4" />
            Add New Line
          </Button>

          <div className="flex gap-2">
            <Button onClick={handleLoadDummyData} className="flex-1" variant="secondary">
              더미 데이터 불러오기
            </Button>
            <Button onClick={handleExportScriptData} className="flex-1" variant="secondary">
              스크립트 데이터 내보내기
            </Button>
          </div>
        </div>
      </div>

      {!isMobile && activeSettingsId && (
        <div className="md:col-span-1 relative">
          <div className="sticky top-4 bg-white rounded-lg shadow-md p-6 max-h-[calc(100vh-2rem)] overflow-y-auto">
            <h3 className="font-medium mb-4">Emotion Settings</h3>
            <SettingsPanel
              key={activeSettingsId}
              line={scriptLines.find((line) => line.id === activeSettingsId)!}
              onEmotionsChange={handleEmotionsChange}
            />
          </div>
        </div>
      )}
    </div>
  );
}