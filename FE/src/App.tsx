import { Toaster } from "react-hot-toast";
import Router from "./router";
function App() {
  return (
    <div className="">
      <Router />
      <Toaster position="top-center" />
    </div>
  );
}

export default App;