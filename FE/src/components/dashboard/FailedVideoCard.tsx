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

  const handleRetry = () => {
    toast.success("추후 개발 예정입니다")
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
      <Card className="overflow-hidden">
        <div className="relative aspect-video w-full">
          <div className="flex h-full w-full items-center justify-center bg-gray-100">
            <AlertCircle className="h-12 w-12 text-red-400" />
          </div>
        </div>
        <div className="p-4">
          <div className="flex items-start justify-between">
            <h3 className="font-semibold line-clamp-1" title={video.title}>
              {video.title}
            </h3>
            <Badge variant="outline" className="flex items-center gap-1 bg-red-50 text-red-700">
              <AlertCircle className="h-3 w-3" />
              실패
            </Badge>
          </div>
          <div className="mt-3 flex gap-2">
            <Button variant="outline" size="sm" className="flex-1" onClick={handleRetry}>
              다시 시도
            </Button>
            <Button 
              variant="outline" 
              size="sm" 
              className="flex-1 text-red-600 hover:text-red-700 hover:bg-red-50" 
              onClick={() => setIsDeleteModalOpen(true)}
            >
              <Trash2 className="mr-2 h-4 w-4" />
              삭제
            </Button>
          </div>
        </div>
      </Card>

      <Dialog open={isDeleteModalOpen} onOpenChange={setIsDeleteModalOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>영상 삭제</DialogTitle>
            <DialogDescription>
              정말로 이 영상을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
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