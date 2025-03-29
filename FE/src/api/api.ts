import axios from "axios"
import { VideoData } from "@/types/video"
import { SocialProvider } from "@/types/auth"
import { store } from "@/store/store"
import { setToken, clearToken } from "@/store/authSlice"

const API_BASE_URL = import.meta.env.REACT_APP_API_URL || "http://localhost:8080";

// axios 기본 설정 추가
axios.defaults.withCredentials = true;  // 쿠키 자동 전송을 위한 설정

export const API_ENDPOINTS = {
  CREATE_VIDEO: `${API_BASE_URL}/api/videos/generate`,
  GET_VIDEOS: `${API_BASE_URL}/api/videos/status/allstory`,
  AUTH: {
    OAUTH: `${API_BASE_URL}/api/auth/oauth`,
    REFRESH: `${API_BASE_URL}/api/auth/refresh`,
    LOGOUT: `${API_BASE_URL}/api/auth/logout`,
  },
};

export const apiConfig = {
  headers: {
    'Content-Type': 'application/json',
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
      const response = await axios.post<TokenResponse>(
        API_ENDPOINTS.AUTH.REFRESH
      );
      
      const newAccessToken = response.data.accessToken;
      localStorage.setItem("accessToken", newAccessToken);
      store.dispatch(setToken(newAccessToken));
      return newAccessToken;
    } catch (error) {
      console.error("토큰 갱신 실패:", error);
      localStorage.removeItem("accessToken");
      store.dispatch(clearToken());
      window.location.href = "/login";
      throw error;
    }
  },

  async logout() {
    try {
      const token = localStorage.getItem("accessToken");
      await axios.post(API_ENDPOINTS.AUTH.LOGOUT, {}, getAuthConfig(token));
      localStorage.removeItem("accessToken");
      store.dispatch(clearToken());
    } catch (error) {
      console.error("로그아웃 실패:", error);
      throw error;
    }
  },

  // 비디오 관련 API
  async createVideo(data: any) {
    const token = localStorage.getItem("accessToken");
    const response = await axios.post<ApiResponse<VideoData>>(
      API_ENDPOINTS.CREATE_VIDEO,
      data,
      getAuthConfig(token)
    );
    return response.data;
  },

  async getVideos() {
    const token = localStorage.getItem("accessToken");
    const response = await axios.get<ApiResponse<VideoData[]>>(
      API_ENDPOINTS.GET_VIDEOS,
      getAuthConfig(token)
    );
    return response.data;
  },
};

// 소셜 로그인 관련 설정
type SocialAuthConfig = {
  google: {
    clientId: string;
    redirectUri: string;
    scope: string;
    authUrl: string;
  };
  naver: {
    clientId: string;
    redirectUri: string;
    authUrl: string;
  };
  kakao: {
    clientId: string;
    redirectUri: string;
    authUrl: string;
  };
};

const SOCIAL_AUTH_CONFIG: SocialAuthConfig = {
  google: {
    clientId: import.meta.env.REACT_APP_GOOGLE_CLIENT_ID,
    redirectUri: `${window.location.origin}/auth/google/callback`,
    scope: "email profile",
    authUrl: "https://accounts.google.com/o/oauth2/v2/auth",
  },
  naver: {
    clientId: import.meta.env.REACT_APP_NAVER_CLIENT_ID,
    redirectUri: `${window.location.origin}/auth/naver/callback`,
    authUrl: "https://nid.naver.com/oauth2.0/authorize",
  },
  kakao: {
    clientId: import.meta.env.REACT_APP_KAKAO_CLIENT_ID,
    redirectUri: `${window.location.origin}/auth/kakao/callback`,
    authUrl: "https://kauth.kakao.com/oauth/authorize",
  },
};

export const socialAuth = {
  getSocialLoginUrl(provider: SocialProvider, state?: string) {
    const config = SOCIAL_AUTH_CONFIG[provider];
    const params = new URLSearchParams({
      client_id: config.clientId,
      redirect_uri: config.redirectUri,
      response_type: "code",
      ...(state && { state }),
    });

    if (provider === "google" && "scope" in config) {
      params.append("scope", config.scope);
      params.append("access_type", "offline");
      params.append("prompt", "consent");
    }

    return `${config.authUrl}?${params.toString()}`;
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
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const newToken = await apiService.refreshToken();
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return axios(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem("accessToken");
        store.dispatch(clearToken());
        window.location.href = "/login";
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
); 