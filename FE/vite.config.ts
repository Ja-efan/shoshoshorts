import path from "path"
import tailwindcss from "@tailwindcss/vite"
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
// import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
    server: {
      proxy: {  // npm run dev 시에만 아래 프록시 설정이 적용됨
        '/api': {
          target: 'http://sss-backend-dev:8080',
          changeOrigin: true,
          secure: false,
        },
      },
    },
});
