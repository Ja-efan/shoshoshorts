// src/components/common/Logo.tsx
import React from "react";
import { Video } from "lucide-react";

interface LogoProps {
  size?: "sm" | "md" | "lg";
}

export const Logo: React.FC<LogoProps> = ({ size = "md" }) => {
  const sizes = {
    sm: {
      icon: "h-6 w-6",
      text: "text-lg",
    },
    md: {
      icon: "h-8 w-8",
      text: "text-xl",
    },
    lg: {
      icon: "h-10 w-10",
      text: "text-2xl",
    },
  };

  return (
    <div className="flex items-center gap-2">
      <Video className={`${sizes[size].icon} text-red-600`} />
      <span className={`${sizes[size].text} font-bold`}>ShoShoShort</span>
    </div>
  );
};