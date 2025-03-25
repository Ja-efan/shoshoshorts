import React from "react";
import { Sparkles, Zap, Lightbulb } from "lucide-react";
import { Section, SectionHeader } from "@/components/common/Section";
import { FeatureCard } from "./FeatureCard";

export const WhatIsSection: React.FC = () => {
  const features = [
    {
      icon: Sparkles,
      title: "AI 기반 스토리텔링",
      description: "우리의 고급 AI가 이야기의 맥락, 캐릭터, 감정을 이해하여 시각적으로 매력적인 콘텐츠를 만듭니다.",
    },
    {
      icon: Zap,
      title: "초고속 제작",
      description: "수 시간이 아닌 몇 분 만에 전문적인 품질의 영상을 생성합니다. 기술적 능력이나 편집 경험이 필요하지 않습니다.",
    },
    {
      icon: Lightbulb,
      title: "무한한 가능성",
      description: "소셜 미디어, 교육, 마케팅, 엔터테인먼트 또는 개인적인 스토리텔링을 위한 영상을 제작하세요.",
    },
  ];

  return (
    <Section>
      <SectionHeader
        title="쇼쇼숓?"
        description="쇼쇼숓은 몇 번의 클릭만으로 작성된 이야기를 매력적인 영상으로 변환하는 AI 기반 플랫폼입니다."
      />

      <div className="mt-16 grid gap-8 md:grid-cols-2 lg:grid-cols-3">
        {features.map((feature, index) => (
          <FeatureCard
            key={index}
            icon={feature.icon}
            title={feature.title}
            description={feature.description}
          />
        ))}
      </div>
    </Section>
  );
};