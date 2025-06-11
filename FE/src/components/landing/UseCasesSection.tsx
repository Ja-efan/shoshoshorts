import React from "react";
import { SectionHeader } from "@/components/common/Section";

interface UseCaseCardProps {
  title: string;
  description: string;
}

const UseCaseCard: React.FC<UseCaseCardProps> = ({ title, description }) => {
  return (
    <div className="rounded-xl border bg-white p-6 shadow-sm">
      <h3 className="text-xl font-semibold">{title}</h3>
      <p className="mt-2 text-gray-600">{description}</p>
    </div>
  );
};

export const UseCasesSection: React.FC = () => {
  const useCases = [
    {
      title: "콘텐츠 크리에이터",
      description: "틱톡, 인스타그램, 유튜브와 같은 소셜 미디어 플랫폼을 위한 매력적인 영상으로 아이디어를 빠르게 변환하세요.",
    },
    {
      title: "교육자",
      description: "학생들의 참여를 유도하고 학습 경험을 향상시키는 교육 콘텐츠와 스토리텔링 영상을 만드세요.",
    },
    {
      title: "마케터",
      description: "비싼 제작 장비 없이도 프로모션 영상과 제품 스토리를 개발하세요.",
    },
    {
      title: "작가",
      description: "당신의 이야기의 본질을 담아내는 시각적 각색으로 작품에 생명을 불어넣으세요.",
    },
    {
      title: "소규모 비즈니스",
      description: "영상 제작팀을 고용하지 않고도 전문적인 마케팅 영상과 소셜 미디어 콘텐츠를 만드세요.",
    },
    {
      title: "개인 사용자",
      description: "개인적인 이야기, 추억, 경험을 친구와 가족과 공유할 수 있는 영상으로 변환하세요.",
    },
  ];

  return (
    <section id="use-cases" className="py-20 bg-gray-50">
      <div className="container px-4">
        <SectionHeader
          title="누가 쇼쇼숏을 사용하나요?"
          description="우리 플랫폼은 다양한 스토리텔링 요구사항을 가진 폭넓은 사용자를 지원합니다"
        />

        <div className="mt-16 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {useCases.map((useCase, index) => (
            <UseCaseCard
              key={index}
              title={useCase.title}
              description={useCase.description}
            />
          ))}
        </div>
      </div>
    </section>
  );
};