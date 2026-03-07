import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/seckill': { target: 'http://localhost:8081', changeOrigin: true },
      '/admin': { target: 'http://localhost:8081', changeOrigin: true },
      // 新增统一认证接口代理
      '/auth': { target: 'http://localhost:8081', changeOrigin: true }
    }
  }
})