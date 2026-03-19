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
    if (error.response?.status === 401 || error.response?.status === 403) {
      // Hết hạn token hoặc không có quyền truy cập -> Logout
      useCookie('auth_token').value = null;
      useCookie('user_data').value = null;
      if (process.client) {
        // Đảm bảo cookie bị xóa
        document.cookie = "auth_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        document.cookie = "user_data=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
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
