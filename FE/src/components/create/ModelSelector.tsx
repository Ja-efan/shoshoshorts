import { useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { HelpTooltip } from "@/components/ui/help-tooltip";
import { ModelType } from "@/types/voice";
import { modelDescriptions } from "@/constants/voiceData";
import { toast } from "react-hot-toast";

interface ModelSelectorProps {
  showModelSelector: boolean;
  setShowModelSelector: (value: boolean) => void;
  voiceModels: ModelType[];
  setVoiceModels: (models: ModelType[]) => void;
  imageModels: ModelType[];
  setImageModels: (models: ModelType[]) => void;
  onVoiceModelChange: () => void;
}

export function ModelSelector({
  showModelSelector,
  setShowModelSelector,
  voiceModels,
  setVoiceModels,
  imageModels,
  setImageModels,
  onVoiceModelChange
}: ModelSelectorProps) {
  const modelSelectorRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (modelSelectorRef.current && !modelSelectorRef.current.contains(event.target as Node)) {
        setShowModelSelector(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [setShowModelSelector]);

  const selectVoiceModel = (index: number) => {
    onVoiceModelChange();
    
    setVoiceModels(
      voiceModels.map((model, i) => ({
        ...model,
        isSelected: i === index,
      }))
    );
    
    toast("모델별로 목소리의 차이가 있을 수 있습니다.", {
      icon: "⚠️",
      duration: 3000,
    });
  };

  const selectImageModel = (index: number) => {
    const model = imageModels[index];
    if (model.name === "Stable Diffusion") {
      toast("아직 개발 중에 있습니다.", {
        icon: "🔨",
        duration: 2000,
      });
      return;
    }
    setImageModels(
      imageModels.map((model, i) => ({
        ...model,
        isSelected: i === index,
      }))
    );
  };

  const toggleModelSelector = () => {
    setShowModelSelector(!showModelSelector);
  };

  return (
    <div className="relative mb-6" ref={modelSelectorRef}>
      <Button
        onClick={toggleModelSelector}
        variant="outline"
        className="w-full opacity-50 hover:opacity-100 transition-opacity"
      >
        AI 모델 설정
      </Button>

      {showModelSelector && (
        <div className="absolute z-10 w-full mt-2 p-4 bg-white border rounded-lg shadow-lg">
          <div className="space-y-4">
            <div>
              <h4 className="text-sm font-medium mb-2 flex items-center gap-2">
                음성 모델
                <HelpTooltip content="각 모델은 서로 다른 음성 특성을 가지고 있습니다. 모델을 변경하면 현재 선택된 음성 설정이 초기화될 수 있습니다." />
              </h4>
              <div className="flex gap-2">
                {voiceModels.map((model, index) => (
                  <div key={model.name} className="relative flex-1">
                    <Button
                      onClick={() => selectVoiceModel(index)}
                      variant={model.isSelected ? "default" : "outline"}
                      className="w-full"
                    >
                      <img
                        src={model.logo}
                        alt={model.name}
                        className="h-4 w-4 mr-2"
                      />
                      {model.name}
                    </Button>
                    <div className="absolute -right-1 -top-1">
                      <HelpTooltip content={modelDescriptions[model.name as keyof typeof modelDescriptions]} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div>
              <h4 className="text-sm font-medium mb-2 flex items-center gap-2">
                이미지 모델
                <HelpTooltip content="각 모델은 서로 다른 이미지 생성 스타일을 가지고 있습니다." />
              </h4>
              <div className="flex gap-2">
                {imageModels.map((model, index) => (
                  <div key={model.name} className="relative flex-1">
                    <Button
                      onClick={() => selectImageModel(index)}
                      variant={model.isSelected ? "default" : "outline"}
                      className="w-full"
                      disabled={model.name === "Stable Diffusion"}
                    >
                      <img
                        src={model.logo}
                        alt={model.name}
                        className="h-4 w-4 mr-2"
                      />
                      {model.name}
                    </Button>
                    <div className="absolute -right-1 -top-1">
                      <HelpTooltip content={model.name === "Stable Diffusion" 
                        ? "더 저렴한 비용으로 안정적인 이미지 생성이 가능한 모델입니다. 다양한 스타일의 이미지를 효율적으로 생성할 수 있습니다. (개발 중)" 
                        : "고품질 이미지 생성이 가능한 모델입니다. 세부적인 디테일을 잘 표현합니다."} 
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
} 