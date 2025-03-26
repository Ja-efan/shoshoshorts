import axios from "axios"

export const API_BASE_URL = 'http://shoshoshorts.duckdns.org:8080';

export const API_ENDPOINTS = {
  CREATE_VIDEO: `${API_BASE_URL}/api/videos/generate`,
  GET_VIDEOS: `${API_BASE_URL}/api/videos/status/allstory`,
} as const;


// API 요청을 위한 기본 설정
export const apiConfig = {
  headers: {
    'Content-Type': 'application/json',
    // Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
    // 'X-Custom-Header': 'custom-value',
  },
};

// Helper function to get headers with authorization
/* const getAuthHeaders = () => {
  const token = localStorage.getItem('accessToken');
  return {
    ...apiConfig.headers,
    Authorization: `Bearer ${token}`,
  };
}; */

interface VideoData {
  title: string
  status: "FAILED" | "COMPLETED" | "PROCESSING"
  completed_at: string | null
  thumbnail_url: string | null
  video_url: string | null
  story_id: string
}

interface ApiResponse<T> {
  data: T
  message: string
  status: number
}

// API 요청 함수들
export const apiService = {
  async createVideo(data: any) {
    const response = await axios.post(API_ENDPOINTS.CREATE_VIDEO, data, apiConfig)
    return response.data
  },

  async getVideos() {
    const response = await axios.get<ApiResponse<VideoData[]>>(API_ENDPOINTS.GET_VIDEOS, apiConfig)
    return response.data
  },
  
  // ... other API methods
};