import { defineConfig } from 'vite'
import path from 'path';

export default defineConfig({
  publicDir: path.resolve(__dirname, '../public'),
  resolve: {
    alias: {
      '@common': path.resolve(__dirname, '../common'),     
    },
  },
  server: { port: 5176 },
  base: '/connect4/'
});