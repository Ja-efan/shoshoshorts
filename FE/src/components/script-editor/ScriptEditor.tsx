"use client"

import { DragDropContext, Droppable, type DropResult } from "@hello-pangea/dnd"
import { Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import ScriptLine from "@/components/script-editor/ScriptLine"
import { SettingsPanel } from "@/components/script-editor/SettingsPanel"
import { useMediaQuery } from "@/hooks/use-media-query"
import { useScript } from "@/hooks/useScript"
import { ScriptLineType } from "@/types/script-editor"
import { defaultEmotions } from "@/types/script-editor"

// 초기 스크립트 데이터
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
]

export default function ScriptEditor() {
  const isMobile = useMediaQuery("(max-width: 768px)")
  const {
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
  } = useScript({ initialLines: initialScriptLines })

  const onDragEnd = (result: DropResult) => {
    if (!result.destination) return
    handleDragEnd(result.source.index, result.destination.index)
  }

  return (
    <div className="grid md:grid-cols-3 gap-6">
      <div className="md:col-span-2 bg-white rounded-lg shadow-md p-6">
        <DragDropContext onDragEnd={onDragEnd}>
          <Droppable droppableId="script-lines">
            {(provided) => (
              <div
                {...provided.droppableProps}
                ref={provided.innerRef}
                className="space-y-4"
                onMouseLeave={clearHoverIndex}
              >
                {scriptLines.map((line, index) => (
                  <div key={line.id} className="relative" onMouseEnter={() => setHover(index)}>
                    <ScriptLine
                      line={line}
                      index={index}
                      onContentChange={updateContent}
                      onSpeakerChange={updateSpeaker}
                      onEmotionsChange={updateEmotions}
                      onDelete={deleteLine}
                      isSettingsActive={activeSettingsId === line.id}
                      onToggleSettings={toggleSettings}
                      isMobile={isMobile}
                    />

                    {/* Insert line button that appears on hover */}
                    {hoverIndex === index && (
                      <div className="absolute left-1/2 transform -translate-x-1/2 -top-3 z-10">
                        <Button
                          variant="outline"
                          size="icon"
                          className="h-6 w-6 rounded-full bg-white shadow-md border-gray-200 hover:bg-gray-100"
                          onClick={() => insertLine(index, defaultEmotions)}
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

        <Button onClick={() => addLine(defaultEmotions)} className="mt-6 w-full" variant="outline">
          <Plus className="mr-2 h-4 w-4" />
          Add New Line
        </Button>
      </div>

      {!isMobile && activeSettingsId && (
        <div className="md:col-span-1 relative">
          <div className="sticky top-4 bg-white rounded-lg shadow-md p-6 max-h-[calc(100vh-2rem)] overflow-y-auto">
            <h3 className="font-medium mb-4">Emotion Settings</h3>
            <SettingsPanel
              key={activeSettingsId}
              line={scriptLines.find((line) => line.id === activeSettingsId)!}
              onEmotionsChange={updateEmotions}
            />
          </div>
        </div>
      )}
    </div>
  )
}