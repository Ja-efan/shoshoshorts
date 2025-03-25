// src/components/common/Section.tsx
import React, { ReactNode } from "react";

interface SectionProps {
  children: ReactNode;
  className?: string;
  background?: "white" | "gray" | "gradient";
  container?: boolean;
}

export const Section: React.FC<SectionProps> = ({
  children,
  className = "",
  background = "white",
  container = true,
}) => {
  const backgrounds = {
    white: "bg-white",
    gray: "bg-gray-50",
    gradient: "bg-gradient-to-br from-red-50 via-white to-red-50",
  };

  return (
    <section className={`py-20 ${backgrounds[background]} ${className}`}>
      {container ? <div className="container px-4">{children}</div> : children}
    </section>
  );
};

interface SectionHeaderProps {
  title: string;
  description?: string;
}

export const SectionHeader: React.FC<SectionHeaderProps> = ({ title, description }) => {
  return (
    <div className="mx-auto max-w-3xl text-center">
      <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">{title}</h2>
      {description && <p className="mt-4 text-lg text-gray-600">{description}</p>}
    </div>
  );
};