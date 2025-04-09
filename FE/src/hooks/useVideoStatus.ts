import { useState, useEffect } from 'react';
import { apiService } from '@/api/api';
import { useNavigate } from 'react-router-dom';
import { VideoStatus, VideoStatusResponse } from '@/types/video';

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
  const [abortController, setAbortController] = useState<AbortController | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const controller = new AbortController();
    setAbortController(controller);

    const connectSSE = async () => {
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
                // 연결 메시지는 JSON 파싱하지 않음
                if (eventData.event === "connect") {
                  console.log('SSE 연결됨:', eventData.data);
                  continue;
                }
                
                // 상태 업데이트 메시지만 JSON 파싱
                const data = JSON.parse(eventData.data) as VideoStatusResponse;
                console.log('SSE 데이터 수신:', data);
                
                // 데이터 형식 확인 및 변환
                if (data && typeof data === 'object') {
                  // 필요한 필드가 있는지 확인하고 없으면 기본값 설정
                  const statusEvent: VideoStatusEvent = {
                    storyId: data.storyId || storyId,
                    status: data.status || 'PROCESSING',
                    createdAt: data.createdAt || new Date().toISOString(),
                    processingStep: data.processing_step || ""
                  };
                  
                  console.log('변환된 상태 이벤트:', statusEvent);
                  setVideoStatus(statusEvent);
                  
                  // 상태가 COMPLETED나 FAILED가 되면 SSE 연결 종료하고 대시보드로 이동
                  if (statusEvent.status === 'COMPLETED' || statusEvent.status === 'FAILED') {
                    console.log(`비디오 상태가 ${statusEvent.status}로 변경되어 SSE 연결을 종료합니다.`);
                    
                    // 백엔드에 SSE 연결 종료 요청
                    const token = localStorage.getItem('accessToken');
                    if (token) {
                      fetch(`/api/video/status/sse/${storyId}`, {
                        method: 'DELETE',
                        headers: {
                          'Authorization': `Bearer ${token}`,
                        },
                        credentials: 'include'
                      }).catch(err => console.error('SSE 연결 종료 요청 실패:', err));
                    }
                    
                    // 리더 종료
                    reader.cancel();
                    setIsConnected(false);
                    
                    // 대시보드로 이동
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

    connectSSE();

    return () => {
      if (abortController) {
        abortController.abort();
      }
      setAbortController(null);
    };
  }, [storyId, navigate]);

  return { videoStatus, error, isConnected };
}; 