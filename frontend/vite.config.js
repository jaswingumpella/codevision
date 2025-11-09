import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/analyze': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/project': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/setupTests.js',
    globals: true,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      statements: 0.9,
      branches: 0.9,
      functions: 0.9,
      lines: 0.9,
      include: ['src/**/*.{js,jsx}']
    }
  }
});
