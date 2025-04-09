export type VideoStatus = "PROCESSING" | "COMPLETED" | "FAILED" | "PENDING";

export interface VideoStatusResponse {
  storyId: string;
  status: VideoStatus;
  createdAt: string;
  processingStep: string | null;
  processing_step?: string | null;
  error?: string | null;
}

export interface VideoData {
  title: string
  status: "FAILED" | "COMPLETED" | "PROCESSING" | "PENDING"
  completed_at: string | null
  thumbnail_url: string | null
  video_url: string | null
  story_id: string
  is_uploaded?: boolean
  created_at: string
} 