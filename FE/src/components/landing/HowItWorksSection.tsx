import React from "react";
import { SectionHeader } from "@/components/common/Section";

interface StepCardProps {
  number: number;
  title: string;
  description: string;
}

const StepCard: React.FC<StepCardProps> = ({ number, title, description }) => {
  return (
    <div className="relative rounded-xl border bg-white p-6 shadow-sm">
      <div className="absolute -top-5 left-6 flex h-10 w-10 items-center justify-center rounded-full bg-red-600 text-white">
        {number}
      </div>
      <div className="mt-4 space-y-4">
        <h3 className="text-xl font-semibold">{title}</h3>
        <p className="text-gray-600">{description}</p>
      </div>
    </div>
  );
};

export const HowItWorksSection: React.FC = () => {
  const steps = [
    {
      title: "이야기 작성",
      description: "이야기를 입력하고 최대 4명의 캐릭터에 이름, 성별, 음성 옵션을 추가하세요.",
    },
    {
      title: "AI 처리",
      description: "우리의 고급 AI가 이야기를 분석하고 음성 내레이션이 포함된 매력적인 영상으로 변환합니다.",
    },
    {
      title: "공유 및 다운로드",
      description: "HD 품질의 영상을 다운로드하거나 소셜 미디어 플랫폼에 직접 공유하세요.",
    },
  ];

  return (
    <section id="how-it-works" className="py-20 bg-gray-50">
      <div className="container px-4">
        <SectionHeader
          title="쇼쇼숓 사용 방법"
          description="우리의 AI 기반 플랫폼은 세 가지 간단한 단계로 영상 제작을 쉽게 만듭니다"
        />

        <div className="mt-16 grid gap-8 md:grid-cols-3">
          {steps.map((step, index) => (
            <StepCard
              key={index}
              number={index + 1}
              title={step.title}
              description={step.description}
            />
          ))}
        </div>
      </div>
    </section>
  );
};