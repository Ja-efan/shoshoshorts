import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Video, MoreVertical, Download, Share2, Trash2 } from "lucide-react"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { VideoData } from "@/types/video"

interface CompletedVideoCardProps {
  video: VideoData
}

export function CompletedVideoCard({ video }: CompletedVideoCardProps) {
  return (
    <Card className="overflow-hidden">
      <Link to={`/video/${video.story_id}`}>
        <div className="relative aspect-video w-full">
          {video.thumbnail_url ? (
            <img
              src={video.thumbnail_url}
              alt={video.title}
              className="h-full w-full object-cover"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center bg-gray-100">
              <Video className="h-12 w-12 text-gray-400" />
            </div>
          )}
        </div>
      </Link>
      <div className="p-4">
        <div className="flex items-start justify-between">
          <Link to={`/video/${video.story_id}`}>
            <h3 className="font-semibold line-clamp-1 hover:text-red-600" title={video.title}>
              {video.title}
            </h3>
          </Link>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                <MoreVertical className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem className="flex items-center gap-2">
                <Download className="h-4 w-4" />
                <span>다운로드</span>
              </DropdownMenuItem>
              <DropdownMenuItem className="flex items-center gap-2">
                <Share2 className="h-4 w-4" />
                <span>공유</span>
              </DropdownMenuItem>
              <DropdownMenuItem className="flex items-center gap-2 text-red-600">
                <Trash2 className="h-4 w-4" />
                <span>삭제</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
        <div className="mt-2 flex items-center justify-between text-sm text-gray-500">
          <span>{video.completed_at}</span>
        </div>
      </div>
    </Card>
  )
} 