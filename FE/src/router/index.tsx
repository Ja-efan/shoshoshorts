import React from "react";
import { useRoutes } from "react-router-dom";
import Layout from "../components/layout";
// import Home from "../pages/Home";
// import About from "../pages/About";
import NotFound from "../pages/notFound";
import LandingPage from "@/pages/LandingPage";

const Router: React.FC = () => {
  return useRoutes([
    {
      path: "/",
      element: <Layout />,
      children: [
        {path: '/', element: <LandingPage /> },
        {path: "*", element: <NotFound /> }
      ],
    },
    // { path: "/login", element: <Login /> },
    { path: "*", element: <NotFound /> },  // 404 페이지 (Navbar 없음)
  ]);
};

export default Router;
