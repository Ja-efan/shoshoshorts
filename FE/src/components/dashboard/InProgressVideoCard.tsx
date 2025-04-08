import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Clock, Loader2, CheckCircle, AlertCircle } from "lucide-react"
import { VideoData } from "@/types/video"
import { useVideoStatus } from "@/hooks/useVideoStatus"
import { useEffect, useState } from "react"

interface InProgressVideoCardProps {
  video: VideoData
  statusText?: string
}

// 처리 단계에 따른 한글 텍스트 매핑
const processingStepText: Record<string, string> = {
  SCRIPT_PROCESSING: "스크립트 처리 중",
  VOICE_GENERATING: "AI 음성 생성 중",
  IMAGE_GENERATING: "AI 이미지 생성 중",
  VOICE_COMPLETED: "AI 음성 생성 완료",
  IMAGE_COMPLETED: "AI 이미지 생성 완료",
  VIDEO_RENDERING: "영상 병합 중",
  VIDEO_RENDER_COMPLETED: "영상 병합 완료",
  VIDEO_UPLOADING: "영상 업로드 중"
};

// 처리 단계에 따른 배지 색상 매핑
const processingStepColor: Record<string, string> = {
  SCRIPT_PROCESSING: "bg-blue-100 text-blue-800",
  VOICE_GENERATING: "bg-purple-100 text-purple-800",
  IMAGE_GENERATING: "bg-green-100 text-green-800",
  VOICE_COMPLETED: "bg-teal-100 text-teal-800",
  IMAGE_COMPLETED: "bg-emerald-100 text-emerald-800",
  VIDEO_RENDERING: "bg-amber-100 text-amber-800",
  VIDEO_RENDER_COMPLETED: "bg-orange-100 text-orange-800",
  VIDEO_UPLOADING: "bg-red-100 text-red-800"
};

export function InProgressVideoCard({ video, statusText = "처리 중" }: InProgressVideoCardProps) {
  const { videoStatus, error, isConnected } = useVideoStatus(video.story_id);
  const [currentStatus, setCurrentStatus] = useState<string>(statusText);
  const [processingStep, setProcessingStep] = useState<string | undefined>(undefined);
  const [status, setStatus] = useState<string>("PROCESSING");

  // SSE 상태 업데이트
  useEffect(() => {
    console.log('videoStatus 변경됨:', videoStatus);
    
    if (videoStatus) {
      setStatus(videoStatus.status);
      
      if (videoStatus.status === "PROCESSING" && videoStatus.processingStep) {
        console.log('처리 단계 업데이트:', videoStatus.processingStep);
        setProcessingStep(videoStatus.processingStep);
        
        // 처리 단계에 따른 상태 텍스트 설정
        const stepText = processingStepText[videoStatus.processingStep];
        if (stepText) {
          setCurrentStatus(stepText);
        } else {
          setCurrentStatus("처리 중");
        }
      } else if (videoStatus.status === "PENDING") {
        setCurrentStatus("대기 중");
        setProcessingStep(undefined);
      } else if (videoStatus.status === "COMPLETED") {
        setCurrentStatus("완료됨");
        setProcessingStep(undefined);
      } else if (videoStatus.status === "FAILED") {
        setCurrentStatus("실패");
        setProcessingStep(undefined);
      } else {
        setCurrentStatus(statusText);
        setProcessingStep(undefined);
      }
    }
  }, [videoStatus, statusText]);

  // 상태에 따른 아이콘 및 스타일 결정
  const getStatusIcon = () => {
    if (status === "COMPLETED") {
      return <CheckCircle className="h-3 w-3 text-green-500" />;
    } else if (status === "FAILED") {
      return <AlertCircle className="h-3 w-3 text-red-500" />;
    } else {
      return <Loader2 className="h-3 w-3 animate-spin" />;
    }
  };

  // 처리 단계에 따른 배지 스타일 결정
  const getBadgeStyle = () => {
    if (processingStep && processingStepColor[processingStep]) {
      return processingStepColor[processingStep];
    }
    return "bg-gray-100 text-gray-800";
  };

  return (
    <Card className="overflow-hidden">
      <div className="relative aspect-video w-full">
        <div className="flex h-full w-full items-center justify-center bg-gray-100">
          <Clock className="h-12 w-12 text-gray-400" />
        </div>
        <div className="absolute inset-x-0 bottom-0 h-1 bg-gray-200">
          <div className="h-full bg-blue-600 animate-pulse"></div>
        </div>
      </div>
      <div className="p-4">
        <h3 className="font-medium">{video.title}</h3>
        <div className="mt-2 flex items-center gap-2">
          <Badge variant="secondary" className={`flex items-center gap-1 ${getBadgeStyle()}`}>
            {getStatusIcon()}
            {currentStatus}
          </Badge>
          {!isConnected && (
            <Badge variant="outline" className="text-xs text-red-500">
              연결 끊김
            </Badge>
          )}
        </div>
        {error && (
          <p className="mt-1 text-xs text-red-500">{error}</p>
        )}
      </div>
    </Card>
  )
} 