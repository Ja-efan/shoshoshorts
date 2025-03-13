import { DragDropContext, Droppable } from "@hello-pangea/dnd"
import { Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import ScriptLine from "./scriptLine"
import { SettingsPanel } from "./SettingsPanel"
import useScript from "@/hooks/useScript"

export default function ScriptEditor() {
  const {
    scriptLines,
    activeSettingsId,
    isMobile,
    handleDragEnd,
    handleContentChange,
    handleSpeakerChange,
    handleEmotionsChange,
    handleDeleteLine,
    addNewLine,
    insertNewLine,
    toggleSettings
  } = useScript()

  const activeLine = activeSettingsId 
    ? scriptLines.find(line => line.id === activeSettingsId)
    : undefined

  return (
    <div className="grid md:grid-cols-3 gap-6">
      <div className="md:col-span-2 bg-white rounded-lg shadow-md p-6">
        <DragDropContext onDragEnd={handleDragEnd}>
          <Droppable droppableId="script-lines">
            {(provided) => (
              <div {...provided.droppableProps} ref={provided.innerRef} className="space-y-1">
                {scriptLines.map((line, index) => (
                  <div key={line.id} className="space-y-1">
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
                    />

                    <div className="flex justify-center">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-6 text-xs text-gray-400 hover:text-gray-600"
                        onClick={() => insertNewLine(index)}
                      >
                        <Plus className="h-3 w-3 mr-1" />
                        Insert line
                      </Button>
                    </div>
                  </div>
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

      {!isMobile && activeLine && (
        <div className="md:col-span-1 bg-white rounded-lg shadow-md p-6">
          <h3 className="font-medium mb-4">Emotion Settings</h3>
          <SettingsPanel
            line={activeLine}
            onEmotionsChange={handleEmotionsChange}
          />
        </div>
      )}
    </div>
  )
}