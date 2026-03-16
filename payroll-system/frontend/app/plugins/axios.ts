import axios from 'axios';

export default defineNuxtPlugin((nuxtApp) => {
  const config = useRuntimeConfig();
  
  const api = axios.create({
    baseURL: config.public.apiBase,
    headers: {
      'Content-Type': 'application/json',
    }
  });

  // Request Interceptor: Thêm token nếu có
  api.interceptors.request.use((config) => {
    const token = useCookie('auth_token').value;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  }, (error) => {
    return Promise.reject(error);
  });

  // Response Interceptor: Xử lý lỗi tập trung
  api.interceptors.response.use((response) => {
    return response.data; // Trả về ApiResponse JSON chuẩn của backend
  }, (error) => {
    if (error.response?.status === 401) {
      // Hết hạn token hoặc không hợp lệ -> Logout
      useCookie('auth_token').value = null;
      useCookie('user_data').value = null;
      if (process.client) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error.response?.data || error.message);
  });

  return {
    provide: {
      api: api
    }
  };
});
