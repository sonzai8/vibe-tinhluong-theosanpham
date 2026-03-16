<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Cấu hình Hệ thống Lương</h2>
        <p class="text-slate-500 font-medium">Cài đặt các thông số nền tảng cho việc tính toán lương</p>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
      <!-- Attendance Config -->
      <div class="card p-8 space-y-6">
        <div class="flex items-center justify-between mb-2">
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-2xl bg-primary-50 text-primary-600 flex items-center justify-center shadow-sm">
              <ClipboardCheck class="w-6 h-6" />
            </div>
            <div>
              <h3 class="text-lg font-black text-slate-900">Thông số tính toán</h3>
              <p class="text-xs text-slate-400 font-bold uppercase tracking-widest">Pricing & Policy</p>
            </div>
          </div>
          
          <div class="flex items-center gap-2 bg-slate-100 p-1 rounded-xl shrink-0">
            <select v-model="selectedMonth" @change="fetchConfigs" class="bg-transparent border-none text-xs font-black text-slate-600 focus:ring-0 outline-none px-2 py-1">
              <option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</option>
            </select>
            <div class="w-px h-4 bg-slate-200"></div>
            <select v-model="selectedYear" @change="fetchConfigs" class="bg-transparent border-none text-xs font-black text-slate-600 focus:ring-0 outline-none px-2 py-1">
              <option v-for="y in [2024, 2025, 2026, 2027]" :key="y" :value="y">{{ y }}</option>
            </select>
          </div>
        </div>

        <div v-if="loading" class="py-12 flex flex-col items-center justify-center gap-4">
          <div class="w-10 h-10 border-4 border-slate-100 border-t-primary-600 rounded-full animate-spin"></div>
          <p class="text-xs text-slate-400 font-bold animate-pulse">Đang tải cấu hình...</p>
        </div>

        <div v-else class="space-y-6 animate-in fade-in duration-300">
          <div class="grid grid-cols-1 gap-4">
            <!-- Min attendance day -->
            <div class="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 block">Ngày công chuyên cần tối thiểu</label>
              <div class="flex gap-3">
                <input v-model="form.minAttendanceDays" type="number" class="w-24 bg-white border border-slate-200 rounded-lg px-3 py-2 text-sm font-bold text-slate-700" />
                <UiButton size="sm" @click="saveConfig('MIN_ATTENDANCE_DAYS', form.minAttendanceDays)" :loading="savingKey === 'MIN_ATTENDANCE_DAYS'">Lưu</UiButton>
              </div>
            </div>

            <!-- Film surcharges -->
            <div class="grid grid-cols-2 gap-4">
              <div class="p-4 bg-slate-50 rounded-xl border border-slate-100 text-xs">
                <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 block italic">Phụ phí Phủ phim 1 mặt</label>
                <div class="flex gap-2">
                  <input v-model="form.filmSurcharge1" type="number" class="w-full bg-white border border-slate-200 rounded-lg px-3 py-2 font-bold text-slate-700" />
                  <UiButton size="sm" @click="saveConfig('FILM_SURCHARGE_1_SIDE', form.filmSurcharge1)" :loading="savingKey === 'FILM_SURCHARGE_1_SIDE'">Lưu</UiButton>
                </div>
              </div>
              <div class="p-4 bg-slate-50 rounded-xl border border-slate-100 text-xs">
                <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2 block italic">Phụ phí Phủ phim 2 mặt</label>
                <div class="flex gap-2">
                  <input v-model="form.filmSurcharge2" type="number" class="w-full bg-white border border-slate-200 rounded-lg px-3 py-2 font-bold text-slate-700" />
                  <UiButton size="sm" @click="saveConfig('FILM_SURCHARGE_2_SIDE', form.filmSurcharge2)" :loading="savingKey === 'FILM_SURCHARGE_2_SIDE'">Lưu</UiButton>
                </div>
              </div>
            </div>
          </div>
          <p class="text-xs text-slate-400 leading-relaxed italic">
            * Các giá trị dưới đây cố định cho <b>Tháng {{ selectedMonth }}/{{ selectedYear }}</b>. Nếu chưa cài đặt, hệ thống dùng mặc định hoặc tháng cũ nhất.
          </p>
        </div>
      </div>

      <!-- Info/Help card -->
      <div class="card p-8 bg-slate-900 text-white space-y-6 relative overflow-hidden">
        <div class="relative z-10 space-y-6">
          <h3 class="text-xl font-black italic">Hướng dẫn vận hành</h3>
          <div class="space-y-4">
            <div class="flex gap-4">
              <div class="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center shrink-0">
                <span class="font-black text-primary-400">01</span>
              </div>
              <p class="text-sm text-slate-300 leading-relaxed">
                Đơn giá sản phẩm được chia làm 2 mức: <b>Cao</b> và <b>Thấp</b>.
              </p>
            </div>
            <div class="flex gap-4">
              <div class="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center shrink-0">
                <span class="font-black text-primary-400">02</span>
              </div>
              <p class="text-sm text-slate-300 leading-relaxed">
                Hệ thống tự động đếm số ngày có phát sinh chấm công của nhân viên trong tháng để so sánh với định mức chuyên cần.
              </p>
            </div>
            <div class="flex gap-4">
              <div class="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center shrink-0">
                <span class="font-black text-primary-400">03</span>
              </div>
              <p class="text-sm text-slate-300 leading-relaxed">
                Bất kỳ thay đổi cấu hình nào cũng sẽ ảnh hưởng đến các lần <b>Tính lại lương</b> tiếp theo. Các bảng lương đã chốt sẽ không bị thay đổi.
              </p>
            </div>
          </div>
        </div>
        <!-- Decorative blobs -->
        <div class="absolute -bottom-20 -right-20 w-64 h-64 bg-primary-600/20 rounded-full blur-3xl"></div>
        <div class="absolute -top-20 -left-20 w-64 h-64 bg-blue-600/10 rounded-full blur-3xl"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ClipboardCheck, Info } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const loading = ref(true);
const savingKey = ref(null);
const configs = ref([]);

const selectedMonth = ref(new Date().getMonth() + 1);
const selectedYear = ref(new Date().getFullYear());

const form = reactive({
  minAttendanceDays: '22',
  filmSurcharge1: '0',
  filmSurcharge2: '0'
});

const fetchConfigs = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/payroll-configs');
    configs.value = res.data;
    
    // Set form values from current configs if available
    const minD = configs.value.find(c => c.configKey === 'MIN_ATTENDANCE_DAYS');
    if (minD) form.minAttendanceDays = minD.configValue;
    
    const s1 = configs.value.find(c => c.configKey === 'FILM_SURCHARGE_1_SIDE');
    if (s1) form.filmSurcharge1 = s1.configValue;
    
    const s2 = configs.value.find(c => c.configKey === 'FILM_SURCHARGE_2_SIDE');
    if (s2) form.filmSurcharge2 = s2.configValue;
    
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const saveConfig = async (key, value) => {
  savingKey.value = key;
  try {
    await $api.put(`/payroll-configs/${key}`, { 
      value: value.toString(),
      month: selectedMonth.value,
      year: selectedYear.value
    });
    await fetchConfigs();
    alert('Cập nhật thành công');
  } catch (err) {
    alert('Lỗi khi cập nhật cấu hình');
  } finally {
    savingKey.value = null;
  }
};

onMounted(fetchConfigs);
</script>
