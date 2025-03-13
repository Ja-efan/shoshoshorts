// components/script/script-editor.tsx
"use client";

import { useState } from "react";
import { DragDropContext, Droppable } from "@hello-pangea/dnd";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import ScriptLine from "@/components/script-editor/ScriptLine";
import SettingsPanel from "@/components/script-editor/SettingsPanel";
import { useMediaQuery } from "@/hooks/use-media-query";
import { useScript } from "@/hooks/useScript";
import { exportScript } from "@/utils/script-helpers";

export default function ScriptEditor() {
  const {
    scriptLines,
    activeSettingsId,
    customSpeakers,
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

  return (
    <div className="grid md:grid-cols-3 gap-6">
      <div className="md:col-span-2 bg-white rounded-lg shadow-md p-6">
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
                      onDelete={handleDeleteLine}
                      isSettingsActive={activeSettingsId === line.id}
                      onToggleSettings={toggleSettings}
                      isMobile={isMobile}
                      customSpeakers={customSpeakers}
                    />

                    {/* Insert line button that appears on hover */}
                    {hoverIndex === index && (
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
            <Button onClick={() => exportScript(scriptLines)} className="flex-1" variant="secondary">
              Export to JSON
            </Button>

            <Button onClick={() => setShowAddSpeaker(!showAddSpeaker)} className="flex-1" variant="secondary">
              Manage Characters
            </Button>
          </div>

          {showAddSpeaker && (
            <div className="p-4 border rounded-lg mt-2">
              <h3 className="font-medium mb-2">Custom Characters</h3>
              <div className="flex gap-2 mb-4">
                <input
                  type="text"
                  value={newSpeakerName}
                  onChange={(e) => setNewSpeakerName(e.target.value)}
                  placeholder="New character name"
                  className="flex-1 px-3 py-2 border rounded-md"
                />
                <Button onClick={handleAddCustomSpeaker} variant="outline">
                  Add
                </Button>
              </div>

              {customSpeakers.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {customSpeakers.map((speaker) => (
                    <div key={speaker} className="flex items-center bg-gray-100 px-3 py-1 rounded-full">
                      <span>{speaker}</span>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-6 w-6 p-0 ml-1"
                        onClick={() => removeCustomSpeaker(speaker)}
                      >
                        ×
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
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