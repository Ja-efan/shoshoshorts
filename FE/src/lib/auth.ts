import { store } from "@/store/store";
import { apiService, resetRefreshTokenAttempts } from "@/api/api";
import { SocialProvider } from "@/types/auth";
import { clearToken } from "@/store/authSlice";

// 로그인 페이지로 리디렉션하는 함수
const redirectToLogin = () => {
  if (typeof window !== 'undefined') {
    window.location.href = "/login";
  }
};

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

  // 로그아웃 함수
  async logout() {
    try {
      await apiService.logout(); // API 서비스의 로그아웃 호출 (토큰 제거, 서버 로그아웃 처리)
      redirectToLogin();
    } catch (error) {
      console.error("로그아웃 에러:", error);
      // 로그아웃 실패시에도 클라이언트에서 강제 로그아웃 처리
      localStorage.removeItem("accessToken");
      store.dispatch(clearToken());
      resetRefreshTokenAttempts();
      redirectToLogin();
    }
  },

  // 토큰 유효성 검사
  async validateToken(): Promise<boolean> {
    try {
      const token = this.getToken();
      if (!token) return false;
      return await apiService.validateToken();
    } catch (error) {
      console.error("토큰 유효성 검사 실패:", error);
      return false;
    }
  },

  // 현재 로그인 상태 확인 (비동기)
  async isAuthenticated(): Promise<boolean> {
    const token = this.getToken();
    if (!token) return false;
    return await this.validateToken();
  },

  // 현재 토큰 가져오기
  getToken(): string | null {
    return store.getState().auth.token;
  },
}; 