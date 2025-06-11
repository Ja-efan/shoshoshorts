import { useState, useEffect } from "react";
import { ScriptLineType, EmotionSettings } from "@/types/script-editor/script-editor";

interface SettingsPanelProps {
  line: ScriptLineType;
  onEmotionsChange: (id: string, emotions: EmotionSettings) => void;
}

export default function SettingsPanel({ line, onEmotionsChange }: SettingsPanelProps) {
  const [emotions, setEmotions] = useState<EmotionSettings>(line.emotions);

  useEffect(() => {
    setEmotions(line.emotions);
  }, [line]);

  const handleChange = (key: keyof EmotionSettings, value: number) => {
    const updatedEmotions = { ...emotions, [key]: value };
    setEmotions(updatedEmotions);
    onEmotionsChange(line.id, updatedEmotions);
  };

  return (
    <div className="space-y-4">
      {Object.entries(emotions)
        .filter(([key]) => key !== "speakingRate")
        .map(([emotion, value]) => (
          <div key={emotion} className="space-y-2">
            <div className="flex justify-between">
              <label htmlFor={`${emotion}-${line.id}`} className="text-sm font-medium capitalize">
                {emotion}
              </label>
              <span className="text-sm text-gray-500">{value.toFixed(1)}</span>
            </div>
            <input
              id={`${emotion}-${line.id}`}
              type="range"
              min={0}
              max={1}
              step={0.1}
              value={value}
              onChange={(e) => handleChange(emotion as keyof EmotionSettings, Number.parseFloat(e.target.value))}
              className="w-full"
            />
          </div>
        ))}

      <div className="space-y-2 pt-2 border-t">
        <div className="flex justify-between">
          <label htmlFor={`speakingRate-${line.id}`} className="text-sm font-medium">
            Speaking Rate
          </label>
          <span className="text-sm text-gray-500">{emotions.speakingRate}</span>
        </div>
        <input
          id={`speakingRate-${line.id}`}
          type="range"
          min={1}
          max={10}
          step={1}
          value={emotions.speakingRate}
          onChange={(e) => handleChange("speakingRate", Number.parseInt(e.target.value))}
          className="w-full"
        />
      </div>
    </div>
  );
}