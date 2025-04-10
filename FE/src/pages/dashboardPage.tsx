"use client"

import { useState, useEffect, useCallback, useMemo } from "react"
import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Search, Plus, Clock, CheckCircle, AlertCircle, Loader2 } from "lucide-react"
import { apiService } from "@/api/api"
import { VideoData, VideoStatus } from "@/types/video"
import { CompletedVideoCard } from "@/components/dashboard/CompletedVideoCard"
import { InProgressVideoCard } from "@/components/dashboard/InProgressVideoCard"
import { FailedVideoCard } from "@/components/dashboard/FailedVideoCard"
import { Navbar } from "@/components/common/Navbar";

export default function DashboardPage() {
  const [searchQuery, setSearchQuery] = useState("")
  const [videos, setVideos] = useState<VideoData[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [lastUpdate, setLastUpdate] = useState(Date.now())

  // 비디오 데이터 페칭 로직
  const fetchVideos = useCallback(async () => {
    try {
      const response = await apiService.getVideos();
      setVideos(response.data);
      setLastUpdate(Date.now());
    } catch (error) {
      console.error('비디오 데이터 가져오기 실패:', error);
      setError('비디오 데이터를 가져오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  // 비디오 상태 변경 핸들러
  const handleVideoStatusChange = useCallback((storyId: string, newStatus: VideoStatus) => {
    setVideos(prevVideos => 
      prevVideos.map(video => 
        video.story_id === storyId 
          ? { ...video, status: newStatus } 
          : video
      )
    );
  }, []);

  // 초기 데이터 로드
  useEffect(() => {
    fetchVideos();
  }, [fetchVideos]);

  // 30초마다 데이터 새로고침
  useEffect(() => {
    const interval = setInterval(fetchVideos, 30000);
    return () => clearInterval(interval);
  }, [fetchVideos]);

  // 비디오 필터링 로직을 메모이제이션
  const { inProgressVideos, completedVideos, failedVideos } = useMemo(() => {
    const filteredVideos = videos.filter(video => 
      video.title.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return {
      inProgressVideos: filteredVideos.filter(video => 
        video.status === 'PENDING' || video.status === 'PROCESSING'
      ),
      completedVideos: filteredVideos.filter(video => video.status === 'COMPLETED'),
      failedVideos: filteredVideos.filter(video => video.status === 'FAILED')
    };
  }, [videos, searchQuery]);

  // InProgressVideoCard 컴포넌트 렌더링 함수
  const renderInProgressSection = (title: string) => (
    <div className={`rounded-lg bg-white p-6 shadow-sm border border-gray-100 ${inProgressVideos.length === 0 ? 'hidden' : ''}`}>
      <h2 className="mb-6 text-xl font-semibold text-gray-800 flex items-center">
        <Clock className="mr-2 h-5 w-5 text-red-500" />
        {title}
      </h2>
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 auto-rows-fr">
        {inProgressVideos.slice(0, 10).map((video) => (
          <InProgressVideoCard 
            key={video.story_id} 
            video={video} 
            statusText={video.status === "PENDING" ? "대기 중" : "처리 중"}
            onStatusChange={handleVideoStatusChange}
          />
        ))}
      </div>
    </div>
  );

  // 처리 중 비디오가 없을 때 표시할 컴포넌트
  const renderEmptyInProgressSection = (title: string) => (
    <div className="rounded-lg bg-white p-6 shadow-sm border border-gray-100">
      <h2 className="mb-6 text-xl font-semibold text-gray-800 flex items-center">
        <Clock className="mr-2 h-5 w-5 text-red-500" />
        {title}
      </h2>
      <div className="flex flex-col items-center justify-center py-12">
        <div className="rounded-full bg-gray-100 p-4">
          <Clock className="h-8 w-8 text-gray-400" />
        </div>
        <h3 className="mt-6 text-xl font-medium">처리 중인 동영상이 없습니다</h3>
        <p className="mt-2 text-center text-gray-500 max-w-md">
          {searchQuery ? "다른 검색어를 시도해보세요" : "처리 중인 동영상이 여기에 표시됩니다"}
        </p>
        {!searchQuery && (
          <Link to="/create" className="mt-6">
            <Button className="bg-red-600 hover:bg-red-700 px-6">
              <Plus className="mr-2 h-4 w-4" />
              동영상 만들기
            </Button>
          </Link>
        )}
      </div>
    </div>
  );

  // 완료된 비디오 섹션 렌더링 함수
  const renderCompletedSection = () => (
    <div className={`rounded-lg bg-white p-6 shadow-sm border border-gray-100 ${completedVideos.length === 0 ? 'hidden' : ''}`}>
      <h2 className="mb-6 text-xl font-semibold text-gray-800 flex items-center">
        <CheckCircle className="mr-2 h-5 w-5 text-green-500" />
        완료됨
      </h2>
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {completedVideos.map((video) => (
          <CompletedVideoCard key={video.story_id} video={video} />
        ))}
      </div>
    </div>
  );

  // 완료된 비디오가 없을 때 표시할 컴포넌트
  const renderEmptyCompletedSection = () => (
    <div className="rounded-lg bg-white p-6 shadow-sm border border-gray-100">
      <h2 className="mb-6 text-xl font-semibold text-gray-800 flex items-center">
        <CheckCircle className="mr-2 h-5 w-5 text-green-500" />
        완료됨
      </h2>
      <div className="flex flex-col items-center justify-center py-12">
        <div className="rounded-full bg-gray-100 p-4">
          <Search className="h-8 w-8 text-gray-400" />
        </div>
        <h3 className="mt-6 text-xl font-medium">완료된 동영상이 없습니다</h3>
        <p className="mt-2 text-center text-gray-500 max-w-md">
          {searchQuery ? "다른 검색어를 시도해보세요" : "완료된 동영상이 여기에 표시됩니다"}
        </p>
        {!searchQuery && (
          <Link to="/create" className="mt-6">
            <Button className="bg-red-600 hover:bg-red-700 px-6">
              <Plus className="mr-2 h-4 w-4" />
              동영상 만들기
            </Button>
          </Link>
        )}
      </div>
    </div>
  );

  // 실패한 비디오 섹션 렌더링 함수
  const renderFailedSection = () => (
    <div className={`rounded-lg bg-white p-6 shadow-sm border border-gray-100 ${failedVideos.length === 0 ? 'hidden' : ''}`}>
      <h2 className="mb-6 text-xl font-semibold text-gray-800 flex items-center">
        <AlertCircle className="mr-2 h-5 w-5 text-red-600" />
        실패
      </h2>
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {failedVideos.map((video) => (
          <FailedVideoCard key={video.story_id} video={video} />
        ))}
      </div>
    </div>
  );

  // 실패한 비디오가 없을 때 표시할 컴포넌트
  const renderEmptyFailedSection = () => (
    <div className="rounded-lg bg-white p-6 shadow-sm border border-gray-100">
      <h2 className="mb-6 text-xl font-semibold text-gray-800 flex items-center">
        <AlertCircle className="mr-2 h-5 w-5 text-red-600" />
        실패
      </h2>
      <div className="flex flex-col items-center justify-center py-12">
        <div className="rounded-full bg-gray-100 p-4">
          <AlertCircle className="h-8 w-8 text-gray-400" />
        </div>
        <h3 className="mt-6 text-xl font-medium">실패한 동영상이 없습니다</h3>
        <p className="mt-2 text-center text-gray-500 max-w-md">
          {searchQuery ? "다른 검색어를 시도해보세요" : "실패한 동영상이 여기에 표시됩니다"}
        </p>
      </div>
    </div>
  );

  if (isLoading && !videos.length) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-red-600" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <AlertCircle className="mx-auto h-12 w-12 text-red-500" />
          <h2 className="mt-4 text-xl font-semibold text-gray-900">오류가 발생했습니다</h2>
          <p className="mt-2 text-gray-500">{error}</p>
          <Button 
            className="mt-4" 
            onClick={() => window.location.reload()}
          >
            새로고침
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      <Navbar showCreateButton={true} />
      <main className="flex-1 py-8">
        <div className="container mx-auto px-4 sm:px-6">
          <div className="flex flex-col gap-8">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <h1 className="text-3xl font-bold tracking-tight">내 동영상</h1>
              <div className="relative w-full max-w-xs">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-500" />
                <Input
                  type="search"
                  placeholder="동영상 검색..."
                  className="pl-9 border-gray-200 focus:border-red-500 focus:ring-red-500"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
            </div>

            <Tabs defaultValue="all" className="w-full">
              <TabsList className="mb-8 bg-white border border-gray-200 p-1 shadow-sm">
                <TabsTrigger value="all" className="px-6 py-2 data-[state=active]:bg-red-50 data-[state=active]:text-red-700">전체</TabsTrigger>
                <TabsTrigger value="completed" className="px-6 py-2 data-[state=active]:bg-red-50 data-[state=active]:text-red-700">완료됨</TabsTrigger>
                <TabsTrigger value="in-progress" className="px-6 py-2 data-[state=active]:bg-red-50 data-[state=active]:text-red-700">처리 중</TabsTrigger>
              </TabsList>

              <TabsContent value="all" className="space-y-10 m-0">
                {inProgressVideos.length > 0 ? renderInProgressSection("작업 중") : renderEmptyInProgressSection("작업 중")}
                {completedVideos.length > 0 ? renderCompletedSection() : renderEmptyCompletedSection()}
                {failedVideos.length > 0 ? renderFailedSection() : renderEmptyFailedSection()}
              </TabsContent>

              <TabsContent value="completed" className="m-0">
                {completedVideos.length > 0 ? renderCompletedSection() : renderEmptyCompletedSection()}
              </TabsContent>

              <TabsContent value="in-progress" className="m-0">
                {inProgressVideos.length > 0 ? renderInProgressSection("처리 중") : renderEmptyInProgressSection("처리 중")}
              </TabsContent>
            </Tabs>
          </div>
        </div>
      </main>
    </div>
  )
}