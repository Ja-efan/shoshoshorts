import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Clock, Loader2 } from "lucide-react"
import { VideoData } from "@/types/video"

interface InProgressVideoCardProps {
  video: VideoData
}

export function InProgressVideoCard({ video }: InProgressVideoCardProps) {
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
        <div className="flex items-start justify-between">
          <h3 className="font-semibold line-clamp-1" title={video.title}>
            {video.title}
          </h3>
          <Badge variant="outline" className="flex items-center gap-1 bg-blue-50 text-blue-700">
            <Loader2 className="h-3 w-3 animate-spin" />
            처리 중
          </Badge>
        </div>
      </div>
    </Card>
  )
} 