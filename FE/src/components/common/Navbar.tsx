import { Link } from "react-router-dom";
import shortLogo from "@/assets/short_logo.png";

interface NavbarProps {
  showCreateButton?: boolean;
}

export function Navbar({ showCreateButton = false }: NavbarProps) {
  return (
    <header className="sticky top-0 z-10 bg-white border-b">
      <div className="container flex h-16 items-center justify-between px-4">
        <div className="flex items-center gap-2">
          <Link to="/" className="flex items-center gap-2">
            <img src={shortLogo} alt="쇼쇼숏 로고" className="h-8 w-auto" />
          </Link>
        </div>
        {showCreateButton && (
          <div className="flex items-center gap-4">
            <Link to="/create">
              <button className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-md">
                동영상 만들기
              </button>
            </Link>
          </div>
        )}
      </div>
    </header>
  );
} 