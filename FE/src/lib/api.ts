export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const API_ENDPOINTS = {
  CREATE_VIDEO: `${API_BASE_URL}/api/videos`,
  GET_VOICES: `${API_BASE_URL}/api/voices`,
  // ... other endpoints
};

// Add type definition for the API request
export interface VideoRequestData {
  title: string;
  story: string;
  characterArr: {
    name: string;
    gender: "1" | "2";
    description: string;
    voice_code: string;
  }[];
}

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

// API 요청 함수들
export const apiService = {
  async createVideo(data: any) {
    const response = await fetch(API_ENDPOINTS.CREATE_VIDEO, {
      method: 'POST',
      ...apiConfig,
      // headers: getAuthHeaders(), // Use this when auth is needed
      body: JSON.stringify(data),
    });
    return response.json();
  },
  
  // ... other API methods
};