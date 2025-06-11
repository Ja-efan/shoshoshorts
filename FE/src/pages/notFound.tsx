import { Link } from "react-router-dom";

function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center h-screen text-center">
      <h1 className="text-6xl font-bold text-gray-800">404</h1>
      <p className="text-xl text-gray-600 mt-4">페이지를 찾을 수 없습니다.</p>
      <Link to="/" className="mt-6 px-6 py-2 text-white bg-blue-500 rounded-lg hover:bg-blue-600">
        홈으로 돌아가기
      </Link>
    </div>
  );
}

export default NotFound;