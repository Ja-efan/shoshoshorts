// src/components/common/Logo.tsx
import React from "react";
import shortLogo from "../../assets/short_logo.png";

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
      <img src={shortLogo} alt="쇼쇼숏 로고" className={`${sizes[size].icon}`} />
      <span className={`${sizes[size].text} font-bold`}>쇼쇼숏</span>
    </div>
  );
};