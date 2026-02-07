import path from 'path';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  // Load env from the root config directory
  const env = loadEnv(mode, path.resolve(__dirname, '../../config'), '');

  return {
    envDir: '../../config',
    server: {
      port: 5001,
      host: '0.0.0.0',
      proxy: {
        '/v1': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        }
      },
    },
    plugins: [react()],
    define: {
      'process.env.API_KEY': JSON.stringify(env.GEMINI_API_KEY),
      'process.env.GEMINI_API_KEY': JSON.stringify(env.GEMINI_API_KEY)
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      }
    }
  };
});
