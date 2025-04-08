import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card"
import { MoreVertical, Play, Download, Share2, Trash2, Check, X } from "lucide-react"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import toast from "react-hot-toast"
import { useState } from "react"
import { VideoData } from "@/types/video"
import { formatDate } from "@/lib/utils"
import { apiService } from "@/api/api"

const getGMTString = () => {
  const offset = new Date().getTimezoneOffset()
  const gmtOffset = -offset / 60
  return `(GMT ${gmtOffset >= 0 ? '+' : ''}${gmtOffset})`
}

interface CompletedVideoCardProps {
  video: VideoData
  onUploadComplete?: (videoId: string) => void
}

export function CompletedVideoCard({ video, onUploadComplete }: CompletedVideoCardProps) {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isShareModalOpen, setIsShareModalOpen] = useState(false)
  const [title, setTitle] = useState(video.title)
  const [description, setDescription] = useState("")
  const [isUploading, setIsUploading] = useState(false)

  const handleDownload = async (e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      await apiService.downloadVideo(video.story_id)
    } catch (error) {
      toast.error("다운로드 중 오류가 발생했습니다")
      console.error(error)
    }
  }

  const handleShare = async (e: React.MouseEvent) => {
    e.stopPropagation()
    if (video.is_uploaded) {
      toast.error("이미 유튜브에 업로드된 영상입니다")
      return
    }
    
    try {
      // 새 창에서 업로드/유튜브 로그인 페이지 열기
      const youtubeLoginUrl = `/upload/youtube/login?storyId=${video.story_id}`;
      
      // 모바일 디바이스 확인
      const isMobile = /iPhone|iPad|iPod|Android/i.test(navigator.userAgent);
      
      if (isMobile) {

        // 모바일에서는 새 탭으로 열기
        window.open(
          youtubeLoginUrl, 
          '_blank'
        );

      } else {
        // 데스크톱에서는 팝업 창으로 열기
        const width = Math.min(600, window.innerWidth * 0.9);
        const height = Math.min(800, window.innerHeight * 0.9);
        const left = (window.innerWidth - width) / 2;
        const top = (window.innerHeight - height) / 2;
        
        window.open(
          youtubeLoginUrl, 
          '_blank', 
          `width=${width},height=${height},left=${left},top=${top},resizable=yes,scrollbars=yes`
        );
      }
    } catch (error) {
      toast.error("YouTube 인증 처리 중 오류가 발생했습니다");
      console.error(error);
    }
  }

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation()
    toast.success("삭제 기능은 현재 개발 중입니다")
  }

  const handleUploadToYoutube = async () => {
    try {
      setIsUploading(true)
      if (!video.video_url) {
        toast.error("비디오 URL이 없습니다")
        return
      }
      await apiService.uploadVideoToYoutube(video.video_url, title, description)
      
      toast.success("유튜브 업로드 요청이 완료되었습니다")
      if (onUploadComplete) {
        onUploadComplete(video.story_id)
      }
      setIsShareModalOpen(false)
    } catch (error) {
      toast.error("업로드 중 오류가 발생했습니다")
      console.error(error)
    } finally {
      setIsUploading(false)
    }
  }

  return (
    <>
      <Card 
        className="group relative overflow-hidden cursor-pointer hover:shadow-lg transition-all duration-300"
        onClick={() => setIsModalOpen(true)}
      >
        <CardHeader className="p-0">
          <div className="relative aspect-square">
            <img
              src={video.thumbnail_url || ""}
              alt={video.title}
              className="object-cover w-full h-full transition-transform duration-300 group-hover:scale-105"
            />
            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <Play className="w-12 h-12 text-white" />
            </div>
            {video.is_uploaded && (
              <div className="absolute top-2 right-2 bg-green-500 rounded-full p-1.5 shadow-md" title="유튜브에 업로드됨">
                <Check className="w-4 h-4 text-white" />
              </div>
            )}
          </div>
        </CardHeader>
        <CardContent className="p-4 space-y-2">
          <h3 className="font-semibold line-clamp-2 text-lg">{video.title}</h3>
          <p className="text-sm text-gray-500">
            완료됨 • {formatDate(video.completed_at)} {getGMTString()}
          </p>
        </CardContent>
        <CardFooter className="p-4 pt-0">
          <DropdownMenu>
            <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
              <Button variant="ghost" size="icon" className="ml-auto">
                <MoreVertical className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={handleDownload}>
                <Download className="mr-2 h-4 w-4" />
                다운로드
              </DropdownMenuItem>
              <DropdownMenuItem 
                onClick={handleShare}
                className={video.is_uploaded ? "opacity-50 cursor-not-allowed" : ""}
                disabled={video.is_uploaded}
              >
                <Share2 className="mr-2 h-4 w-4" />
                {video.is_uploaded ? "이미 공유됨" : "유튜브에 공유"}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={handleDelete} className="text-red-600">
                <Trash2 className="mr-2 h-4 w-4" />
                삭제
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </CardFooter>
      </Card>

      <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
        <DialogContent className="max-w-4xl">
          <DialogHeader>
            <DialogTitle>{video.title}</DialogTitle>
            <DialogDescription>
              동영상을 시청하고 관리할 수 있습니다.
            </DialogDescription>
            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute right-4 top-4 rounded-sm opacity-70 ring-offset-background transition-opacity hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:pointer-events-none data-[state=open]:bg-accent data-[state=open]:text-muted-foreground"
            >
              <X className="h-4 w-4" />
              <span className="sr-only">닫기</span>
            </button>
          </DialogHeader>
          <div className="aspect-video w-full">
            <video
              src={video.video_url || ""}
              controls
              className="w-full h-full"
              title={video.title}
            />
          </div>
        </DialogContent>
      </Dialog>

      <Dialog open={isShareModalOpen} onOpenChange={setIsShareModalOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>유튜브에 공유하기</DialogTitle>
            <DialogDescription>
              유튜브에 업로드할 동영상의 제목과 설명을 입력해주세요.
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="youtube-title" className="text-right">
                제목
              </Label>
              <Input
                id="youtube-title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="col-span-3"
              />
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="youtube-description" className="text-right">
                설명
              </Label>
              <Textarea
                id="youtube-description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="col-span-3"
                rows={4}
              />
            </div>
          </div>
          <DialogFooter>
            <Button onClick={() => setIsShareModalOpen(false)} variant="outline">
              취소
            </Button>
            <Button onClick={handleUploadToYoutube} disabled={isUploading}>
              {isUploading ? "업로드 중..." : "유튜브에 업로드"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
} 