import { useEffect, useState } from "react"
import { useLocation, useNavigate } from "react-router-dom"
import shortLogo from "@/assets/short_logo.png"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Play, Check, Youtube } from "lucide-react"
import { formatDate } from "@/lib/utils"
import { apiService } from "@/api/api"
import { VideoData } from "@/types/video"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import toast from "react-hot-toast"
import { motion } from "framer-motion"

export default function YoutubeUploadPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [video, setVideo] = useState<VideoData | null>(null);
  const [isConfirmed, setIsConfirmed] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [isVideoModalOpen, setIsVideoModalOpen] = useState(false);
  const [isSuccessModalOpen, setIsSuccessModalOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const authSuccess = searchParams.get("authSuccess");
    const storyId = searchParams.get("storyId");

    if (!authSuccess || !storyId) {
      toast.error("잘못된 접근입니다.");
      navigate("/dashboard");
      return;
    }

    void loadVideoData(storyId);
  }, [location, navigate]);

  const loadVideoData = async (storyId: string) => {
    try {
      const data = await apiService.getVideoStatus(storyId);
      if (!data) {
        throw new Error("비디오 데이터를 찾을 수 없습니다.");
      }
      setVideo(data);
      setTitle(data.title || "ShoShoShort 영상");
      setDescription("AI로 생성된 영상입니다. #ShoShoShorts #쇼쇼쇼츠");
    } catch (error) {
      toast.error("비디오 정보를 불러오는데 실패했습니다.");
      navigate("/dashboard");
    }
  };

  const handleUpload = async () => {
    if (!video || !isConfirmed) return;

    try {
      setIsUploading(true);
      await apiService.uploadVideoToYoutube(
        video.video_url || "",
        title,
        description
      );
      setIsSuccessModalOpen(true);
    } catch (error) {
      toast.error("업로드 중 오류가 발생했습니다.");
      setIsUploading(false);
    }
  };

  const handleCloseWindow = () => {
    window.close();
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

  if (!video) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-gray-100 to-gray-200 relative overflow-hidden">
      {/* 배경 디자인 요소 */}
      <div className="absolute inset-0 bg-grid-pattern opacity-5"></div>
      <div className="absolute inset-0 bg-gradient-to-br from-red-500/10 via-pink-500/10 to-blue-500/10"></div>
      <div className="absolute inset-0 bg-gradient-to-t from-pink-500/5 via-transparent to-transparent"></div>
      
      {/* 장식용 원형 요소들 */}
      <div className="absolute top-20 right-[20%] w-32 h-32 bg-red-400/10 rounded-full blur-xl"></div>
      <div className="absolute bottom-20 left-[15%] w-40 h-40 bg-blue-400/10 rounded-full blur-xl"></div>
      <div className="absolute top-[40%] left-[10%] w-24 h-24 bg-yellow-400/10 rounded-full blur-xl"></div>
      
      {/* 로고 */}
      <motion.div 
        className="absolute top-4 md:top-6 left-4 md:left-6 z-10"
        initial={{ scale: 0.8, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.5 }}
      >
        <img src={shortLogo} alt="ShoShoShorts" className="h-10 md:h-12 lg:h-16" />
      </motion.div>

      <main className="container mx-auto px-4 py-8 md:py-12 relative">
        <motion.div 
          className="w-full max-w-2xl mx-auto mt-10 md:mt-16 space-y-6 md:space-y-8"
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <motion.div 
            className="text-center space-y-2"
            variants={containerVariants}
            initial="hidden"
            animate="visible"
          >
            <motion.h1 
              className="text-2xl md:text-3xl font-bold bg-gradient-to-r from-red-600 via-pink-500 to-red-600 bg-clip-text text-transparent"
              variants={itemVariants}
            >
              유튜브 업로드 설정
            </motion.h1>
            <motion.p 
              className="text-sm md:text-base text-gray-600"
              variants={itemVariants}
            >
              영상 정보를 설정하고 유튜브에 업로드하세요
            </motion.p>
          </motion.div>

          <motion.div variants={containerVariants} initial="hidden" animate="visible">
            <Card className="overflow-hidden bg-white/80 backdrop-blur-lg shadow-2xl border border-white/20">
              <CardHeader className="p-0">
                <motion.div 
                  className="relative aspect-square cursor-pointer group" 
                  onClick={() => video.video_url && setIsVideoModalOpen(true)}
                  whileHover={{ scale: 1.02 }}
                  transition={{ duration: 0.2 }}
                  variants={itemVariants}
                >
                  <img
                    src={video.thumbnail_url || ""}
                    alt={video.title}
                    className="object-cover w-full h-full rounded-t-lg"
                  />
                  <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center rounded-t-lg">
                    <div className="bg-red-600 rounded-full p-3 md:p-4 shadow-lg transform group-hover:scale-110 transition-transform">
                      <Play className="w-6 h-6 md:w-8 md:h-8 text-white" />
                    </div>
                  </div>
                </motion.div>
              </CardHeader>
              <CardContent className="p-4 md:p-6 space-y-4 md:space-y-6">
                <motion.div variants={itemVariants} className="space-y-1">
                  <p className="text-xs md:text-sm text-gray-500">
                    생성일: {formatDate(video.created_at)}
                  </p>
                </motion.div>

                <motion.div variants={itemVariants} className="space-y-4">
                  <div className="space-y-1.5 md:space-y-2">
                    <Label htmlFor="title" className="text-xs md:text-sm font-medium">
                      제목
                    </Label>
                    <Input
                      id="title"
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      placeholder="ShoShoShort 영상"
                      className="text-sm md:text-base border-gray-300 focus:border-red-500 focus:ring-red-500"
                    />
                  </div>

                  <div className="space-y-1.5 md:space-y-2">
                    <Label htmlFor="description" className="text-xs md:text-sm font-medium">
                      설명
                    </Label>
                    <Textarea
                      id="description"
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="간단한 설명이나 해시태그를 입력하세요"
                      className="text-sm md:text-base border-gray-300 focus:border-red-500 focus:ring-red-500 min-h-[80px] md:min-h-[100px]"
                    />
                  </div>
                </motion.div>

                <motion.div variants={itemVariants} className="flex items-center space-x-2 pt-2">
                  <div 
                    className={`w-4 md:w-5 h-4 md:h-5 flex items-center justify-center rounded border transition-colors cursor-pointer
                      ${isConfirmed 
                        ? 'bg-red-600 border-red-600 text-white' 
                        : 'border-gray-300 hover:border-red-600'
                      }`}
                    onClick={() => setIsConfirmed(!isConfirmed)}
                  >
                    {isConfirmed && <Check className="w-2 md:w-3 h-2 md:h-3" />}
                  </div>
                  <label
                    className="text-xs md:text-sm font-medium leading-none cursor-pointer select-none"
                    onClick={() => setIsConfirmed(!isConfirmed)}
                  >
                    위 영상을 유튜브에 업로드하는 것에 동의합니다
                  </label>
                </motion.div>

                <motion.div variants={itemVariants} className="flex justify-end space-x-2 md:space-x-4 pt-4">
                  <Button
                    variant="outline"
                    onClick={handleCloseWindow}
                    className="text-xs md:text-sm py-1.5 md:py-2 px-3 md:px-4 hover:bg-gray-100/80 border-gray-300"
                  >
                    취소
                  </Button>
                  <Button
                    onClick={handleUpload}
                    disabled={!isConfirmed || isUploading}
                    className="text-xs md:text-sm py-1.5 md:py-2 px-3 md:px-4 bg-red-600 hover:bg-red-700 text-white"
                  >
                    {isUploading ? (
                      <div className="flex items-center gap-1 md:gap-2">
                        <span className="animate-spin h-3 w-3 md:h-4 md:w-4 border-2 border-white border-opacity-50 border-t-white rounded-full"></span>
                        업로드 중...
                      </div>
                    ) : (
                      <div className="flex items-center gap-1 md:gap-2">
                        <Youtube className="w-3 h-3 md:w-4 md:h-4" />
                        유튜브에 업로드
                      </div>
                    )}
                  </Button>
                </motion.div>
              </CardContent>
            </Card>
          </motion.div>

          <motion.div 
            variants={itemVariants}
            className="text-center text-xs md:text-sm text-gray-500 bg-white/50 backdrop-blur-sm rounded-xl p-3 md:p-4 border border-white/20 shadow-sm"
          >
            <p className="flex items-center justify-center gap-2">
              <span className="text-red-500">⚠️</span>
              유튜브 가이드라인을 준수한 콘텐츠만 업로드해 주세요
            </p>
          </motion.div>
        </motion.div>
      </main>

      <Dialog open={isVideoModalOpen} onOpenChange={setIsVideoModalOpen}>
        <DialogContent className="max-w-[90vw] md:max-w-4xl bg-white/90 backdrop-blur-md">
          <DialogHeader>
            <DialogTitle className="text-lg md:text-xl font-bold">{video.title}</DialogTitle>
            <DialogDescription className="text-xs md:text-sm">
              동영상을 시청하고 확인할 수 있습니다.
            </DialogDescription>
          </DialogHeader>
          <div className="aspect-video w-full rounded-lg overflow-hidden">
            <video
              src={video.video_url || ""}
              controls
              className="w-full h-full"
              title={video.title}
            />
          </div>
        </DialogContent>
      </Dialog>

      <Dialog open={isSuccessModalOpen} onOpenChange={setIsSuccessModalOpen}>
        <DialogContent className="w-[90%] max-w-xs sm:max-w-md bg-white/90 backdrop-blur-md">
          <DialogHeader>
            <DialogTitle className="text-center text-lg md:text-xl font-bold">업로드 요청 완료</DialogTitle>
            <DialogDescription className="text-center text-xs md:text-sm">
              유튜브 숏츠 업로드 요청이 성공적으로 완료되었습니다.
              <br />
              영상 처리 상태는 대시보드에서 확인하실 수 있습니다.
            </DialogDescription>
          </DialogHeader>
          <div className="flex justify-center mt-4 md:mt-6">
            <Button
              onClick={handleCloseWindow}
              className="text-xs md:text-sm bg-red-600 hover:bg-red-700 text-white px-6 md:px-8 py-1.5 md:py-2"
            >
              확인
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
} 