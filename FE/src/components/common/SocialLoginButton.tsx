import { Button } from "@/components/ui/button";

interface SocialLoginButtonProps {
  icon: string;
  text: string;
  bgColor: string;
  onClick: () => void;
}

export const SocialLoginButton: React.FC<SocialLoginButtonProps> = ({
  icon,
  text,
  bgColor,
  onClick,
}) => {
  return (
    <Button
      onClick={onClick}
      className={`flex items-center justify-center gap-2 ${bgColor} hover:opacity-90 text-base h-12`}
    >
      <img src={icon} alt={text} className="h-5 w-5" />
      {text}
    </Button>
  );
}; 