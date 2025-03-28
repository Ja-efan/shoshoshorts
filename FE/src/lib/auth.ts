import { store } from "@/store/store";
import { apiService } from "@/api/api";
import { SocialProvider } from "@/types/auth";

export const authService = {
  // 소셜 로그인 콜백 처리
  async handleSocialLoginCallback(provider: SocialProvider, code: string) {
    try {
      return await apiService.handleSocialLoginCallback(provider, code);
    } catch (error) {
      console.error("소셜 로그인 콜백 처리 실패:", error);
      throw error;
    }
  },

  // 토큰 갱신 함수
  async refreshToken(): Promise<string> {
    try {
      return await apiService.refreshToken();
    } catch (error) {
      console.error("토큰 갱신 에러:", error);
      throw error;
    }
  },

  // 로그아웃 함수
  async logout() {
    try {
      await apiService.logout();
    } catch (error) {
      console.error("로그아웃 에러:", error);
      throw error;
    }
  },

  // 현재 로그인 상태 확인
  isAuthenticated(): boolean {
    return store.getState().auth.isAuthenticated;
  },

  // 현재 토큰 가져오기
  getToken(): string | null {
    return store.getState().auth.token;
  },
}; 