export default defineNuxtRouteMiddleware((to, from) => {
  const { isLoggedIn } = useAuth();

  // Danh sách các trang không cần login
  const publicPages = ['/login', '/register'];
  const isPublicPage = publicPages.includes(to.path);

  if (!isLoggedIn.value && !isPublicPage) {
    return navigateTo('/login');
  }

  if (isLoggedIn.value && isPublicPage) {
    return navigateTo('/');
  }
});
