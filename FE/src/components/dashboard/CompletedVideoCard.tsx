import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card"
import { MoreVertical, Play, Download, Share2, Trash2 } from "lucide-react"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import toast from "react-hot-toast"
import { useState } from "react"
import { VideoData } from "@/types/video"
import { formatDate } from "@/lib/utils"

interface CompletedVideoCardProps {
  video: VideoData
}

export function CompletedVideoCard({ video }: CompletedVideoCardProps) {
  const [isModalOpen, setIsModalOpen] = useState(false)

  const handleDownload = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (video.video_url) {
      window.open(video.video_url, '_blank')
    }
  }

  const handleShare = (e: React.MouseEvent) => {
    e.stopPropagation()
    toast.success("공유 기능은 현재 개발 중입니다")
  }

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation()
    toast.success("삭제 기능은 현재 개발 중입니다")
  }

  return (
    <>
      <Card 
        className="group relative overflow-hidden cursor-pointer hover:shadow-lg transition-shadow"
        onClick={() => setIsModalOpen(true)}
      >
        <CardHeader className="p-0">
          <div className="relative aspect-video">
            <img
              src={video.thumbnail_url || ""}
              alt={video.title}
              className="object-cover w-full h-full"
            />
            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <Play className="w-12 h-12 text-white" />
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-4">
          <h3 className="font-semibold line-clamp-2">{video.title}</h3>
          <p className="text-sm text-gray-500 mt-1">
            완료됨 • {formatDate(video.completed_at)}
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
              <DropdownMenuItem onClick={handleShare}>
                <Share2 className="mr-2 h-4 w-4" />
                공유하기
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
    </>
  )
} 