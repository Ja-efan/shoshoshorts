import { useState } from 'react';
import { Search, Menu } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Link as ScrollLink } from 'react-scroll';

const navigationItems = [
  { to: "hero", label: "홈" },
  { to: "how-it-works", label: "사용방법" },
  { to: "technology", label: "기술" },
  { to: "use-cases", label: "활용사례" },
  { to: "faq", label: "FAQ" },
];

const Navbar = () => {
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      toast.success(`"${searchQuery}" 검색 결과는 준비 중입니다!`);
      setSearchQuery('');
      setIsSearchOpen(false);
    } else {
      toast.error('검색어를 입력해주세요.');
    }
  };

  return (
    <nav className="bg-white shadow-md fixed w-full top-0 z-50">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          {/* 로고 */}
          <div className="flex-shrink-0">
            <Link to="/" className="text-xl font-bold hover:text-red-600 transition-colors">
              쇼쇼숓
            </Link>
          </div>

          {/* 데스크톱 메뉴 */}
          <div className="hidden md:flex items-center space-x-6">
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

          {/* 검색창 */}
          <div className="hidden md:flex items-center">
            <button
              onClick={() => setIsSearchOpen(!isSearchOpen)}
              className="p-2 hover:bg-gray-100 rounded-full"
            >
              <Search className="w-5 h-5" />
            </button>
            <AnimatePresence>
              {isSearchOpen ? (
                <motion.form
                  onSubmit={handleSearch}
                  initial={{ width: 0, opacity: 0 }}
                  animate={{ width: 200, opacity: 1 }}
                  exit={{ width: 0, opacity: 0 }}
                  className="relative ml-2"
                >
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="검색어를 입력하세요..."
                    className="w-full px-4 py-1 border rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </motion.form>
              ) : null}
            </AnimatePresence>
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
              
              {/* 모바일 검색창 */}
              <form onSubmit={handleSearch} className="px-3 py-2 space-y-2">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="검색어를 입력하세요..."
                  className="w-full px-4 py-2 border rounded-full focus:outline-none focus:ring-2 focus:ring-red-500"
                />
                <button
                  type="submit"
                  className="w-full bg-red-600 text-white py-2 px-4 rounded-full hover:bg-red-700 transition-colors"
                >
                  검색하기
                </button>
              </form>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </nav>
  );
};

export default Navbar;