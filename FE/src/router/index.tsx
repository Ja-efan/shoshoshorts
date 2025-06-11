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
import YoutubeLoginPage from "@/pages/upload/youtube/login";
import YoutubeUploadPage from "@/pages/upload/youtube/upload";

const Router: React.FC = () => {
  return useRoutes([
    {
      path: "/",
      element: <Layout />,
      children: [{ path: "/", element: <LandingPage /> }],
    },
    { path: "/login", element: <LoginPage /> },
    { path: "/terms", element: <TermsPage /> },
    // {
    //   path: "/mypage/debug",
    //   element: <Mypage />,
    // },
    {
      path: "/mypage",
      element: (
        <ProtectedRoute>
          <Mypage />
        </ProtectedRoute>
      ),
    },
    // {
    //   path: "/dashboard/debug",
    //   element: <DashboardPage />,
    // },
    {
      path: "/dashboard/",
      element: (
        <ProtectedRoute>
          <DashboardPage />
        </ProtectedRoute>
      ),
    },
    // {
    //   path: "/create/debug",
    //   element: <CreateVideoPage />,
    // },
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
    {
      path: "/upload/youtube/login",
      element: (
        <ProtectedRoute>
          <YoutubeLoginPage />
        </ProtectedRoute>
      ),
    },
    {
      path: "/youtube-upload",
      element: (
        <ProtectedRoute>
          <YoutubeUploadPage />
        </ProtectedRoute>
      ),
    },
    { path: "*", element: <NotFound /> },
  ]);
};

export default Router;
