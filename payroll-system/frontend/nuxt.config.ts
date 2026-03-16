import { fileURLToPath } from 'node:url'

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },
  modules: ['@nuxtjs/tailwindcss'],
  
  // Sử dụng đường dẫn tuyệt đối để tránh lỗi resolve alias khi thư mục cha có dấu cách
  css: [
    fileURLToPath(new URL('./assets/css/main.css', import.meta.url))
  ],

  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080/api'
    }
  }
})
