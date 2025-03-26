export interface VideoData {
  title: string
  status: "FAILED" | "COMPLETED" | "PENDING"
  completed_at: string | null
  thumbnail_url: string | null
  video_url: string | null
  story_id: string
} 