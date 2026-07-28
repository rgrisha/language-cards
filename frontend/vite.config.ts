import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  // Lets the app be served under a reverse-proxy path prefix (e.g. /language-cards/)
  // instead of the domain root. Set at build time via VITE_BASE_PATH; defaults to "/".
  base: process.env.VITE_BASE_PATH || "/",
  plugins: [react()],
  server: {
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
});
