import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Clock, Loader2, CheckCircle, AlertCircle } from "lucide-react"
import { VideoData } from "@/types/video"
import { useVideoStatus } from "@/hooks/useVideoStatus"
import { useEffect, useState, useRef } from "react"

interface InProgressVideoCardProps {
  video: VideoData
  statusText?: string
  onStatusChange?: (storyId: string, newStatus: "FAILED" | "COMPLETED" | "PROCESSING" | "PENDING") => void
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
  SCRIPT_PROCESSING: "bg-blue-100 text-blue-800 border-blue-200",
  VOICE_GENERATING: "bg-purple-100 text-purple-800 border-purple-200",
  IMAGE_GENERATING: "bg-green-100 text-green-800 border-green-200",
  VOICE_COMPLETED: "bg-teal-100 text-teal-800 border-teal-200",
  IMAGE_COMPLETED: "bg-emerald-100 text-emerald-800 border-emerald-200",
  VIDEO_RENDERING: "bg-amber-100 text-amber-800 border-amber-200",
  VIDEO_RENDER_COMPLETED: "bg-orange-100 text-orange-800 border-orange-200",
  VIDEO_UPLOADING: "bg-red-100 text-red-800 border-red-200"
};

export function InProgressVideoCard({ video, statusText = "처리 중", onStatusChange }: InProgressVideoCardProps) {
  const { videoStatus, error, connect, disconnect, isConnected } = useVideoStatus(video.story_id);
  const [currentStatus, setCurrentStatus] = useState<string>(statusText);
  const [processingStep, setProcessingStep] = useState<string | undefined>(undefined);
  const [status, setStatus] = useState<string>("PROCESSING");
  const connectionAttempted = useRef(false);
  const reconnectTimeoutRef = useRef<number | null>(null);

  // 문서 가시성 변경 감지를 위한 useEffect
  useEffect(() => {
    let reconnectTimeout: number;
    
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        console.log(`[${video.story_id}] 탭이 활성화되었습니다. SSE 연결 확인`);
        
        // 기존 재연결 타임아웃이 있다면 취소
        if (reconnectTimeoutRef.current) {
          window.clearTimeout(reconnectTimeoutRef.current);
          reconnectTimeoutRef.current = null;
        }
        
        // 상태가 완료나 실패가 아니고 연결이 없는 경우에만 연결 시도
        if (!isConnected && status !== "COMPLETED" && status !== "FAILED") {
          console.log(`[${video.story_id}] 연결이 없습니다. 새로 연결합니다.`);
          // 약간의 지연 후 연결 시도 (여러 카드가 있을 경우 동시에 연결 시도하는 것 방지)
          reconnectTimeout = window.setTimeout(() => {
            connect();
          }, 300);
          reconnectTimeoutRef.current = reconnectTimeout;
        }
      } else {
        console.log(`[${video.story_id}] 탭이 비활성화되었습니다.`);
        // 탭 비활성화 시에는 연결을 유지합니다
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    
    // 페이지 로드 또는 새로고침 감지를 위한 이벤트 리스너
    const handleBeforeUnload = () => {
      console.log(`[${video.story_id}] 페이지 새로고침 감지 - SSE 연결 종료`);
      disconnect(true);
    };
    
    window.addEventListener('beforeunload', handleBeforeUnload);
    
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('beforeunload', handleBeforeUnload);
      
      if (reconnectTimeoutRef.current) {
        window.clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
    };
  }, [connect, disconnect, isConnected, status, video.story_id]);

  // 초기 마운트 및 언마운트 처리
  useEffect(() => {
    // 컴포넌트가 마운트될 때 한 번만 실행
    if (!connectionAttempted.current) {
      console.log(`[${video.story_id}] 최초 SSE 연결 시작`);
      
      // 약간의 지연 후 연결 시도 (여러 카드가 있을 경우 동시에 연결 시도하는 것 방지)
      const timeout = window.setTimeout(() => {
        connect();
        connectionAttempted.current = true;
      }, 500);
      
      return () => {
        window.clearTimeout(timeout);
      };
    }
    
    // 컴포넌트가 언마운트될 때 SSE 연결 해제
    return () => {
      console.log(`[${video.story_id}] 컴포넌트 언마운트 - SSE 연결 해제`);
      // 즉시 해제 (서버에 알림)
      disconnect(true);
    };
  }, [connect, disconnect, video.story_id]);

  // 주기적 연결 상태 확인 및 필요시 재연결
  useEffect(() => {
    // 완료 또는 실패 상태면 재연결 시도 안함
    if (status === "COMPLETED" || status === "FAILED") {
      return;
    }
    
    const interval = setInterval(() => {
      if (!isConnected) {
        console.log(`[${video.story_id}] 주기적 확인: 연결이 없습니다. 새로 연결합니다.`);
        connect();
      }
    }, 10000); // 10초마다 확인 (부하 감소)

    return () => clearInterval(interval);
  }, [connect, isConnected, status, video.story_id]);

  // SSE 상태 업데이트
  useEffect(() => {
    console.log(`[${video.story_id}] videoStatus 변경됨:`, videoStatus);
    
    if (videoStatus) {
      setStatus(videoStatus.status);
      
      if (videoStatus.status === "PENDING") {
        setCurrentStatus("대기 중");
        setProcessingStep(undefined);
      } else if (videoStatus.status === "PROCESSING" && videoStatus.processingStep) {
        console.log(`[${video.story_id}] 처리 단계 업데이트:`, videoStatus.processingStep);
        setProcessingStep(videoStatus.processingStep);
        
        // 처리 단계에 따른 상태 텍스트 설정
        const stepText = processingStepText[videoStatus.processingStep];
        if (stepText) {
          setCurrentStatus(stepText);
        } else {
          setCurrentStatus("처리 중");
        }
      } else if (videoStatus.status === "COMPLETED" || videoStatus.status === "FAILED") {
        // 상태가 COMPLETED나 FAILED로 변경되면 부모 컴포넌트에 알림
        if (onStatusChange) {
          onStatusChange(video.story_id, videoStatus.status as "FAILED" | "COMPLETED" | "PROCESSING" | "PENDING");
        }
        setCurrentStatus(videoStatus.status === "COMPLETED" ? "완료됨" : "실패");
        setProcessingStep(undefined);
        
        // 완료 또는 실패 시 SSE 연결 종료 (이 상태에서는 더 이상 업데이트가 필요 없음)
        console.log(`[${video.story_id}] 비디오 상태가 ${videoStatus.status}로 변경되어 SSE 연결을 종료합니다.`);
        disconnect(true);
      }
    }
  }, [videoStatus, onStatusChange, video.story_id, disconnect]);

  // 상태에 따른 아이콘 및 스타일 결정
  const getStatusIcon = () => {
    if (status === "COMPLETED") {
      return <CheckCircle className="h-3 w-3 text-green-500" />;
    } else if (status === "FAILED") {
      return <AlertCircle className="h-3 w-3 text-red-500" />;
    } else if (status === "PENDING") {
      return <Clock className="h-3 w-3 text-gray-500" />;
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
    <Card className="overflow-hidden border border-gray-200 shadow-sm hover:shadow-md transition-shadow h-full">
      <div className="relative aspect-video w-full">
        <div className="flex h-full w-full items-center justify-center bg-gray-100">
          <Clock className="h-12 w-12 text-gray-400" />
        </div>
        <div className="absolute inset-x-0 bottom-0 h-1 bg-gray-200">
          <div className="h-full bg-blue-600 animate-pulse"></div>
        </div>
      </div>
      <div className="p-4 flex flex-col h-[calc(100%-16rem)]">
        <h3 className="font-medium text-gray-800 line-clamp-1 mb-3">{video.title}</h3>
        <div className="mt-auto">
          <Badge variant="outline" className={`flex items-center gap-1.5 py-1 border ${getBadgeStyle()}`}>
            {getStatusIcon()}
            <span className="whitespace-nowrap">{currentStatus}</span>
          </Badge>
          {error && (
            <p className="mt-2 text-xs text-red-500">{error}</p>
          )}
        </div>
      </div>
    </Card>
  )
} 