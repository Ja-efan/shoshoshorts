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
        <ProtectedRoute>
          <CreateVideoPage />
        </ProtectedRoute>
      ),
    },
    {
      path: '/dashboard',
      element: (
        <ProtectedRoute>
          <DashboardPage />
        </ProtectedRoute>
      ),
    },
    { path: "*", element: <NotFound /> },
  ]);
};

export const router = createBrowserRouter([
  {
    path: "/",
    element: <ProtectedRoute><DashboardPage /></ProtectedRoute>,
  },
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/create",
    element: <ProtectedRoute><CreateVideoPage /></ProtectedRoute>,
  },
  {
    path: "/terms",
    element: <TermsPage />,
  },
  {
    path: "/auth/:provider/callback",
    element: <AuthCallbackPage />,
  },
]);

export default Router;
