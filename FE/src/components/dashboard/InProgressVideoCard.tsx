import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Clock, Loader2 } from "lucide-react"
import { VideoData } from "@/types/video"

interface InProgressVideoCardProps {
  video: VideoData
  statusText?: string
}

export function InProgressVideoCard({ video, statusText = "처리 중" }: InProgressVideoCardProps) {
  return (
    <Card className="overflow-hidden">
      <div className="relative aspect-video w-full">
        <div className="flex h-full w-full items-center justify-center bg-gray-100">
          <Clock className="h-12 w-12 text-gray-400" />
        </div>
        <div className="absolute inset-x-0 bottom-0 h-1 bg-gray-200">
          <div className="h-full bg-blue-600 animate-pulse"></div>
        </div>
      </div>
      <div className="p-4">
        <h3 className="font-medium">{video.title}</h3>
        <div className="mt-2 flex items-center gap-2">
          <Badge variant="secondary" className="flex items-center gap-1">
            <Loader2 className="h-3 w-3 animate-spin" />
            {statusText}
          </Badge>
        </div>
      </div>
    </Card>
  )
} 