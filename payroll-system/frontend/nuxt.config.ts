import { fileURLToPath } from 'node:url'

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },
  modules: ['@nuxtjs/tailwindcss', '@nuxtjs/i18n'],

  i18n: {
    locales: [
      { code: 'vi', iso: 'vi-VN', file: 'vi.json', name: 'Tiếng Việt' },
      { code: 'zh', iso: 'zh-CN', file: 'zh.json', name: '中文' },
      { code: 'en', iso: 'en-US', file: 'en.json', name: 'English' }
    ],
    defaultLocale: 'vi',
    langDir: 'locales',
    strategy: 'no_prefix'
  },
  
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