<template>
  <div class="space-y-8">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-3xl font-black text-slate-900 tracking-tight">Trung tâm Bảng lương</h2>
        <p class="text-slate-500 font-medium">Tính toán và quản lý lương nhân viên hàng tháng</p>
      </div>
      <div class="flex gap-3">
        <div class="card p-1 flex bg-slate-100 rounded-xl">
          <button 
            @click="viewMode = 'list'"
            :class="`px-4 py-2 rounded-lg text-sm font-black transition-all ${viewMode === 'list' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-400 hover:text-slate-600'}`"
          >
            Lịch sử chốt lương
          </button>
          <button 
            @click="viewMode = 'calculate'"
            :class="`px-4 py-2 rounded-lg text-sm font-black transition-all ${viewMode === 'calculate' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-400 hover:text-slate-600'}`"
          >
            Tính lương mới
          </button>
        </div>
      </div>
    </div>

    <!-- Calculate View -->
    <div v-if="viewMode === 'calculate'" class="space-y-8 animate-in fade-in slide-in-from-top-4 duration-500">
      <div class="card p-10 flex flex-col items-center text-center space-y-8 max-w-2xl mx-auto shadow-2xl shadow-primary-50">
        <div class="w-20 h-20 bg-primary-600 rounded-3xl flex items-center justify-center text-white shadow-xl shadow-primary-200">
          <Calculator class="w-10 h-10" />
        </div>
        <div class="space-y-2">
          <h3 class="text-2xl font-black text-slate-900">Tính toán lương tháng</h3>
          <p class="text-slate-500 font-medium max-w-sm">Hệ thống sẽ tổng hợp chấm công, sản lượng và các khoản thưởng/phạt để xuất bảng lương.</p>
        </div>

        <div class="flex gap-6 w-full">
          <div class="flex-1 flex flex-col gap-2">
            <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest text-left ml-1">Chọn Tháng</label>
            <select v-model="calcForm.month" class="input-field h-14 text-lg font-black">
              <option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</option>
            </select>
          </div>
          <div class="flex-1 flex flex-col gap-2">
            <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest text-left ml-1">Chọn Năm</label>
            <select v-model="calcForm.year" class="input-field h-14 text-lg font-black">
              <option v-for="y in [2024, 2025, 2026]" :key="y" :value="y">Năm {{ y }}</option>
            </select>
          </div>
        </div>

        <UiButton @click="handleCalculate" class="w-full h-16 text-xl font-black rounded-2xl shadow-xl shadow-primary-100" :loading="calculating">
          <Play class="w-6 h-6 fill-current" />
          Bắt đầu tính lương
        </UiButton>
      </div>
    </div>

    <!-- History List View -->
    <div v-else class="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div class="card overflow-hidden">
        <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
          <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
          <p class="text-slate-500 font-bold">Đang tải lịch sử...</p>
        </div>

        <div v-else-if="payrolls.length === 0" class="p-20 text-center space-y-4">
          <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-300">
            <Wallet class="w-10 h-10" />
          </div>
          <p class="text-slate-500 font-bold">Chưa có bảng lương nào được chốt.</p>
        </div>

        <table v-else class="w-full text-left">
          <thead>
            <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
              <th class="px-8 py-5">Kỳ lương</th>
              <th class="px-8 py-5">Nhân viên</th>
              <th class="px-8 py-5">Lương SP</th>
              <th class="px-8 py-5">Thưởng/Phạt</th>
              <th class="px-8 py-5">Thực lĩnh</th>
              <th class="px-8 py-5">Trạng thái</th>
              <th class="px-8 py-5 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 border-b border-slate-100">
            <tr v-for="p in payrolls" :key="p.id" class="hover:bg-slate-50/50 transition-all group">
              <td class="px-8 py-5">
                <span class="font-black text-slate-900">{{ p.month }}/{{ p.year }}</span>
              </td>
              <td class="px-8 py-5">
                <p class="font-bold text-slate-700">{{ p.employeeName }}</p>
                <p class="text-[10px] font-black text-slate-400 uppercase tracking-tighter">{{ p.employeeCode }}</p>
              </td>
              <td class="px-8 py-5 text-sm font-bold text-slate-600">{{ p.productSalary.toLocaleString() }}đ</td>
              <td class="px-8 py-5 text-sm font-bold" :class="p.totalPenaltyBonus >= 0 ? 'text-emerald-600' : 'text-red-600'">
                {{ p.totalPenaltyBonus >= 0 ? '+' : '' }}{{ p.totalPenaltyBonus.toLocaleString() }}đ
              </td>
              <td class="px-8 py-5 font-black text-primary-700 text-lg">{{ p.totalSalary.toLocaleString() }}đ</td>
              <td class="px-8 py-5">
                <span :class="`px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${p.status === 'CONFIRMED' ? 'bg-emerald-50 text-emerald-600' : 'bg-orange-50 text-orange-600'}`">
                  {{ p.status === 'CONFIRMED' ? 'Đã chốt' : 'Dự toán' }}
                </span>
              </td>
              <td class="px-8 py-5 text-right">
                <button v-if="p.status !== 'CONFIRMED'" @click="handleConfirm(p.id)" class="px-4 py-2 bg-primary-50 text-primary-700 rounded-lg text-xs font-black hover:bg-primary-600 hover:text-white transition-all">
                  Chốt lương
                </button>
                <div v-else class="flex justify-end pr-2">
                  <CheckCircle2 class="text-emerald-500 w-6 h-6" />
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Calculator, Play, Wallet, CheckCircle2 } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const viewMode = ref('list');
const calculating = ref(false);
const loading = ref(true);
const payrolls = ref([]);

const calcForm = reactive({
  month: new Date().getMonth() + 1,
  year: new Date().getFullYear()
});

const fetchPayrolls = async () => {
  loading.value = true;
  try {
    const res = await $api.get(`/payrolls/${calcForm.year}/${calcForm.month}/items`);
    payrolls.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const handleCalculate = async () => {
  calculating.value = true;
  try {
    const res = await $api.post('/payrolls/calculate', calcForm);
    alert(res.message);
    viewMode.value = 'list';
    fetchPayrolls();
  } catch (err) {
    alert(err.message || 'Lỗi tính toán');
  } finally {
    calculating.value = false;
  }
};

const handleConfirm = async (id) => {
  if (!confirm('Xác nhận chốt lương cho nhân viên này? Thao tác này không thể hoàn tác.')) return;
  try {
    await $api.put(`/payrolls/${id}/confirm`);
    fetchPayrolls();
  } catch (err) {
    alert(err.message);
  }
};

onMounted(fetchPayrolls);

// Reset payrolls when month/year changes in list view
watch([() => calcForm.month, () => calcForm.year], () => {
  if (viewMode.value === 'list') fetchPayrolls();
});
</script>
