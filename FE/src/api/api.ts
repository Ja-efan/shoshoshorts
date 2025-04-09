import axios from "axios";
import { VideoData } from "@/types/video";
import { SocialProvider } from "@/types/auth";
import { store } from "@/store/store";
import { setToken, clearToken } from "@/store/authSlice";
import { IUserData } from "@/types/user";
import { ISpeakerInfo, ISpeakerInfoGet } from "@/types/speakerInfo";

const API_BASE_URL = import.meta.env.VITE_API_BASE || "/api";

// axios 기본 설정 추가
axios.defaults.withCredentials = true; // 쿠키 자동 전송을 위한 설정

// 인터셉터가 없는 axios 인스턴스 생성 (토큰 갱신용)
const axiosWithoutInterceptor = axios.create({
  withCredentials: true,
});

// refreshToken 요청 횟수를 추적하는 변수
let refreshTokenAttempts = 0;
const MAX_REFRESH_ATTEMPTS = 3;

// refreshToken 시도 횟수 초기화 함수
export const resetRefreshTokenAttempts = () => {
  refreshTokenAttempts = 0;
};

// 토큰 정리 및 로그아웃 상태로 변경
const clearTokenAndState = () => {
  localStorage.removeItem("accessToken");
  store.dispatch(clearToken());
};

export const API_ENDPOINTS = {
  CREATE_VIDEO: `${API_BASE_URL}/videos/generate`,
  GET_VIDEOS: `${API_BASE_URL}/videos/status/allstory`,
  GET_VIDEO_STATUS: `${API_BASE_URL}/videos/status`,
  DELETE_VIDEO: `${API_BASE_URL}/videos`,
  RETRY_VIDEO: `${API_BASE_URL}/videos/retry`,
  YOUTUBE_UPLOAD: `${API_BASE_URL}/youtube/upload`,
  YOUTUBE_AUTH: `${API_BASE_URL}/youtube/auth`,
  DOWNLOAD_VIDEO: `${API_BASE_URL}/videos/download`,
  YOUTUBE_SSE_STATUS: `${API_BASE_URL}/video/status/sse`,
  AUTH: {
    OAUTH: `${API_BASE_URL}/auth/oauth`,
    REFRESH: `${API_BASE_URL}/auth/refresh`,
    LOGOUT: `${API_BASE_URL}/auth/logout`,
    VALIDATE: `${API_BASE_URL}/auth/check`,
  },
  USER_DATA: `${API_BASE_URL}/auth/userdata`,
  GET_SPEAKER_LIBRARY: `${API_BASE_URL}/speaker/library`,
  UPLOAD_SPEAKER: `${API_BASE_URL}/speaker/upload`,
};

export const apiConfig = {
  headers: {
    "Content-Type": "application/json",
  },
};

// 인증 헤더를 포함한 설정 가져오기
const getAuthConfig = (token?: string | null) => {
  const authToken = token || localStorage.getItem("accessToken");
  return {
    ...apiConfig,
    headers: {
      ...apiConfig.headers,
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
    },
  };
};

export interface TokenResponse {
  accessToken: string;
  refreshToken?: string; // 서버에서 리프레시 토큰을 제공하는 경우 사용
}

export interface LoginResponse extends TokenResponse {}

export interface ApiResponse<T> {
  data: T;
  message: string;
  status: number;
}

// API 요청 함수들
export const apiService = {
  // 인증 관련 API
  async handleSocialLoginCallback(provider: SocialProvider, code: string) {
    try {
      const response = await axios.post<LoginResponse>(
        API_ENDPOINTS.AUTH.OAUTH,
        { provider, code }
      );
      // 액세스 토큰 저장 및 Redux store 업데이트
      const token = response.data.accessToken;
      localStorage.setItem("accessToken", token);
      store.dispatch(setToken(token));
      return response.data;
    } catch (error) {
      console.error("소셜 로그인 콜백 처리 실패:", error);
      throw error;
    }
  },

  async refreshToken() {
    try {
      // 인터셉터가 없는 axios 인스턴스 사용
      const response = await axiosWithoutInterceptor.post<TokenResponse>(
        API_ENDPOINTS.AUTH.REFRESH
      );

      const newAccessToken = response.data.accessToken;
      localStorage.setItem("accessToken", newAccessToken);
      store.dispatch(setToken(newAccessToken));
      refreshTokenAttempts = 0; // 성공하면 카운터 초기화
      return newAccessToken;
    } catch (error) {
      console.error("토큰 갱신 실패:", error);
      refreshTokenAttempts++;

      if (refreshTokenAttempts >= MAX_REFRESH_ATTEMPTS) {
        // 3번 이상 실패하면 로그아웃 처리
        clearTokenAndState();
        throw new Error("토큰 갱신 시도 횟수 초과");
      }
      throw error;
    }
  },

  async logout() {
    try {
      const token = localStorage.getItem("accessToken");
      await axios.post(API_ENDPOINTS.AUTH.LOGOUT, {}, getAuthConfig(token));
      clearTokenAndState();
      resetRefreshTokenAttempts();
    } catch (error) {
      console.error("로그아웃 실패:", error);
      clearTokenAndState();
      resetRefreshTokenAttempts();
      throw error;
    }
  },

  // 토큰 유효성 검사
  async validateToken() {
    const token = localStorage.getItem("accessToken");
    try {
      await axios.get(API_ENDPOINTS.AUTH.VALIDATE, getAuthConfig(token));
      return true;
    } catch (error) {
      try {
        // 토큰 검증 실패 시 refreshToken 시도
        await this.refreshToken();
        return true;
      } catch (refreshError) {
        console.error("토큰 갱신 실패:", refreshError);
        return false;
      }
    }
  },

  // 비디오 관련 API
  async createVideo({ data }: { data: any }) {
    let token = localStorage.getItem("accessToken");
    let retryCount = 0;
    const MAX_RETRIES = 1; // 토큰 갱신 후 1번만 재시도

    while (retryCount <= MAX_RETRIES) {
      try {
        const response = await axios.post<ApiResponse<VideoData>>(
          API_ENDPOINTS.CREATE_VIDEO,
          data,
          getAuthConfig(token)
        );
        return response.data;
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 401 && retryCount < MAX_RETRIES) {
          // 토큰 만료 시 갱신 시도
          try {
            const newToken = await this.refreshToken();
            token = newToken; // 갱신된 토큰으로 업데이트
            retryCount++;
            continue; // 갱신된 토큰으로 재시도
          } catch (refreshError) {
            console.error("토큰 갱신 실패:", refreshError);
            throw new Error("토큰 갱신에 실패했습니다. 다시 로그인해주세요.");
          }
        }
        // 다른 오류이거나 최대 재시도 횟수를 초과한 경우
        throw error;
      }
    }
  },

  async getVideos() {
    const token = localStorage.getItem("accessToken");
    const response = await axios.get<ApiResponse<VideoData[]>>(
      API_ENDPOINTS.GET_VIDEOS,
      getAuthConfig(token)
    );
    return response.data;
  },


  async uploadVideoToYoutube(storyId: string, title: string, description: string) {
    try {
      const token = localStorage.getItem("accessToken");
      const response = await axios.post(
        `${API_ENDPOINTS.YOUTUBE_UPLOAD}`,
        { storyId, title, description },
        getAuthConfig(token)
      );
      return response.data;
    } catch (error) {
      console.error("유튜브 업로드 실패:", error);
      throw error;
    }
  },

  async downloadVideo(storyId: string) {
    const token = localStorage.getItem("accessToken");
    window.location.href = `${API_ENDPOINTS.DOWNLOAD_VIDEO}/${storyId}?token=${token}`;
  },

  // YouTube 인증 URL 가져오기
  async getYoutubeAuthUrl(storyId?: string) {
    const token = localStorage.getItem("accessToken");
    try {
      const url = storyId
        ? `${API_ENDPOINTS.YOUTUBE_AUTH}?storyId=${storyId}`
        : API_ENDPOINTS.YOUTUBE_AUTH;

      const response = await axios.get<{ authUrl: string }>(
        url,
        getAuthConfig(token)
      );
      return response.data.authUrl;
    } catch (error) {
      console.error("YouTube 인증 URL 가져오기 실패:", error);
      throw error;
    }
  },

  // 비디오 상태 조회
  async getVideoStatus(storyId: string) {
    const token = localStorage.getItem("accessToken");
    try {
      const response = await axios.get<VideoData>(
        `${API_ENDPOINTS.GET_VIDEO_STATUS}/${storyId}`,
        getAuthConfig(token)
      );
      return response.data;
    } catch (error) {
      console.error("비디오 상태 조회 실패:", error);
      throw error;
    }
  },
  async getVideoStatusSSE(storyId: string) {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      throw new Error("인증 토큰이 없습니다.");
    }
    
    const response = await fetch(`${API_ENDPOINTS.YOUTUBE_SSE_STATUS}/${storyId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'text/event-stream',
      },
      credentials: 'include'
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return response;
  },
  // 유저 데이터 관련 API
  async getUserData() {
    const token = localStorage.getItem("accessToken");
    const response = await axios.get<ApiResponse<IUserData>>(
      API_ENDPOINTS.USER_DATA,
      getAuthConfig(token)
    );
    return response.data;
  },

  async getSpeakerLibrary() {
    const token = localStorage.getItem("accessToken");
    const response = await axios.get<ApiResponse<ISpeakerInfoGet[]>>(
      API_ENDPOINTS.GET_SPEAKER_LIBRARY,
      getAuthConfig(token)
    );
    return response.data;
  },

  async uploadSpeaker(speakerInfo: ISpeakerInfo) {
    const token = localStorage.getItem("accessToken");
    const response = await axios.post<ApiResponse<ISpeakerInfo>>(
      API_ENDPOINTS.UPLOAD_SPEAKER,
      speakerInfo,
      getAuthConfig(token)
    );
    return response.data;
  },

  async deleteVideo(storyId: string) {
    const token = localStorage.getItem("accessToken");
    try {
      const response = await axios.delete(
        `${API_ENDPOINTS.DELETE_VIDEO}/${storyId}`,
        getAuthConfig(token)
      );
      return response.data;
    } catch (error) {
      console.error("비디오 삭제 실패:", error);
      throw error;
    }
  },

  async retryVideo(storyId: string) {
    const token = localStorage.getItem("accessToken");
    try {
      const response = await axios.post(
        API_ENDPOINTS.RETRY_VIDEO,
        { storyId },
        getAuthConfig(token)
      );
      return response.data;
    } catch (error) {
      console.error("비디오 재시도 실패:", error);
      throw error;
    }
  },
};

// 인터셉터 설정
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    // 이미 재시도한 요청이거나 특정 엔드포인트는 제외
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      originalRequest.url !== API_ENDPOINTS.AUTH.REFRESH
    ) {
      originalRequest._retry = true;
      try {
        const newToken = await apiService.refreshToken();
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return axios(originalRequest);
      } catch (refreshError) {
        clearTokenAndState();
        window.location.href = "/login";
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);
