import React from "react";
import { useRoutes, createBrowserRouter } from "react-router-dom";
import Layout from "../components/layout";
import NotFound from "../pages/notFound";
import LandingPage from "@/pages/landingPage";
import CreateVideoPage from "@/pages/createPage";
import DashboardPage from "@/pages/dashboardPage";
import LoginPage from "@/pages/loginPage";
import TermsPage from "@/pages/terms";
import { ProtectedRoute } from "@/components/common/ProtectedRoute";
import AuthCallbackPage from "@/pages/auth/callback";

const Router: React.FC = () => {
  return useRoutes([
    {
      path: "/",
      element: <Layout />,
      children: [
        { path: '/', element: <LandingPage /> },
      ],
    },
    { path: '/login', element: <LoginPage /> },
    { path: '/terms', element: <TermsPage /> },
    {
      path: '/create',
      element: (
          <CreateVideoPage />
      ),
    },
    {
      path: '/dashboard',
      element: (
          <DashboardPage />
      ),
    },
    {
      path: "/dashboard/debug",
      element: <ProtectedRoute><DashboardPage /></ProtectedRoute>,
    },
    {
      path: "/create/debug",
      element: <ProtectedRoute><CreateVideoPage /></ProtectedRoute>,
    },
    {
      path: "/auth/:provider/callback",
      element: <AuthCallbackPage />,
    },
    { path: "*", element: <NotFound /> },
  ]);
};

export default Router;
