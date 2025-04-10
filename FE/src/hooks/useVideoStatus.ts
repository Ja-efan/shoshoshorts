import { useState, useCallback, useEffect, useRef } from "react";
import { apiService } from "@/api/api";

// 처리 단계에 따른 한글 텍스트 매핑
const processingStepText: Record<string, string> = {
  SCRIPT_PROCESSING: "스크립트 처리 중",
  VOICE_GENERATING: "AI 음성 생성 중",
  IMAGE_GENERATING: "AI 이미지 생성 중",
  VOICE_COMPLETED: "AI 음성 생성 완료",
  IMAGE_COMPLETED: "AI 이미지 생성 완료",
  VIDEO_RENDERING: "영상 병합 중",
  VIDEO_RENDER_COMPLETED: "영상 병합 완료",
  VIDEO_UPLOADING: "영상 업로드 중",
};

interface VideoStatusUpdate {
  status: "FAILED" | "COMPLETED" | "PENDING" | "PROCESSING";
  processingStep?: string;
  errorMessage?: string;
}

export function useVideoStatus(storyId: string) {
  const [videoStatus, setVideoStatus] = useState<VideoStatusUpdate | null>(
    null
  );
  const [error, setError] = useState<string | null>(null);
  const [isConnected, setIsConnected] = useState(false);

  const eventSourceRef = useRef<EventSource | null>(null);
  const connectionTimeoutRef = useRef<number | null>(null);
  const retryCountRef = useRef<number>(0);
  const MAX_RETRY_COUNT = 5;
  const RETRY_DELAY = 2000; // 2초 후 재시도

  // 처리 단계 텍스트 변환 함수
  const getProcessingStepText = useCallback(
    (step: string | undefined): string => {
      if (!step) return "처리 중";
      return processingStepText[step] || "처리 중";
    },
    []
  );

  // SSE 연결 함수
  const connect = useCallback(() => {
    // 이미 연결되어 있는 경우 중복 연결 방지
    if (eventSourceRef.current || isConnected) {
      console.log(`[${storyId}] 이미 SSE 연결이 있습니다.`);
      return;
    }

    if (retryCountRef.current >= MAX_RETRY_COUNT) {
      console.log(`[${storyId}] 최대 재시도 횟수 초과. 연결 시도 중단.`);
      setError("서버 연결 실패. 다시 시도해주세요.");
      return;
    }

    // 이전 타임아웃 제거
    if (connectionTimeoutRef.current) {
      window.clearTimeout(connectionTimeoutRef.current);
      connectionTimeoutRef.current = null;
    }

    console.log(`[${storyId}] SSE 연결 시작...`);
    setError(null);

    try {
      apiService
        .getVideoStatusSSE(storyId)
        .then((response) => {
          if (!response.ok) {
            throw new Error(`서버 응답 오류: ${response.status}`);
          }

          // EventSource 생성 및 이벤트 핸들러 설정
          const eventSource = new EventSource(`${response.url}`, {
            withCredentials: true,
          });
          eventSourceRef.current = eventSource;

          eventSource.onopen = () => {
            console.log(`[${storyId}] SSE 연결 성공`);
            setIsConnected(true);
            retryCountRef.current = 0; // 성공 시 재시도 카운트 초기화
          };

          eventSource.onmessage = (event) => {
            try {
              console.log(`[${storyId}] SSE 메시지 수신:`, event.data);
              const data = JSON.parse(event.data) as VideoStatusUpdate;

              // 상태 업데이트 전 로그
              if (data.status === "PROCESSING" && data.processingStep) {
                console.log(
                  `[${storyId}] 처리 단계 업데이트: ${
                    data.processingStep
                  } (${getProcessingStepText(data.processingStep)})`
                );
              }

              setVideoStatus(data);

              // 완료나 실패 상태인 경우 연결 종료
              if (data.status === "COMPLETED" || data.status === "FAILED") {
                console.log(
                  `[${storyId}] 최종 상태 (${data.status}) 도달. SSE 연결 종료`
                );
                disconnect(true); // 서버에 알림
              }
            } catch (e) {
              console.error(`[${storyId}] 메시지 파싱 오류:`, e);
              setError("데이터 파싱 오류");
            }
          };

          eventSource.onerror = (event) => {
            console.error(`[${storyId}] SSE 연결 오류:`, event);
            setIsConnected(false);

            // 연결 오류 시 재연결 시도
            if (eventSourceRef.current) {
              eventSourceRef.current.close();
              eventSourceRef.current = null;

              retryCountRef.current++;
              console.log(
                `[${storyId}] 연결 재시도 ${retryCountRef.current}/${MAX_RETRY_COUNT}`
              );

              // 재연결 시도
              connectionTimeoutRef.current = window.setTimeout(() => {
                if (document.visibilityState === "visible") {
                  connect();
                }
              }, RETRY_DELAY);
            }
          };
        })
        .catch((error) => {
          console.error(`[${storyId}] SSE 연결 실패:`, error);
          setError(`연결 실패: ${error.message}`);
          setIsConnected(false);

          // 오류 발생 시 재연결 시도
          retryCountRef.current++;
          connectionTimeoutRef.current = window.setTimeout(() => {
            if (document.visibilityState === "visible") {
              connect();
            }
          }, RETRY_DELAY);
        });
    } catch (e) {
      console.error(`[${storyId}] SSE 연결 중 예외 발생:`, e);
      setError(
        `연결 오류: ${e instanceof Error ? e.message : "알 수 없는 오류"}`
      );
      setIsConnected(false);
    }
  }, [storyId, isConnected, getProcessingStepText]);

  // SSE 연결 종료 함수
  const disconnect = useCallback(
    (notifyServer: boolean = false) => {
      console.log(
        `[${storyId}] SSE 연결 종료 요청. 서버 알림: ${notifyServer}`
      );

      // 모든 타임아웃 제거
      if (connectionTimeoutRef.current) {
        window.clearTimeout(connectionTimeoutRef.current);
        connectionTimeoutRef.current = null;
      }

      // EventSource 종료
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
        setIsConnected(false);
        console.log(`[${storyId}] EventSource 종료됨`);
      }

      // 서버에 연결 종료 알림 (필요한 경우)
      if (notifyServer) {
        apiService
          .closeVideoStatusSSE(storyId)
          .then(() => console.log(`[${storyId}] 서버에 연결 종료 알림 성공`))
          .catch((err) =>
            console.error(`[${storyId}] 서버에 연결 종료 알림 실패:`, err)
          );
      }
    },
    [storyId]
  );

  // 컴포넌트 언마운트 시 연결 종료
  useEffect(() => {
    return () => {
      console.log(`[${storyId}] 컴포넌트 언마운트 - 연결 정리`);
      disconnect(true);
    };
  }, [disconnect, storyId]);

  // 현재 상태에 따른 표시 텍스트 반환
  const getStatusText = useCallback((): string => {
    if (!videoStatus) return "대기 중";

    switch (videoStatus.status) {
      case "PENDING":
        return "대기 중";
      case "PROCESSING":
        return videoStatus.processingStep
          ? getProcessingStepText(videoStatus.processingStep)
          : "처리 중";
      case "COMPLETED":
        return "완료됨";
      case "FAILED":
        return "실패";
      default:
        return "처리 중";
    }
  }, [videoStatus, getProcessingStepText]);

  return {
    videoStatus,
    statusText: getStatusText(),
    error,
    isConnected,
    connect,
    disconnect,
  };
}
