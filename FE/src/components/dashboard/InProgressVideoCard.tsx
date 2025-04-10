import { Card } from "@/components/ui/card"
import { Clock } from "lucide-react"
import { VideoData } from "@/types/video"
interface InProgressVideoCardProps {
  video: VideoData
  statusText?: string
  onStatusChange?: (storyId: string, newStatus: "FAILED" | "COMPLETED" | "PROCESSING" | "PENDING") => void
}

export function InProgressVideoCard({video}: InProgressVideoCardProps) {

  return (
    <Card className="overflow-hidden border border-gray-200 shadow-sm hover:shadow-md transition-shadow h-full">
      <div className="relative aspect-video w-full">
        <div className="flex h-full w-full items-center justify-center bg-gray-100">
          <Clock className="h-12 w-12 text-gray-400" />
        </div>
        <div className="absolute inset-x-0 bottom-0 h-1 bg-gray-200">
          <div className="h-full bg-blue-600 animate-pulse"></div>
        </div>
      </div>
      <div className="p-4 flex flex-col">
        <h3 className="font-semibold text-gray-800" title={video.title}>
          {video.title}
        </h3>
        <div className="mt-auto">

        </div>
      </div>
    </Card>
  )
} 