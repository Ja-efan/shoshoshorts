"use client"

import { useState, useEffect } from "react"
import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Search, Plus, Clock, Loader2 } from "lucide-react"
import { apiService } from "@/api/api"
import { VideoData } from "@/types/video"
import { CompletedVideoCard } from "@/components/dashboard/CompletedVideoCard"
import { InProgressVideoCard } from "@/components/dashboard/InProgressVideoCard"
import { FailedVideoCard } from "@/components/dashboard/FailedVideoCard"
import shortLogo from "@/assets/short_logo.png";

export default function DashboardPage() {
  const [searchQuery, setSearchQuery] = useState("")
  const [videos, setVideos] = useState<VideoData[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const fetchVideos = async () => {
      try {
        const response = await apiService.getVideos()
        setVideos(response.data)
      } catch (error) {
        console.error('Failed to fetch videos:', error)
      } finally {
        setIsLoading(false)
      }
    }

    fetchVideos()
  }, [])

  // Filter videos based on search query
  const filteredVideos = videos.filter((video) =>
    video.title.toLowerCase().includes(searchQuery.toLowerCase())
  )

  // Separate videos by status
  const completedVideos = filteredVideos.filter(video => video.status === "COMPLETED")
  const inProgressVideos = filteredVideos.filter(video => video.status === "PROCESSING")
  const failedVideos = filteredVideos.filter(video => video.status === "FAILED")

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-red-600" />
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-10 bg-white border-b">
        <div className="container mx-auto flex h-16 items-center justify-between px-4">
          <div className="flex items-center gap-2">
            <Link to="/" className="flex items-center gap-2">
              <img src={shortLogo} alt="쇼쇼숓 로고" className="h-8 w-8" />
              <span className="text-xl font-bold">쇼쇼숓</span>
            </Link>
          </div>
          <div className="flex items-center gap-4">
            <Link to="/create">
              <Button className="bg-red-600 hover:bg-red-700">
                <Plus className="mr-2 h-4 w-4" />
                동영상 만들기
              </Button>
            </Link>
          </div>
        </div>
      </header>

      <main className="flex-1 py-8">
        <div className="container mx-auto px-4">
          <div className="flex flex-col gap-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <h1 className="text-3xl font-bold">내 동영상</h1>
              <div className="relative w-full max-w-xs">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-500" />
                <Input
                  type="search"
                  placeholder="동영상 검색..."
                  className="pl-9"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
            </div>

            <Tabs defaultValue="all" className="w-full">
              <TabsList className="mb-6">
                <TabsTrigger value="all">전체</TabsTrigger>
                <TabsTrigger value="completed">완료됨</TabsTrigger>
                <TabsTrigger value="in-progress">처리 중</TabsTrigger>
              </TabsList>

              <TabsContent value="all" className="space-y-8">
                {/* In Progress Section */}
                {inProgressVideos.length > 0 && (
                  <div>
                    <h2 className="mb-4 text-xl font-semibold">처리 중</h2>
                    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                      {inProgressVideos.map((video) => (
                        <InProgressVideoCard key={video.story_id} video={video} />
                      ))}
                    </div>
                  </div>
                )}

                {/* Completed Section */}
                {completedVideos.length > 0 && (
                  <div>
                    <h2 className="mb-4 text-xl font-semibold">완료됨</h2>
                    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                      {completedVideos.map((video) => (
                        <CompletedVideoCard key={video.story_id} video={video} />
                      ))}
                    </div>
                  </div>
                )}

                {/* Failed Section */}
                {failedVideos.length > 0 && (
                  <div>
                    <h2 className="mb-4 text-xl font-semibold">실패</h2>
                    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                      {failedVideos.map((video) => (
                        <FailedVideoCard key={video.story_id} video={video} />
                      ))}
                    </div>
                  </div>
                )}

                {/* Empty State */}
                {filteredVideos.length === 0 && (
                  <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-12">
                    <div className="rounded-full bg-gray-100 p-3">
                      <Search className="h-8 w-8 text-gray-400" />
                    </div>
                    <h3 className="mt-4 text-lg font-medium">동영상을 찾을 수 없습니다</h3>
                    <p className="mt-1 text-sm text-gray-500">
                      {searchQuery ? "다른 검색어를 시도해보세요" : "첫 번째 동영상을 만들어보세요"}
                    </p>
                    {!searchQuery && (
                      <Link to="/create" className="mt-4">
                        <Button className="bg-red-600 hover:bg-red-700">
                          <Plus className="mr-2 h-4 w-4" />
                          동영상 만들기
                        </Button>
                      </Link>
                    )}
                  </div>
                )}
              </TabsContent>

              <TabsContent value="completed">
                {completedVideos.length > 0 ? (
                  <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                    {completedVideos.map((video) => (
                      <CompletedVideoCard key={video.story_id} video={video} />
                    ))}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-12">
                    <div className="rounded-full bg-gray-100 p-3">
                      <Search className="h-8 w-8 text-gray-400" />
                    </div>
                    <h3 className="mt-4 text-lg font-medium">완료된 동영상이 없습니다</h3>
                    <p className="mt-1 text-sm text-gray-500">
                      {searchQuery ? "다른 검색어를 시도해보세요" : "완료된 동영상이 여기에 표시됩니다"}
                    </p>
                  </div>
                )}
              </TabsContent>

              <TabsContent value="in-progress">
                {inProgressVideos.length > 0 ? (
                  <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                    {inProgressVideos.map((video) => (
                      <InProgressVideoCard key={video.story_id} video={video} />
                    ))}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-12">
                    <div className="rounded-full bg-gray-100 p-3">
                      <Clock className="h-8 w-8 text-gray-400" />
                    </div>
                    <h3 className="mt-4 text-lg font-medium">처리 중인 동영상이 없습니다</h3>
                    <p className="mt-1 text-sm text-gray-500">
                      {searchQuery ? "다른 검색어를 시도해보세요" : "처리 중인 동영상이 여기에 표시됩니다"}
                    </p>
                  </div>
                )}
              </TabsContent>
            </Tabs>
          </div>
        </div>
      </main>
    </div>
  )
}