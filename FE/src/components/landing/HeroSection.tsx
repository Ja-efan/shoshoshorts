import React from "react";
import { Link } from "react-router-dom";
import { Play, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";

export const HeroSection: React.FC = () => {
  return (
    <section id="hero" className="relative overflow-hidden bg-gradient-to-br from-red-50 via-white to-red-50 py-20">
      <div className="absolute -right-40 -top-40 h-96 w-96 rounded-full bg-red-100/50"></div>
      <div className="absolute -bottom-40 -left-40 h-96 w-96 rounded-full bg-red-100/50"></div>

      <div className="container relative px-4">
        <div className="grid gap-12 md:grid-cols-2 md:items-center">
          <div className="space-y-6">
            <h1 className="text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl">
              당신의 <span className="text-red-600">이야기</span>를 영상으로 만들어보세요
            </h1>
            <p className="text-lg text-gray-600">
              텍스트만으로 전문적인 1분짜리 영상을 몇 초 만에 제작하세요. 편집 기술이 필요 없습니다.
            </p>
            <div className="flex flex-wrap gap-4">
              <Link to="/create">
                <Button size="lg" className="bg-red-600 hover:bg-red-700">
                  <Play className="mr-2 h-5 w-5" />
                  지금 시작하기
                </Button>
              </Link>
            </div>
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <CheckCircle2 className="h-4 w-4 text-green-500" />
              편집 기술이 필요하지 않습니다.
            </div>
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <CheckCircle2 className="h-4 w-4 text-green-500" />
              기획 기술이 필요하지 않습니다.
            </div>
          </div>

          <div className="relative mx-auto aspect-video w-full max-w-lg rounded-xl border bg-white p-2 shadow-lg">
            <div className="relative h-full w-full overflow-hidden rounded-lg bg-gray-100">
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="rounded-full bg-white/80 p-4">
                  <Play className="h-12 w-12 text-red-600" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};