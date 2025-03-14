import { useState } from "react";
import { Draggable } from "@hello-pangea/dnd";
import { Grip, Settings, Trash2, ChevronUp } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Slider } from "@/components/ui/slider";
import { Label } from "@/components/ui/label";
import { ScriptLineType, Speaker, EmotionSettings, Character } from "@/types/script-editor/script-editor";
import { getLineStyles, getAvatarColor, getAvatarInitial } from "@/utils/script-editor/script-helpers";

interface ScriptLineProps {
  line: ScriptLineType;
  index: number;
  onContentChange: (id: string, content: string) => void;
  onSpeakerChange: (id: string, type: Speaker) => void;
  onEmotionsChange: (id: string, emotions: EmotionSettings) => void;
  onDeleteLine: (id: string) => void;
  onToggleSettings: (id: string) => void;
  isSettingsOpen: boolean;
  isMobile: boolean;
  characters: Character[];
  onInsertLine: (index: number) => void;
}

export default function ScriptLine({
  line,
  index,
  onContentChange,
  onSpeakerChange,
  onEmotionsChange,
  onDeleteLine,
  onToggleSettings,
  isSettingsOpen,
  isMobile,
  characters,
}: ScriptLineProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(line.content);
  const [emotions, setEmotions] = useState<EmotionSettings>(line.emotions);
  const [showMobileSettings, setShowMobileSettings] = useState(false);

  const handleBlur = () => {
    onContentChange(line.id, editContent);
    setIsEditing(false);
  };

  const handleEmotionChange = (key: keyof EmotionSettings, value: number) => {
    const updatedEmotions = { ...emotions, [key]: value };
    setEmotions(updatedEmotions);
    onEmotionsChange(line.id, updatedEmotions);
  };

  const toggleMobileSettings = () => {
    setShowMobileSettings(!showMobileSettings);
  };

  const speakers: Speaker[] = characters.map(c => c.name as Speaker);

  return (
    <Draggable draggableId={line.id} index={index}>
      {(provided) => (
        <div className="space-y-2 relative">
          <div
            ref={provided.innerRef}
            {...provided.draggableProps}
            className={`flex items-start gap-3 p-3 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors ${getLineStyles(line.type)}`}
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
                    onBlur={handleBlur}
                    autoFocus
                    className="min-h-[80px]"
                  />
                </div>
              ) : (
                <div
                  className="p-2 min-h-[40px] cursor-pointer"
                  onClick={() => {
                    setIsEditing(true);
                    setEditContent(line.content); // Ensure we have the latest content
                  }}
                >
                  <div className="text-sm font-medium text-gray-500 mb-1">{line.type}</div>
                  <div>{line.content}</div>
                </div>
              )}
            </div>

            <div className="flex gap-2">
              <Button
                variant="ghost"
                size="icon"
                onClick={() => {
                  if (isMobile) {
                    toggleMobileSettings();
                  }
                  onToggleSettings(line.id);
                }}
                className={isSettingsOpen && !isMobile ? "bg-gray-200" : ""}
              >
                <Settings className="h-5 w-5 text-gray-500" />
              </Button>
              <Button variant="ghost" size="icon" onClick={() => onDeleteLine(line.id)}>
                <Trash2 className="h-5 w-5 text-gray-500" />
              </Button>
            </div>
          </div>

          {/* Mobile Settings Panel */}
          {isMobile && showMobileSettings && (
            <div className="p-4 bg-gray-50 rounded-lg border border-gray-200 ml-8">
              <div className="flex justify-between items-center mb-3">
                <h4 className="font-medium">Emotion Settings</h4>
                <Button variant="ghost" size="sm" onClick={toggleMobileSettings}>
                  <ChevronUp className="h-4 w-4" />
                </Button>
              </div>
              <div className="space-y-4">
                {Object.entries(emotions)
                  .filter(([key]) => key !== "speakingRate")
                  .map(([emotion, value]) => (
                    <div key={emotion} className="space-y-2">
                      <div className="flex justify-between">
                        <Label htmlFor={`mobile-${emotion}-${line.id}`} className="capitalize">
                          {emotion}
                        </Label>
                        <span className="text-sm text-gray-500">{value.toFixed(1)}</span>
                      </div>
                      <Slider
                        id={`mobile-${emotion}-${line.id}`}
                        min={0}
                        max={1}
                        step={0.1}
                        value={[value]}
                        onValueChange={(values) => handleEmotionChange(emotion as keyof EmotionSettings, values[0])}
                      />
                    </div>
                  ))}

                <div className="space-y-2 pt-2 border-t">
                  <div className="flex justify-between">
                    <Label htmlFor={`mobile-speakingRate-${line.id}`}>Speaking Rate</Label>
                    <span className="text-sm text-gray-500">{emotions.speakingRate}</span>
                  </div>
                  <Slider
                    id={`mobile-speakingRate-${line.id}`}
                    min={1}
                    max={10}
                    step={1}
                    value={[emotions.speakingRate]}
                    onValueChange={(values) => handleEmotionChange("speakingRate", values[0])}
                  />
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </Draggable>
  );
}