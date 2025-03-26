import React from "react";
import { useRoutes } from "react-router-dom";
import Layout from "../components/layout";
import NotFound from "../pages/notFound";
import LandingPage from "@/pages/LandingPage";
import CreateVideoPage from "@/pages/createPage";
import DashboardPage from "@/pages/dashboardPage";

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
    {path: '/dashboard', element: <DashboardPage/>},
    { path: "*", element: <NotFound /> }, 
  ]);
};

export default Router;
