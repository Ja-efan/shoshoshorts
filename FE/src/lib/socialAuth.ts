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
    clientId: "446448118294-54suhjh7ltfj18ska0j3aufl1hmn244k.apps.googleusercontent.com",
    redirectUri: `${window.location.origin}/auth/google/callback`,
    scope: "email profile",
    authUrl: "https://accounts.google.com/o/oauth2/v2/auth",
  },
  naver: {
    clientId: "h1MhZbPaPsiiZhVhII4d",
    redirectUri: `${window.location.origin}/auth/naver/callback`,
    authUrl: "https://nid.naver.com/oauth2.0/authorize",
  },
  kakao: {
    clientId: "YOUR_KAKAO_CLIENT_ID",
    redirectUri: `${window.location.origin}/auth/kakao/callback`,
    authUrl: "https://kauth.kakao.com/oauth/authorize",
  },
} as const;

export const socialAuth = {
  // 소셜 로그인 URL 생성
  getSocialLoginUrl(provider: "google" | "naver" | "kakao", state?: string) {
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

  // 소셜 로그인 콜백 처리
  async handleCallback(provider: "google" | "naver" | "kakao", code: string) {
    const config = SOCIAL_AUTH_CONFIG[provider];
    
    try {
      // 백엔드로 인증 코드 전송
      const response = await fetch(`${import.meta.env.VITE_API_URL}/api/auth/${provider}/callback`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          code,
          redirectUri: config.redirectUri,
        }),
      });

      if (!response.ok) {
        throw new Error("인증 처리 실패");
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error(`${provider} 로그인 콜백 처리 실패:`, error);
      throw error;
    }
  },
}; 