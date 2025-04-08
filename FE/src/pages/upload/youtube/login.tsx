import { useEffect, useState } from "react"
import { useLocation } from "react-router-dom"
import googleIcon from "@/assets/login/google_logo.png"
import shortLogo from "@/assets/short_logo.png"
import { SocialLoginButton } from "@/components/common/SocialLoginButton"
import { apiService } from "@/api/api"
import { motion } from "framer-motion"

export default function YoutubeLoginPage() {
  const location = useLocation();
  const [storyId, setStoryId] = useState<string | null>(null);

  // URL에서 storyId 파라미터 추출
  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const id = searchParams.get("storyId");
    setStoryId(id);
  }, [location]);

  const handleGoogleLogin = async () => {
    try {
      if (!storyId) {
        throw new Error("스토리 ID가 없습니다.");
      }
      
      // 유튜브 인증 URL 가져오기
      const authUrl = await apiService.getYoutubeAuthUrl(storyId);
      
      // 인증 URL로 리다이렉트
      window.location.href = authUrl;
    } catch (error) {
      console.error("유튜브 인증 실패:", error);
    }
  };

  // 애니메이션 변수
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: { 
      opacity: 1,
      transition: { 
        staggerChildren: 0.1,
        delayChildren: 0.2
      }
    }
  };
  
  const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: { 
      y: 0, 
      opacity: 1,
      transition: { type: "spring", stiffness: 100 }
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-gray-100 to-gray-200 relative overflow-hidden">
      {/* 배경 디자인 요소 */}
      <div className="absolute inset-0 bg-grid-pattern opacity-5"></div>
      <div className="absolute inset-0 bg-gradient-to-br from-red-500/10 via-pink-500/10 to-blue-500/10"></div>
      <div className="absolute inset-0 bg-gradient-to-t from-pink-500/5 via-transparent to-transparent"></div>
      
      {/* 로고 */}
      <motion.div 
        className="absolute top-6 left-6 z-10"
        initial={{ scale: 0.8, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.5 }}
      >
        <img src={shortLogo} alt="ShoShoShorts" className="h-12 md:h-16" />
      </motion.div>

      {/* 장식용 원형 요소들 */}
      <div className="absolute top-20 right-[20%] w-32 h-32 bg-red-400/10 rounded-full blur-xl"></div>
      <div className="absolute bottom-20 left-[15%] w-40 h-40 bg-blue-400/10 rounded-full blur-xl"></div>
      <div className="absolute top-[40%] left-[10%] w-24 h-24 bg-yellow-400/10 rounded-full blur-xl"></div>
      
      <main className="container mx-auto px-4 py-12 relative">
        <motion.div 
          className="max-w-2xl mx-auto mt-8 md:mt-16"
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <div className="bg-white/80 backdrop-blur-lg rounded-2xl shadow-2xl overflow-hidden border border-white/20">
            <div className="p-5 md:p-8 lg:p-12">
              <motion.div 
                className="text-center space-y-6 md:space-y-8"
                variants={containerVariants}
                initial="hidden"
                animate="visible"
              >
                <motion.div className="space-y-3 md:space-y-4" variants={itemVariants}>
                  <h1 className="text-3xl md:text-4xl font-bold bg-gradient-to-r from-red-600 via-pink-500 to-red-600 bg-clip-text text-transparent animate-gradient">
                    유튜브 숏츠 업로드
                  </h1>
                  <p className="text-base md:text-lg text-gray-600">
                    ShoShoShorts와 함께 당신의 이야기를 숏츠로 공유해보세요
                  </p>
                </motion.div>

                <motion.div 
                  className="flex justify-center space-x-8 md:space-x-12 py-3 md:py-6"
                  variants={itemVariants}
                >
                  <motion.div 
                    className="text-center group"
                    whileHover={{ scale: 1.05 }}
                    transition={{ type: "spring", stiffness: 300 }}
                  >
                    <div className="text-2xl md:text-3xl mb-2 md:mb-3 group-hover:animate-bounce">🎬</div>
                    <p className="text-xs md:text-sm font-medium text-gray-600">AI 자동 생성</p>
                  </motion.div>
                  <motion.div 
                    className="text-center group"
                    whileHover={{ scale: 1.05 }}
                    transition={{ type: "spring", stiffness: 300 }}
                  >
                    <div className="text-2xl md:text-3xl mb-2 md:mb-3 group-hover:animate-bounce">⚡️</div>
                    <p className="text-xs md:text-sm font-medium text-gray-600">빠른 업로드</p>
                  </motion.div>
                  <motion.div 
                    className="text-center group"
                    whileHover={{ scale: 1.05 }}
                    transition={{ type: "spring", stiffness: 300 }}
                  >
                    <div className="text-2xl md:text-3xl mb-2 md:mb-3 group-hover:animate-bounce">✨</div>
                    <p className="text-xs md:text-sm font-medium text-gray-600">멋진 효과</p>
                  </motion.div>
                </motion.div>

                <motion.div className="relative py-5 md:py-8" variants={itemVariants}>
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-gray-200"></div>
                  </div>
                  <div className="relative flex justify-center text-xs md:text-sm">
                    <span className="px-4 md:px-6 py-1 bg-white/90 text-gray-500 rounded-full border border-gray-100 shadow-sm">
                      Google 계정으로 시작하기
                    </span>
                  </div>
                </motion.div>

                <motion.div 
                  className="flex justify-center items-center w-full"
                  variants={itemVariants}
                >
                  <motion.div 
                    className="w-full max-w-sm"
                    style={{justifyItems:"center"}}
                  >
                    <SocialLoginButton
                      icon={googleIcon}
                      text="구글 계정으로 유튜브 로그인"
                      bgColor="bg-white text-gray-700 border border-gray-300 hover:bg-gray-50 hover:border-red-200 shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105"
                      onClick={handleGoogleLogin}
                    />
                  </motion.div>
                </motion.div>

                <motion.div 
                  className="space-y-4 md:space-y-6 text-xs md:text-sm text-gray-500"
                  variants={itemVariants}
                >
                  <div className="px-4 md:px-6 py-3 md:py-4 bg-gradient-to-br from-gray-50/80 to-gray-50/20 rounded-xl leading-relaxed border border-gray-100/80 shadow-sm">
                    <p className="mb-2 md:mb-3 font-medium text-gray-700">✓ 로그인 시 제공되는 기능</p>
                    <ul className="space-y-1.5 md:space-y-2 text-gray-600">
                      <li className="flex items-center">
                        <span className="w-4 md:w-5 h-4 md:h-5 mr-2 flex items-center justify-center bg-red-100 text-red-600 rounded-full">•</span>
                        AI가 자동으로 생성하는 숏츠 영상
                      </li>
                      <li className="flex items-center">
                        <span className="w-4 md:w-5 h-4 md:h-5 mr-2 flex items-center justify-center bg-red-100 text-red-600 rounded-full">•</span>
                        유튜브 채널에 자동 업로드
                      </li>
                      <li className="flex items-center">
                        <span className="w-4 md:w-5 h-4 md:h-5 mr-2 flex items-center justify-center bg-red-100 text-red-600 rounded-full">•</span>
                        맞춤형 썸네일 및 제목 설정
                      </li>
                    </ul>
                  </div>
                  <div className="flex flex-col items-center space-y-2 md:space-y-3 text-xs md:text-sm">
                    <p className="text-gray-600 text-center px-2">이 과정은 귀하의 유튜브 채널에 동영상을 업로드하기 위한 권한을 부여합니다.</p>
                    <p className="flex flex-wrap items-center justify-center gap-1 md:gap-2">
                      <span>개인정보 보호정책:</span>
                      <a 
                        href="https://policies.google.com/privacy" 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="text-red-600 hover:text-red-700 hover:underline transition-colors"
                      >
                        Google 개인정보처리방침
                      </a>
                    </p>
                  </div>
                </motion.div>
              </motion.div>
            </div>
          </div>
        </motion.div>
      </main>
    </div>
  )
} 