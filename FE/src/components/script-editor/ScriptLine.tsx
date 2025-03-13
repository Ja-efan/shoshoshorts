"use client"

import { useState } from "react"
import { Draggable } from "@hello-pangea/dnd"
import { Grip, Settings, Trash2, Check, X } from "lucide-react"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Dialog, DialogDescription, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog"
import { Slider } from "@/components/ui/slider"
import { Label } from "@/components/ui/label"
import type { ScriptLineType, Speaker, EmotionSettings } from "@/types/script-editor"
import { getAvatarColor, getAvatarInitial } from "@/utils/script-helpers"
interface ScriptLineProps {
  line: ScriptLineType
  index: number
  onContentChange: (id: string, content: string) => void
  onSpeakerChange: (id: string, type: Speaker) => void
  onEmotionsChange: (id: string, emotions: EmotionSettings) => void
  onDelete: (id: string) => void
}

export default function ScriptLine({
  line,
  index,
  onContentChange,
  onSpeakerChange,
  onEmotionsChange,
  onDelete,
}: ScriptLineProps) {
  const [isEditing, setIsEditing] = useState(false)
  const [editContent, setEditContent] = useState(line.content)
  const [showSettings, setShowSettings] = useState(false)
  const [emotions, setEmotions] = useState<EmotionSettings>(line.emotions)

  const handleSave = () => {
    onContentChange(line.id, editContent)
    setIsEditing(false)
  }

  const handleCancel = () => {
    setEditContent(line.content)
    setIsEditing(false)
  }

  const handleSaveSettings = () => {
    onEmotionsChange(line.id, emotions)
    setShowSettings(false)
  }

  const speakers: Speaker[] = ["Narrator", "Situation", "Speaker A", "Speaker B"]

  return (
    <Draggable draggableId={line.id} index={index}>
      {(provided) => (
        <div
          ref={provided.innerRef}
          {...provided.draggableProps}
          className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
        >
          <div {...provided.dragHandleProps} className="mt-2">
            <Grip className="h-5 w-5 text-gray-400" />
          </div>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Avatar className={`h-10 w-10 cursor-pointer ${getAvatarColor(line.type)}`}>
                <AvatarFallback>{getAvatarInitial(line.type)}</AvatarFallback>
              </Avatar>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start">
              {speakers.map((speaker) => (
                <DropdownMenuItem key={speaker} onClick={() => onSpeakerChange(line.id, speaker)}>
                  {speaker}
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>

          <div className="flex-1">
            {isEditing ? (
              <div className="space-y-2">
                <Textarea
                  value={editContent}
                  onChange={(e) => setEditContent(e.target.value)}
                  className="min-h-[80px]"
                />
                <div className="flex justify-end gap-2">
                  <Button size="sm" variant="ghost" onClick={handleCancel}>
                    <X className="h-4 w-4 mr-1" /> Cancel
                  </Button>
                  <Button size="sm" onClick={handleSave}>
                    <Check className="h-4 w-4 mr-1" /> Save
                  </Button>
                </div>
              </div>
            ) : (
              <div className="p-2 min-h-[40px] cursor-pointer" onClick={() => setIsEditing(true)}>
                <div className="text-sm font-medium text-gray-500 mb-1">{line.type}</div>
                <div>{line.content}</div>
              </div>
            )}
          </div>

          <div className="flex gap-2">
            <Button variant="ghost" size="icon" onClick={() => setShowSettings(true)}>
              <Settings className="h-5 w-5 text-gray-500" />
            </Button>
            <Button variant="ghost" size="icon" onClick={() => onDelete(line.id)}>
              <Trash2 className="h-5 w-5 text-gray-500" />
            </Button>
          </div>

          <Dialog open={showSettings} onOpenChange={setShowSettings}>
            <DialogContent className="max-w-md">
              
              <DialogHeader>
                <DialogTitle>Emotion Settings</DialogTitle>
              </DialogHeader>
              <DialogDescription>각 대사의 보이스 요소를 세팅 하기 위한 Dialog</DialogDescription>
              <div className="space-y-4 py-4">
                {Object.entries(emotions)
                  .filter(([key]) => key !== "speakingRate")
                  .map(([emotion, value]) => (
                    <div key={emotion} className="space-y-2">
                      <div className="flex justify-between">
                        <Label htmlFor={`${emotion}-${line.id}`} className="capitalize">
                          {emotion}
                        </Label>
                        <span className="text-sm text-gray-500">{value.toFixed(1)}</span>
                      </div>
                      <Slider
                        id={`${emotion}-${line.id}`}
                        min={0}
                        max={1}
                        step={0.1}
                        value={[value]}
                        onValueChange={(values) => setEmotions({ ...emotions, [emotion]: values[0] })}
                      />
                    </div>
                  ))}

                <div className="space-y-2 pt-2 border-t">
                  <div className="flex justify-between">
                    <Label htmlFor={`speakingRate-${line.id}`}>Speaking Rate</Label>
                    <span className="text-sm text-gray-500">{emotions.speakingRate}</span>
                  </div>
                  <Slider
                    id={`speakingRate-${line.id}`}
                    min={1}
                    max={10}
                    step={1}
                    value={[emotions.speakingRate]}
                    onValueChange={(values) => setEmotions({ ...emotions, speakingRate: values[0] })}
                  />
                </div>
              </div>

              <DialogFooter>
                <Button onClick={handleSaveSettings}>Save Changes</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      )}
    </Draggable>
  )
}