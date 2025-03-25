import React, { ReactNode } from "react";

import { Footer } from "./Footer";

interface LayoutProps {
  children: ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  return (
    <div className="flex min-h-screen flex-col">
      <main className="flex-1 mx-auto w-full max-w-7xl whitespace-normal break-words break-keep">{children}</main>
      <Footer />
    </div>
  );
};