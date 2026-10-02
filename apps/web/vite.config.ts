import path from "node:path";
import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [
    tanstackRouter({
      target: "react",
      routesDirectory: "./src/pages",
      autoCodeSplitting: true,
    }),
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: [
      {
        find: "@components",
        replacement: path.resolve(__dirname, "./src/components"),
      },
      {
        find: "@ui",
        replacement: path.resolve(__dirname, "./src/components/ui"),
      },
      {
        find: "@animate",
        replacement: path.resolve(__dirname, "./src/components/ui/animate"),
      },
      { find: "@assets", replacement: path.resolve(__dirname, "./src/assets") },
      { find: "@stores", replacement: path.resolve(__dirname, "./src/stores") },
      { find: "@pages", replacement: path.resolve(__dirname, "./src/pages") },
      { find: "@lib", replacement: path.resolve(__dirname, "./src/lib") },
      { find: "@hooks", replacement: path.resolve(__dirname, "./src/hooks") },
      {
        find: "@app",
        replacement: path.resolve(__dirname, "./src/pages/_app"),
      },
      { find: "@@types", replacement: path.resolve(__dirname, "./src/types") },
      { find: /^@\//, replacement: `${path.resolve(__dirname, "./src")}/` },
    ],
  },
  server: {
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
});
