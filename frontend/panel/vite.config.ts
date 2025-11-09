import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

import path from 'path';
export default defineConfig({
  plugins: [react()],
  publicDir: path.resolve(__dirname, '../public'),
  resolve: {
    alias: {
      '@common': path.resolve(__dirname, '../common'),     
    },
  },
  server: { port: 5174 },
  base: '/panel/'
});