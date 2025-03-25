import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

export function CTASection() {
  return (
    <section className="bg-gradient-to-r from-red-600 to-red-700 py-20 text-white">
      <div className="container px-4 text-center">
        <h2 className="text-4xl font-bold tracking-tight sm:text-5xl">
          당신의 이야기에 생명을 불어넣을 준비가 되셨나요?
        </h2>
        <p className="mx-auto mt-6 max-w-2xl text-xl text-white/90">
          이미 수천 명의 크리에이터들이 자신의 이야기를 매력적인 영상으로 변환했습니다
        </p>
        <div className="mt-10">
          <Link to="/create">
            <Button 
              size="lg" 
              className="bg-white px-8 py-6 text-lg font-semibold text-red-600 hover:bg-white/90 transition-all duration-200"
            >
              무료로 시작하기
            </Button>
          </Link>
        </div>
      </div>
    </section>
  );
}