import React from "react";
import { Section, SectionHeader } from "@/components/common/Section";
import { TestimonialCard } from "./TestimonialCard";
import { testimonials } from "@/lib/constants/testimonials";

export const TestimonialsSection: React.FC = () => {
  return (
    <Section background="gray">
      <SectionHeader
        title="사용자 후기"
        description="수많은 만족한 크리에이터들과 함께하세요"
      />

      <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {testimonials.map((testimonial, index) => (
          <TestimonialCard
            key={index}
            name={testimonial.name}
            role={testimonial.role}
            quote={testimonial.quote}
          />
        ))}
      </div>
    </Section>
  );
}