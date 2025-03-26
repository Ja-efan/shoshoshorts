import React from "react";
import { useRoutes } from "react-router-dom";
import Layout from "../components/layout";
// import Home from "../pages/Home";
// import About from "../pages/About";
import NotFound from "../pages/notFound";
import LandingPage from "@/pages/LandingPage";
import CreateVideoPage from "@/pages/createPage";

const Router: React.FC = () => {
  return useRoutes([
    {
      path: "/",
      element: <Layout />,
      children: [
        {path: '/', element: <LandingPage /> },
      ],
    },
    {path: '/create', element: <CreateVideoPage/>},
    { path: "*", element: <NotFound /> }, 
  ]);
};

export default Router;
