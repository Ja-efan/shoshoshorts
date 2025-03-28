import { Link, useLocation } from "react-router-dom"
import shortLogo from "@/assets/short_logo.png"
import kakaoIcon from "@/assets/login/kakao_logo.png"
import naverIcon from "@/assets/login/naver_logo.png"
import googleIcon from "@/assets/login/google_logo.png"
import { socialAuth } from "@/lib/socialAuth"
import { SocialLoginButton } from "@/components/common/SocialLoginButton"

export default function LoginPage() {
  const location = useLocation();
  const from = (location.state as any)?.from?.pathname || "/dashboard";

  const handleSocialLogin = (provider: "google" | "naver" | "kakao") => {
    try {
      const state = encodeURIComponent(JSON.stringify({ from }));
      const url = socialAuth.getSocialLoginUrl(provider, state);
      window.location.href = url;
    } catch (error) {
      console.error(`${provider} 로그인 URL 생성 실패:`, error);
    }
  };

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-10 bg-white border-b">
        <div className="container flex h-16 items-center px-4">
          <Link to="/" className="flex items-center gap-2">
            <img src={shortLogo} alt="쇼쇼숓 로고" className="h-8 w-8" />
            <span className="text-xl font-bold">쇼쇼숓</span>
          </Link>
        </div>
      </header>

      <main className="flex flex-1 items-center justify-center py-12">
        <div className="mx-auto w-full max-w-md space-y-6 px-4">
          <div className="space-y-2 text-center">
            <h1 className="text-3xl font-bold">Sign in</h1>
            <p className="text-gray-500">Choose a login method to continue</p>
          </div>

          <div className="grid gap-4">
            <SocialLoginButton
              icon={kakaoIcon}
              text="카카오톡으로 간편하게 로그인"
              bgColor="bg-[#FEE500] text-black hover:bg-[#E6CF00]"
              onClick={() => handleSocialLogin("kakao")}
            />
            <SocialLoginButton
              icon={naverIcon}
              text="네이버로 간편하게 로그인"
              bgColor="bg-[#03C75A] text-white hover:bg-[#02B350]"
              onClick={() => handleSocialLogin("naver")}
            />
            <SocialLoginButton
              icon={googleIcon}
              text="GOOGLE로 간편하게 로그인"
              bgColor="bg-white text-black border border-gray-300 hover:bg-gray-50"
              onClick={() => handleSocialLogin("google")}
            />
          </div>

          <div className="text-center text-sm">
            By continuing, you agree to our
            <Link to="/terms" className="ml-1 font-medium text-red-600 hover:underline">
              Terms of Service
            </Link>
          </div>
        </div>
      </main>
    </div>
  )
}

