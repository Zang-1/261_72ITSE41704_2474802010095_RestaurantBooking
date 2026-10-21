import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server forwards /api to Spring Boot, so the browser sees one origin
// and the SameSite=Strict session cookie works.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
