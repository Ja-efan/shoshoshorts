import React from "react";
import { Cpu, Palette, Layers, Video } from "lucide-react";
import { SectionHeader } from "@/components/common/Section";
import { TechCard } from "./TechCard";

export const TechnologySection: React.FC = () => {
  const techCards = [
    {
      icon: Cpu,
      title: "자연어 처리",
      description: "고급 NLP 알고리즘이 이야기의 맥락, 감정, 내러티브 구조를 이해하여 일관된 시각적 표현을 만듭니다.",
      features: [
        "감정 장면을 위한 감정 분석",
        "캐릭터 관계 매핑",
        "장면 감지 및 분할",
      ],
    },
    {
      icon: Palette,
      title: "시각적 생성",
      description: "최첨단 이미지 생성 모델이 이야기 설명을 바탕으로 시각적으로 멋진 장면을 만듭니다.",
      features: [
        "동적 장면 구성",
        "설명 기반 캐릭터 시각화",
        "영상 전체의 일관된 시각적 스타일",
      ],
    },
    {
      icon: Layers,
      title: "음성 합성",
      description: "생생한 음성 생성으로 자연스러운 대화와 내레이션을 통해 캐릭터에 생명을 불어넣습니다.",
      features: [
        "다양한 캐릭터를 위한 음성 옵션",
        "대화를 위한 감정 톤 매칭",
        "자연스러운 속도와 강조",
      ],
    },
    {
      icon: Video,
      title: "영상 구성",
      description: "자동화된 영상 편집 기술이 시각, 오디오, 전환을 매끄러운 최종 결과물로 결합합니다.",
      features: [
        "동적 장면 전환",
        "이야기 분위기에 맞는 배경 음악",
        "전문적인 속도와 타이밍",
      ],
    },
  ];

  return (
    <section id="technology" className="py-20">
      <div className="container px-4">
        <SectionHeader
          title="쇼쇼숓의 기술"
          description="최첨단 AI와 머신 러닝 기술로 구동됩니다"
        />

        <div className="mt-16 grid gap-8 md:grid-cols-2">
          {techCards.map((card, index) => (
            <TechCard
              key={index}
              icon={card.icon}
              title={card.title}
              description={card.description}
              features={card.features}
            />
          ))}
        </div>
      </div>
    </section>
  );
};