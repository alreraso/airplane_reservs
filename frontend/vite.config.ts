import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// In development the browser talks to Vite (http://localhost:5173) and Vite forwards
// every /api request, including the SSE stream, to Spring Boot. Same origin, so no CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
