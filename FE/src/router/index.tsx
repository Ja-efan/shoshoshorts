import React from "react";
import { useRoutes } from "react-router-dom";
import Layout from "../components/layout";
import NotFound from "../pages/notFound";
import LandingPage from "@/pages/LandingPage";
import CreateVideoPage from "@/pages/createPage";
import DashboardPage from "@/pages/dashboardPage";
import LoginPage from "@/pages/loginPage";
import TermsPage from "@/pages/terms";
import { ProtectedRoute } from "@/components/common/ProtectedRoute";
import AuthCallbackPage from "@/pages/auth/callback";
import Mypage from "../pages/Mypage/Mypage";

const Router: React.FC = () => {
  return useRoutes([
    {
      path: "/",
      element: <Layout />,
      children: [{ path: "/", element: <LandingPage /> }],
    },
    { path: "/login", element: <LoginPage /> },
    { path: "/terms", element: <TermsPage /> },
    {
      path: "/mypage",
      element: (
        <ProtectedRoute>
          <Mypage />
        </ProtectedRoute>
      ),
    },
    {
      path: "/dashboard/",
      element: (
        <ProtectedRoute>
          <DashboardPage />
        </ProtectedRoute>
      ),
    },
    {
      path: "/create/",
      element: (
        <ProtectedRoute>
          <CreateVideoPage />
        </ProtectedRoute>
      ),
    },
    {
      path: "/auth/:provider/callback",
      element: <AuthCallbackPage />,
    },
    { path: "*", element: <NotFound /> },
  ]);
};

export default Router;
