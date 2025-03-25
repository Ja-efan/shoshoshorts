import React from "react";
import { Layout } from "@/components/common/Layout";
import { HeroSection } from "@/components/landing/HeroSection";
import { WhatIsSection } from "@/components/landing/WhatIsSection";
import { HowItWorksSection } from "@/components/landing/HowItWorksSection";
import { TechnologySection } from "@/components/landing/TechnologySection";
import { UseCasesSection } from "@/components/landing/UseCasesSection";
import { TestimonialsSection } from "@/components/landing/TestimonialsSection";
import { FAQSection } from "@/components/landing/FAQSection";
import { CTASection } from "@/components/landing/CTASection";

export default function LandingPage() {
  return (
    <Layout>
      <HeroSection />
      <WhatIsSection />
      <HowItWorksSection />
      <TechnologySection />
      <UseCasesSection />
      <TestimonialsSection />
      <FAQSection />
      <CTASection />
    </Layout>
  );
}