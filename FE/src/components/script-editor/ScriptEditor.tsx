"use client"

import { DragDropContext, Droppable } from "@hello-pangea/dnd"
import { Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import ScriptLine from "./scriptLine"
import useScript from "@/hooks/useScript"

export default function ScriptEditor() {
  const {
    scriptLines,
    handleDragEnd,
    handleContentChange,
    handleSpeakerChange,
    handleEmotionsChange,
    handleDeleteLine,
    addNewLine
  } = useScript()

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <DragDropContext onDragEnd={handleDragEnd}>
        <Droppable droppableId="script-lines">
          {(provided) => (
            <div {...provided.droppableProps} ref={provided.innerRef} className="space-y-4">
              {scriptLines.map((line, index) => (
                <ScriptLine
                  key={line.id}
                  line={line}
                  index={index}
                  onContentChange={handleContentChange}
                  onSpeakerChange={handleSpeakerChange}
                  onEmotionsChange={handleEmotionsChange}
                  onDelete={handleDeleteLine}
                />
              ))}
              {provided.placeholder}
            </div>
          )}
        </Droppable>
      </DragDropContext>

      <Button onClick={addNewLine} className="mt-6 w-full" variant="outline">
        <Plus className="mr-2 h-4 w-4" />
        Add New Line
      </Button>
    </div>
  )
}