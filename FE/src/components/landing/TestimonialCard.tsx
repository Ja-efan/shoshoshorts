import React from "react";
import { Star } from "lucide-react";

interface TestimonialCardProps {
  name: string;
  role: string;
  quote: string;
}

export const TestimonialCard: React.FC<TestimonialCardProps> = ({ name, role, quote }) => {
  return (
    <div className="rounded-xl border bg-white p-6 shadow-sm">
      <div className="flex gap-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <Star key={star} className="h-5 w-5 fill-yellow-400 text-yellow-400" />
        ))}
      </div>
      <blockquote className="mt-4">
        <p className="text-gray-600">"{quote}"</p>
      </blockquote>
      <div className="mt-4 flex items-center gap-3">
        <div className="h-10 w-10 overflow-hidden rounded-full bg-gray-200">
          {/* Placeholder for user avatar */}
        </div>
        <div>
          <p className="font-semibold">{name}</p>
          <p className="text-sm text-gray-500">{role}</p>
        </div>
      </div>
    </div>
  );
};