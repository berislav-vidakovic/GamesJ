import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Serve both sudoku/public and frontend/public
  publicDir: path.resolve(__dirname, '../public'),
  resolve: {
    alias: {
      '@common': path.resolve(__dirname, '../common'),     
    },
  },
  server: { port: 5175 },
  base: '/sudoku/'
});
