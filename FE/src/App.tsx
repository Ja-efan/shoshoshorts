import { Routes, Route, Link } from "react-router-dom";

import NotFound from "./pages/notFound";

function App() {
  return (
    <div className="p-4">
      hello world
      <nav className="flex gap-4 mb-4">
        <Link to="/" className="text-blue-500">Home</Link>
        <Link to="/about" className="text-blue-500">About</Link>
      </nav>

      <Routes>
        <Route path="*" element={<NotFound />} />
      </Routes>
    </div>
  );
}

export default App;