import { useState, useEffect, useRef } from 'react';
import { apiService } from '@/api/api';
import { useNavigate } from 'react-router-dom';
import { VideoStatus, VideoStatusResponse } from '@/types/video';

// SSE 연결을 관리하는 전역 상태
const activeConnections = new Map<string, {
  controller: AbortController;
  count: number;
  disconnectTimeout?: NodeJS.Timeout;
}>();

// 연결 해제 요청을 일괄 처리하기 위한 큐
const disconnectQueue = new Set<string>();
let disconnectTimeout: NodeJS.Timeout | null = null;

// 일괄 연결 해제 함수
const batchDisconnect = () => {
  if (disconnectQueue.size === 0) return;

  const token = localStorage.getItem('accessToken');
  if (!token) return;

  // 모든 연결 해제 요청을 하나의 요청으로 처리
  fetch('/api/video/status/sse/batch-disconnect', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ storyIds: Array.from(disconnectQueue) }),
    credentials: 'include'
  }).catch(err => console.error('일괄 연결 해제 요청 실패:', err));

  disconnectQueue.clear();
  disconnectTimeout = null;
};

interface VideoStatusEvent {
  storyId: string;
  status: VideoStatus;
  createdAt: string;
  processingStep: string | null;
}

export const useVideoStatus = (storyId: string) => {
  const [videoStatus, setVideoStatus] = useState<VideoStatusEvent | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const navigate = useNavigate();
  const isUnmounting = useRef(false);

  // SSE 연결 함수
  const connect = async () => {
    const existingConnection = activeConnections.get(storyId);
    
    if (existingConnection) {
      // 이미 연결이 있는 경우 카운트만 증가
      existingConnection.count += 1;
      setIsConnected(true);
      return;
    }

    const controller = new AbortController();
    activeConnections.set(storyId, {
      controller,
      count: 1
    });

    try {
      const response = await apiService.getVideoStatusSSE(storyId);
      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('SSE 리더를 초기화할 수 없습니다.');
      }

      setIsConnected(true);
      const decoder = new TextDecoder();
      let buffer = '';
      
      const processChunk = (chunk: string) => {
        if (isUnmounting.current) return;
        
        buffer += chunk;
        const lines = buffer.split('\n\n');
        buffer = lines.pop() || '';
        
        for (const line of lines) {
          if (line.trim() === '') continue;
          
          const lines = line.split('\n');
          const eventData: Record<string, string> = {};
          
          for (const l of lines) {
            if (l.startsWith('event:')) {
              eventData.event = l.substring(6).trim();
            } else if (l.startsWith('data:')) {
              eventData.data = l.substring(5).trim();
            }
          }
          
          if (eventData.data) {
            try {
              if (eventData.event === "connect") {
                console.log('SSE 연결됨:', eventData.data);
                continue;
              }
              
              const data = JSON.parse(eventData.data) as VideoStatusResponse;
              console.log('SSE 데이터 수신:', data);
              
              if (data && typeof data === 'object') {
                const statusEvent: VideoStatusEvent = {
                  storyId: data.storyId || storyId,
                  status: data.status || 'PROCESSING',
                  createdAt: data.createdAt || new Date().toISOString(),
                  processingStep: data.processing_step || ""
                };
                
                console.log('변환된 상태 이벤트:', statusEvent);
                setVideoStatus(statusEvent);
                
                if (statusEvent.status === 'COMPLETED' || statusEvent.status === 'FAILED') {
                  console.log(`비디오 상태가 ${statusEvent.status}로 변경되어 SSE 연결을 종료합니다.`);
                  disconnect(true); // 즉시 해제
                  navigate('/dashboard');
                }
              } else {
                console.error('잘못된 데이터 형식:', data);
              }
            } catch (err) {
              console.error('SSE 데이터 파싱 오류:', err);
            }
          }
        }
      };

      while (true) {
        if (isUnmounting.current) break;
        
        const { done, value } = await reader.read();
        if (done) break;
        
        const chunk = decoder.decode(value, { stream: true });
        processChunk(chunk);
      }
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        console.log('SSE 연결이 취소되었습니다.');
      } else {
        console.error('SSE 연결 오류:', error);
        setError(error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.');
      }
      setIsConnected(false);
    }
  };

  // SSE 연결 해제 함수
  const disconnect = async (immediate = false) => {
    const existingConnection = activeConnections.get(storyId);
    if (existingConnection) {
      existingConnection.count -= 1;
      
      // 마지막 컴포넌트가 연결을 해제하는 경우에만 실제로 연결을 종료
      if (existingConnection.count <= 0) {
        existingConnection.controller.abort();
        activeConnections.delete(storyId);
        
        // 즉시 해제가 필요한 경우 (COMPLETED/FAILED 상태)
        if (immediate) {
          const token = localStorage.getItem('accessToken');
          if (token) {
            try {
              await fetch(`/api/video/status/sse/${storyId}`, {
                method: 'DELETE',
                headers: {
                  'Authorization': `Bearer ${token}`,
                },
                credentials: 'include'
              });
            } catch (err) {
              console.error('SSE 연결 종료 요청 실패:', err);
            }
          }
        } else {
          // 일괄 처리 큐에 추가
          disconnectQueue.add(storyId);
          
          // 1초 후에 일괄 처리 실행
          if (!disconnectTimeout) {
            disconnectTimeout = setTimeout(batchDisconnect, 1000);
          }
        }
      }
    }
    
    setIsConnected(false);
  };

  useEffect(() => {
    isUnmounting.current = false;
    
    // 페이지 언로드 시 SSE 연결 해제
    const handleBeforeUnload = () => {
      isUnmounting.current = true;
      disconnect();
    };

    // 이벤트 리스너 등록
    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      isUnmounting.current = true;
      window.removeEventListener('beforeunload', handleBeforeUnload);
      disconnect();
    };
  }, [storyId, navigate]);

  return { videoStatus, error, isConnected, connect, disconnect };
}; 