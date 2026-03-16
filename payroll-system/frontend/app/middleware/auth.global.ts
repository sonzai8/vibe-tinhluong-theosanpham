export default defineNuxtRouteMiddleware((to, from) => {
  const { isLoggedIn, hasPermission } = useAuth();

  // Danh sách các trang không cần login
  const publicPages = ['/login', '/register'];
  const isPublicPage = publicPages.includes(to.path);

  if (!isLoggedIn.value && !isPublicPage) {
    return navigateTo('/login');
  }

  if (isLoggedIn.value && isPublicPage) {
    return navigateTo('/');
  }

  // Kiểm tra quyền truy cập route
  if (isLoggedIn.value) {
    const routePermissions: Record<string, string> = {
      '/employees': 'EMPLOYEE_VIEW',
      '/departments': 'SYSTEM_ADMIN',
      '/teams': 'SYSTEM_ADMIN',
      '/roles': 'SYSTEM_ADMIN',
      '/products': 'PRODUCTION_VIEW',
      '/production-records': 'PRODUCTION_VIEW',
      '/attendances': 'ATTENDANCE_VIEW',
      '/payrolls': 'PAYROLL_VIEW',
      '/penalty-bonus': 'PAYROLL_VIEW',
      '/product-qualities': 'SYSTEM_ADMIN',
      '/quality-layers': 'SYSTEM_ADMIN',
      '/production-steps': 'SYSTEM_ADMIN',
      '/settings': 'SYSTEM_ADMIN'
    };

    const requiredPermission = routePermissions[to.path];
    if (requiredPermission && !hasPermission(requiredPermission)) {
      console.warn(`Access denied for ${to.path}. Required: ${requiredPermission}`);
      return navigateTo('/');
    }
  }
});
