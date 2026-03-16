<template>
  <NuxtLayout name="auth">
    <div class="card w-full max-w-md p-8 sm:p-10 space-y-8 shadow-2xl shadow-primary-50">
      <div class="text-center space-y-2">
        <h1 class="text-3xl font-black text-slate-900 tracking-tight">Tạo tài khoản mới</h1>
        <p class="text-slate-500 font-medium">Đăng ký thành viên quản trị hệ thống</p>
      </div>

      <form @submit.prevent="handleRegister" class="space-y-6">
        <div class="space-y-4">
          <UiInput v-model="form.code" label="Mã nhân viên" placeholder="VD: NV001" required />
          <UiInput v-model="form.username" label="Tên đăng nhập" placeholder="VD: admin_plywood" required />
          <UiInput v-model="form.password" label="Mật khẩu" type="password" placeholder="••••••••" required />
          <UiInput v-model="form.fullName" label="Họ và tên" placeholder="VD: Nguyễn Văn Quản Lý" required />
        </div>

        <div v-if="error" class="p-3 bg-red-50 border border-red-100 text-red-600 text-sm rounded-lg">
          {{ error }}
        </div>
        <div v-if="success" class="p-3 bg-emerald-50 border border-emerald-100 text-emerald-600 text-sm rounded-lg font-bold">
          {{ success }}
        </div>

        <UiButton type="submit" :loading="loading" class="w-full h-12 text-lg font-black">
          Đăng ký ngay
        </UiButton>
      </form>

      <div class="text-center pt-2 border-t border-slate-100">
        <p class="text-slate-500 text-sm font-medium">
          Đã có tài khoản? 
          <NuxtLink to="/login" class="text-primary-600 hover:text-primary-700 font-black hover:underline underline-offset-4">Đăng nhập tại đây</NuxtLink>
        </p>
      </div>
    </div>
  </NuxtLayout>
</template>

<script setup>
definePageMeta({
  layout: false
});

const { register } = useAuth();
const loading = ref(false);
const error = ref('');
const success = ref('');

const form = reactive({
  code: '',
  username: '',
  password: '',
  fullName: ''
});

const handleRegister = async () => {
  loading.value = true;
  error.value = '';
  success.value = '';
  
  try {
    const res = await register(form);
    if (res.success) {
      success.value = 'Đăng ký thành công! Đang chuyển hướng...';
      setTimeout(() => navigateTo('/login'), 2000);
    } else {
      error.value = res.message;
    }
  } catch (err) {
    error.value = err.message || 'Lỗi đăng ký';
  } finally {
    loading.value = false;
  }
};
</script>
