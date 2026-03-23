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
  }, async (error) => {
    const originalRequest = error.config;

    // Nếu lỗi 401 và chưa thử refresh lần nào
    if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = useCookie('auth_refresh_token').value;

      if (refreshToken) {
        try {
          // Gọi API refresh token
          const response = await axios.post(`${config.public.apiBase}/auth/refresh`, {
            refreshToken: refreshToken
          });

          if (response.data.success) {
            const { token, refreshToken: newRefreshToken } = response.data.data;

            // Cập nhật cookie mới
            useCookie('auth_token').value = token;
            if (newRefreshToken) {
              useCookie('auth_refresh_token').value = newRefreshToken;
            }

            // Cập nhật header và thực hiện lại request cũ
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          }
        } catch (refreshError) {
          // Refresh thất bại -> Logout
          console.error('Refresh token failed:', refreshError);
        }
      }

      // Logout nếu không có refresh token hoặc refresh thất bại
      handleLogout();
    }

    return Promise.reject(error.response?.data || error.message);
  });

  function handleLogout() {
    useCookie('auth_token').value = null;
    useCookie('auth_refresh_token').value = null;
    useCookie('user_data').value = null;

    if (process.client) {
      document.cookie = "auth_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
      document.cookie = "auth_refresh_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
      document.cookie = "user_data=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";

      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
  }

  return {
    provide: {
      api: api
    }
  };
});
