import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Desarrollo: Vite en :5173 con proxy hacia el backend local :8080.
// Producción: el build se copia a backend/src/main/resources/static/.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
  },
});
