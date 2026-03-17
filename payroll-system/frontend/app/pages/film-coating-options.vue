<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900 tracking-tight">Tùy chọn Phủ phim</h2>
        <p class="text-slate-500 font-medium">Quản lý các loại phủ phim và mức phụ phí tương ứng</p>
      </div>
    </div>

    <!-- Info Card -->
    <div class="bg-amber-50 border border-amber-100 rounded-2xl p-6 flex items-start gap-4">
      <div class="w-12 h-12 rounded-xl bg-amber-100 flex items-center justify-center text-amber-600 shrink-0">
        <ShieldAlert class="w-6 h-6" />
      </div>
      <div>
        <p class="text-amber-900 font-black tracking-tight">Lưu ý cấu hình</p>
        <p class="text-amber-700/80 text-sm font-medium leading-relaxed">
          Mức phụ phí này sẽ được cộng trực tiếp vào đơn giá sản phẩm khi tính lương nếu sản phẩm đó có thuộc tính phủ phim tương ứng. 
          Giá trị được cấu hình theo từng tháng hiệu lực.
        </p>
      </div>
    </div>

    <!-- Options List -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div v-for="opt in coatingOptions" :key="opt.key" class="card p-8 group hover:border-primary-500 transition-all cursor-pointer relative overflow-hidden">
        <div class="absolute -right-4 -top-4 w-24 h-24 bg-primary-50 rounded-full opacity-0 group-hover:opacity-100 transition-all duration-500 -z-10"></div>
        
        <div class="flex items-center justify-between mb-6">
          <div :class="['w-14 h-14 rounded-2xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110 duration-500', opt.color]">
            <component :is="opt.icon" class="w-7 h-7" />
          </div>
          <div class="text-right">
            <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Trạng thái</p>
            <p class="text-xs font-black text-emerald-600 uppercase">Đang áp dụng</p>
          </div>
        </div>

        <h3 class="text-xl font-black text-slate-900 mb-2 tracking-tight">{{ opt.label }}</h3>
        <p class="text-slate-500 text-sm font-medium mb-6 leading-relaxed">{{ opt.description }}</p>

        <div class="space-y-4 pt-4 border-t border-slate-100">
          <div class="flex items-center justify-between">
            <span class="text-xs font-black text-slate-400 uppercase tracking-widest">Phụ phí hiện tại</span>
            <span class="text-lg font-black text-primary-600">{{ formatMoney(configs[opt.configKey] || 0) }}</span>
          </div>
          <UiButton variant="outline" class="w-full font-bold group-hover:bg-primary-600 group-hover:text-white group-hover:border-primary-600" @click="openEdit(opt)">
            Thay đổi cấu hình
          </UiButton>
        </div>
      </div>
    </div>

    <!-- Edit Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-md p-8 animate-in zoom-in duration-200">
        <div class="flex items-center justify-between mb-8">
          <h3 class="text-xl font-black text-slate-900 tracking-tight">Cấu hình: {{ activeOpt.label }}</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="space-y-6">
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Tháng</label>
              <select v-model="editForm.month" class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 outline-none focus:bg-white focus:border-slate-200 transition-all">
                <option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</option>
              </select>
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Năm</label>
              <select v-model="editForm.year" class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 outline-none focus:bg-white focus:border-slate-200 transition-all">
                <option v-for="y in [2024, 2025, 2026]" :key="y" :value="y">{{ y }}</option>
              </select>
            </div>
          </div>

          <UiInput v-model="editForm.value" type="number" label="Mức phụ phí (VNĐ)" placeholder="Nhập số tiền phụ phí" required />

          <div class="flex gap-3 pt-2">
            <button @click="showModal = false" class="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-600 font-bold hover:bg-slate-50 transition-all text-sm">Hủy bỏ</button>
            <UiButton class="flex-1 font-black" :loading="saving" @click="handleSave">Cập nhật ngay</UiButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ShieldCheck, ShieldAlert, X, Zap, Layers, Minus } from 'lucide-vue-next';
const { $api } = useNuxtApp();

const coatingOptions = [
  { 
    key: 'NONE', 
    label: 'Không phủ phim', 
    configKey: 'FILM_SURCHARGE_NONE', 
    icon: Minus, 
    color: 'bg-slate-100 text-slate-600',
    description: 'Sản phẩm mộc, không có lớp bảo vệ phim.'
  },
  { 
    key: 'SIDE_1', 
    label: 'Phủ phim 1 mặt', 
    configKey: 'FILM_SURCHARGE_1_SIDE', 
    icon: Zap, 
    color: 'bg-blue-100 text-blue-600',
    description: 'Bề mặt được phủ 1 lớp phim phenolic chống thấm.'
  },
  { 
    key: 'SIDE_2', 
    label: 'Phủ phim 2 mặt', 
    configKey: 'FILM_SURCHARGE_2_SIDE', 
    icon: Layers, 
    color: 'bg-indigo-100 text-indigo-600',
    description: 'Cả hai bề mặt được phủ phim, độ bền cao nhất.'
  }
];

const configs = ref({});
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);
const activeOpt = ref({});

const now = new Date();
const editForm = reactive({
  month: now.getMonth() + 1,
  year: now.getFullYear(),
  value: 0
});

const fetchConfigs = async () => {
  loading.value = true;
  try {
    // Fetch each config value for current month/year
    const m = now.getMonth() + 1;
    const y = now.getFullYear();
    
    const results = await Promise.all(
      coatingOptions.map(opt => 
        $api.get(`/payroll-configs/${opt.configKey}`, { params: { month: m, year: y } })
          .catch(() => ({ data: '0' }))
      )
    );
    
    results.forEach((res, idx) => {
      configs.value[coatingOptions[idx].configKey] = res.data;
    });
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openEdit = (opt) => {
  activeOpt.value = opt;
  editForm.value = configs.value[opt.configKey] || 0;
  showModal.value = true;
};

const handleSave = async () => {
  saving.value = true;
  try {
    await $api.put(`/payroll-configs/${activeOpt.value.configKey}`, {
      value: editForm.value.toString(),
      month: editForm.month,
      year: editForm.year
    });
    alert('Cập nhật thành công');
    showModal.value = false;
    fetchConfigs();
  } catch (err) {
    alert('Lỗi: ' + (err.response?.data?.message || err.message));
  } finally {
    saving.value = false;
  }
};

const formatMoney = (val) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
};

onMounted(fetchConfigs);
</script>
