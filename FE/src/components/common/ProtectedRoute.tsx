import { Navigate, useLocation } from "react-router-dom";
import { authService } from "@/lib/auth";
import { toast } from "react-hot-toast";
import { useEffect, useRef, useState } from "react";
import { Loader2 } from "lucide-react";

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const location = useLocation();
  const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);
  const toastShown = useRef(false);

  useEffect(() => {
    const checkAuth = async () => {
      const auth = await authService.isAuthenticated();
      setIsAuthenticated(auth);
      
      if (!auth && !toastShown.current) {
        toast.error("로그인이 필요한 서비스입니다.");
        toastShown.current = true;
      }
    };

    checkAuth();
  }, []);

  // 인증 상태 확인 중에는 로딩 상태를 표시
  if (isAuthenticated === null) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50">
        <div className="text-center space-y-4">
          <Loader2 className="w-12 h-12 animate-spin text-red-600 mx-auto" />
          <p className="text-gray-600 text-lg font-medium">인증 확인 중...</p>
          <p className="text-gray-500 text-sm">잠시만 기다려주세요</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}; 