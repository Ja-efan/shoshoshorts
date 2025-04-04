import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { AlertCircle } from "lucide-react"
import { VideoData } from "@/types/video"
import toast from "react-hot-toast"

interface FailedVideoCardProps {
  video: VideoData
}

export function FailedVideoCard({ video }: FailedVideoCardProps) {
  const handleRetry = () => {
    toast.info("추후 개발 예정입니다")
  }

  return (
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
        <Button variant="outline" size="sm" className="mt-3 w-full" onClick={handleRetry}>
          다시 시도
        </Button>
      </div>
    </Card>
  )
} 