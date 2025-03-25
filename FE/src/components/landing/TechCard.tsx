import React from "react";
import { CheckCircle2, LucideIcon } from "lucide-react";

interface TechCardProps {
  icon: LucideIcon;
  title: string;
  description: string;
  features: string[];
}

export const TechCard: React.FC<TechCardProps> = ({ icon: Icon, title, description, features }) => {
  return (
    <div className="rounded-xl border bg-white p-6 shadow-sm">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-red-100 text-red-600">
        <Icon className="h-6 w-6" />
      </div>
      <h3 className="mt-4 text-xl font-semibold">{title}</h3>
      <p className="mt-2 text-gray-600">{description}</p>
      <ul className="mt-4 space-y-2 text-gray-600">
        {features.map((feature, index) => (
          <li key={index} className="flex items-start gap-2">
            <CheckCircle2 className="mt-0.5 h-4 w-4 text-green-500" />
            <span>{feature}</span>
          </li>
        ))}
      </ul>
    </div>
  );
};