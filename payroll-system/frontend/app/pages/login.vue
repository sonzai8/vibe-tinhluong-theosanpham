<template>
  <NuxtLayout name="auth">
    <div class="card w-full max-w-md p-8 sm:p-10 space-y-8">
      <div class="text-center space-y-2">
        <div class="mx-auto w-16 h-16 bg-primary-600 rounded-2xl flex items-center justify-center text-white shadow-xl shadow-primary-200 mb-6 group hover:rotate-6 transition-transform">
          <Layers class="w-10 h-10" />
        </div>
        <h1 class="text-3xl font-black text-slate-900 tracking-tight">Chào mừng trở lại</h1>
        <p class="text-slate-500 font-medium">Hệ thống quản lý lương Plywood</p>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-6">
        <div class="space-y-4">
          <UiInput
            v-model="form.username"
            label="Tên đăng nhập"
            placeholder="Nhập username của bạn"
            id="username"
            required
          >
            <template #icon><User class="w-5 h-5" /></template>
          </UiInput>

          <UiInput
            v-model="form.password"
            label="Mật khẩu"
            type="password"
            placeholder="••••••••"
            id="password"
            required
          >
            <template #icon><Lock class="w-5 h-5" /></template>
          </UiInput>
        </div>

        <div v-if="error" class="p-3 bg-red-50 border border-red-100 text-red-600 text-sm rounded-lg flex items-center gap-2 animate-shake">
          <AlertCircle class="w-4 h-4 flex-shrink-0" />
          <span>{{ error }}</span>
        </div>

        <div class="flex items-center justify-between text-sm">
          <label class="flex items-center gap-2 cursor-pointer group">
            <input type="checkbox" class="w-4 h-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500 transition-all" />
            <span class="text-slate-600 group-hover:text-slate-900 font-medium">Ghi nhớ đăng nhập</span>
          </label>
          <a href="#" class="text-primary-600 hover:text-primary-700 font-bold decoration-2 underline-offset-4 hover:underline transition-all">Quên mật khẩu?</a>
        </div>

        <UiButton type="submit" :loading="loading" class="w-full h-12 text-lg font-bold shadow-lg shadow-primary-100">
          Đăng nhập ngay
        </UiButton>
      </form>

      <div class="text-center pt-2">
        <p class="text-slate-500 text-sm font-medium">
          Chưa có tài khoản? 
          <NuxtLink to="/register" class="text-primary-600 hover:text-primary-700 font-bold decoration-2 underline-offset-4 hover:underline transition-all">Đăng ký mới</NuxtLink>
        </p>
      </div>
    </div>
  </NuxtLayout>
</template>

<script setup>
import { Layers, User, Lock, AlertCircle } from 'lucide-vue-next';

definePageMeta({
  layout: false
});

const { login } = useAuth();
const loading = ref(false);
const error = ref('');

const form = reactive({
  username: '',
  password: ''
});

const handleLogin = async () => {
  loading.value = true;
  error.value = '';
  console.log("da click button login")
  try {
    const res = await login(form);
    console.log(res)
    if (!res.success) {
      error.value = res.message;
    }
  } catch (err) {
    error.value = 'Máy chủ không phản hồi. Vui lòng thử lại sau.';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}
.animate-shake {
  animation: shake 0.2s ease-in-out 0s 2;
}
</style>
