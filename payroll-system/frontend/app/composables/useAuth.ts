export const useAuth = () => {
  const token = useCookie<string | null>('auth_token');
  const user = useCookie<any | null>('user_data');
  const router = useRouter();

  const isLoggedIn = computed(() => !!token.value);

  const login = async (loginData: any) => {
    const { $api } = useNuxtApp();
    try {
      const response: any = await $api.post('/auth/login', loginData);
      // Backend trả về map chứa token, employeeId, fullName trực tiếp
      if (response && response.data.token) {
        token.value = response.data.token;
        user.value = {
          id: response.data.employeeId,
          fullName: response.data.fullName,
          role: response.data.roleName,
          permissions: response.data.permissions || []
        };
        router.push('/');
        return { success: true };
      }
      return { success: false, message: 'Dữ liệu phản hồi không hợp lệ' };
    } catch (error: any) {
      console.log(error)
      const msg = error.response?._data?.message || error.message || 'Đăng nhập thất bại';
      return { success: false, message: msg };
    }
  };

  const register = async (registerData: any) => {
    const { $api } = useNuxtApp();
    try {
      const response: any = await $api.post('/auth/register', registerData);
      return response;
    } catch (error: any) {
      throw error;
    }
  };

  const logout = () => {
    token.value = null;
    user.value = null;
    router.push('/login');
  };

  const hasPermission = (permission: string) => {
    if (!user.value || !user.value.permissions) return false;
    // SYSTEM_ADMIN có tất cả các quyền
    if (user.value.permissions.includes('SYSTEM_ADMIN')) return true;
    return user.value.permissions.includes(permission);
  };

  return {
    token,
    user,
    isLoggedIn,
    login,
    register,
    logout,
    hasPermission
  };
}
