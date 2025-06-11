import { useEffect, useCallback } from "react"
import { useNavigate, useSearchParams } from "react-router-dom"
import { authService } from "@/lib/auth"

export default function AuthCallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  const handleCallback = useCallback(async () => {
    try {
      const provider = window.location.pathname.split("/")[2] as "google" | "naver" | "kakao"
      const code = searchParams.get("code")
      const state = searchParams.get("state")

      console.log("소셜 로그인 콜백 - Provider:", provider)
      console.log("소셜 로그인 콜백 - Code:", code)
      console.log("소셜 로그인 콜백 - State:", state)

      if (!code) {
        throw new Error("인증 코드가 없습니다.")
      }

      await authService.handleSocialLoginCallback(provider, code)

      // 이전 페이지 정보가 있으면 해당 페이지로, 없으면 대시보드로 이동
      if (state) {
        try {
          const decodedState = decodeURIComponent(state);
          const { from } = JSON.parse(decodedState);
          navigate(from);
        } catch (error) {
          console.error("state 파싱 실패:", error);
          navigate("/dashboard");
        }
      } else {
        navigate("/dashboard");
      }
    } catch (error) {
      console.error("소셜 로그인 콜백 처리 실패:", error)
      navigate("/login")
    }
  }, [searchParams, navigate])

  useEffect(() => {
    handleCallback()
  }, [handleCallback])

  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="text-center">
        <h2 className="text-xl font-semibold">로그인 처리 중...</h2>
        <p className="mt-2 text-gray-600">잠시만 기다려주세요.</p>
      </div>
    </div>
  )
} 