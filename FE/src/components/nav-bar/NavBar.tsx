import { useState, useEffect } from 'react';
import { UserCircle2, Menu } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link, useNavigate } from 'react-router-dom';
import { Link as ScrollLink } from 'react-scroll';
import shortLogo from "@/assets/short_logo.png"
import { apiService } from "@/api/api";

const navigationItems = [
  { to: "hero", label: "홈" },
  { to: "how-it-works", label: "사용방법" },
  { to: "technology", label: "기술" },
  { to: "use-cases", label: "활용사례" },
  { to: "faq", label: "FAQ" },
];

const Navbar = () => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const token = localStorage.getItem("accessToken");
      console.log("현재 토큰:", token);
      
      if (!token) {
        console.log("토큰이 없음 - 비인증 상태로 설정");
        setIsAuthenticated(false);
        return;
      }

      const isValid = await apiService.validateToken();
      console.log("토큰 검증 결과:", isValid);
      setIsAuthenticated(isValid);
    } catch (error) {
      console.error('인증 상태 확인 실패:', error);
      setIsAuthenticated(false);
    }
  };

  const handleAuthClick = () => {
    console.log("인증 상태:", isAuthenticated);
    if (isAuthenticated) {
      navigate('/mypage');
    } else {
      console.log("로그인 페이지로 이동");
      navigate('/login');
    }
  };

  return (
    <nav className="bg-white shadow-md fixed w-full top-0 z-50">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          {/* 로고 */}
          <div className="flex-shrink-0">
            <Link to="/" className="flex items-center gap-2">
              <img src={shortLogo} alt="쇼쇼숓 로고" className="h-8 w-auto" />
            </Link>
          </div>

          {/* 데스크톱 메뉴 */}
          <div className="hidden md:flex items-center justify-center flex-1 space-x-8">
            {navigationItems.map((item) => (
              <ScrollLink
                key={item.to}
                to={item.to}
                spy={true}
                smooth={true}
                offset={-64}
                duration={500}
                activeClass="text-red-600"
                className="text-gray-700 hover:text-red-600 transition-colors cursor-pointer"
              >
                {item.label}
              </ScrollLink>
            ))}
          </div>

          {/* 로그인/마이페이지 아이콘 */}
          <div className="hidden md:flex items-center">
            <button
              onClick={handleAuthClick}
              className="p-2 hover:bg-gray-100 rounded-full text-gray-700 hover:text-red-600 transition-colors"
              title={isAuthenticated ? "마이페이지" : "로그인"}
            >
              <UserCircle2 className="w-6 h-6" />
            </button>
          </div>

          {/* 모바일 메뉴 버튼 */}
          <div className="md:hidden flex items-center">
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="p-2 hover:bg-gray-100 rounded-full"
            >
              <Menu className="w-6 h-6" />
            </button>
          </div>
        </div>
      </div>

      {/* 모바일 메뉴 */}
      <AnimatePresence>
        {isMobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="md:hidden bg-white border-t absolute top-16 left-0 w-full"
          >
            <div className="px-4 py-2 space-y-1">
              {navigationItems.map((item) => (
                <ScrollLink
                  key={item.to}
                  to={item.to}
                  spy={true}
                  smooth={true}
                  offset={-64}
                  duration={500}
                  className="block px-3 py-2 rounded-md text-gray-700 hover:bg-gray-100 cursor-pointer"
                  onClick={() => setIsMobileMenuOpen(false)}
                >
                  {item.label}
                </ScrollLink>
              ))}
              
              {/* 모바일 로그인/마이페이지 버튼 */}
              <button
                onClick={() => {
                  handleAuthClick();
                  setIsMobileMenuOpen(false);
                }}
                className="w-full mt-2 bg-red-600 text-white py-2 px-4 rounded-full hover:bg-red-700 transition-colors flex items-center justify-center gap-2"
              >
                <UserCircle2 className="w-5 h-5" />
                {isAuthenticated ? "마이페이지" : "로그인"}
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </nav>
  );
};

export default Navbar;