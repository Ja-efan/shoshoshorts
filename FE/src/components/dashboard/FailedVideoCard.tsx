import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { AlertCircle, Trash2 } from "lucide-react"
import { VideoData } from "@/types/video"
import toast from "react-hot-toast"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog"
import { useState } from "react"
import { apiService } from "@/api/api"

interface FailedVideoCardProps {
  video: VideoData
}

export function FailedVideoCard({ video }: FailedVideoCardProps) {
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false)
  const [isRetryModalOpen, setIsRetryModalOpen] = useState(false)

  const handleRetry = async () => {
    try {
      await apiService.retryVideo(video.story_id)
      toast.success("영상 생성을 다시 시도합니다")
      window.location.reload()
    } catch (error) {
      toast.error("다시 시도 중 오류가 발생했습니다")
      console.error(error)
    }
  }

  const handleDelete = async () => {
    try {
      await apiService.deleteVideo(video.story_id)
      toast.success("영상이 삭제되었습니다")
      window.location.reload()
    } catch (error) {
      toast.error("영상 삭제 중 오류가 발생했습니다")
      console.error(error)
    }
  }

  return (
    <>
      <Card className="overflow-hidden border border-gray-200 shadow-sm hover:shadow-md transition-shadow">
        <div className="relative aspect-video w-full">
          <div className="flex h-full w-full items-center justify-center bg-gray-100">
            <AlertCircle className="h-12 w-12 text-red-400" />
          </div>
          <div className="absolute inset-x-0 bottom-0 h-1 bg-red-200">
            <div className="h-full w-full bg-red-500"></div>
          </div>
        </div>
        <div className="p-4">
          <div className="flex items-start justify-between mb-4">
            <h3 className="font-semibold line-clamp-1 text-gray-800" title={video.title}>
              {video.title}
            </h3>
            <Badge variant="outline" className="flex items-center gap-1.5 py-1 bg-red-50 text-red-700 border-red-200">
              <AlertCircle className="h-3 w-3" />
              <span>실패</span>
            </Badge>
          </div>
          <div className="flex gap-2">
            <Button 
              variant="outline" 
              size="sm" 
              className="flex-1 border-gray-200 hover:bg-gray-50 hover:text-gray-900" 
              onClick={() => setIsRetryModalOpen(true)}
            >
              다시 시도
            </Button>
            <Button 
              variant="outline" 
              size="sm" 
              className="flex-1 text-red-600 hover:text-red-700 hover:bg-red-50 border-red-200" 
              onClick={() => setIsDeleteModalOpen(true)}
            >
              <Trash2 className="mr-2 h-4 w-4" />
              삭제
            </Button>
          </div>
        </div>
      </Card>

      <Dialog open={isRetryModalOpen} onOpenChange={setIsRetryModalOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle className="text-xl font-semibold">영상 생성 재시도</DialogTitle>
            <DialogDescription className="text-gray-600 mt-2">
              영상 생성을 다시 시도하시겠습니까? 이 작업은 이전 생성 결과를 덮어쓰게 됩니다.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-6">
            <Button variant="outline" onClick={() => setIsRetryModalOpen(false)}>
              취소
            </Button>
            <Button className="bg-blue-600 hover:bg-blue-700" onClick={handleRetry}>
              다시 시도
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={isDeleteModalOpen} onOpenChange={setIsDeleteModalOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle className="text-xl font-semibold">영상 삭제</DialogTitle>
            <DialogDescription className="text-gray-600 mt-2">
              정말로 이 영상을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-6">
            <Button variant="outline" onClick={() => setIsDeleteModalOpen(false)}>
              취소
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              삭제
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
} 