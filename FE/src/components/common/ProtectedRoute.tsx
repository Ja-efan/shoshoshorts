import { Navigate, useLocation } from "react-router-dom";
import { authService } from "@/lib/auth";
import { toast } from "react-hot-toast";
import { useEffect, useRef } from "react";

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const location = useLocation();
  const isAuthenticated = authService.isAuthenticated();
  const toastShown = useRef(false);

  useEffect(() => {
    if (!isAuthenticated && !toastShown.current) {
      toast.error("로그인이 필요한 서비스입니다.");
      toastShown.current = true;
    }
  }, [isAuthenticated]);

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}; 