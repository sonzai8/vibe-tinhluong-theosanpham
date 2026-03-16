<template>
  <div class="space-y-8 pb-12">
    <!-- Welcome Header with Date & Quick Action -->
    <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
      <div class="animate-in fade-in slide-in-from-left duration-700">
        <h1 class="text-4xl font-black text-slate-900 tracking-tight">
          Bảng điều khiển <span class="text-primary-600">CMS</span>
        </h1>
        <p class="text-slate-500 font-medium flex items-center gap-2 mt-2">
          <Calendar class="w-4 h-4 text-primary-500" />
          {{ todayFormatted }} — Hệ thống vận hành ổn định
        </p>
      </div>
      <div class="flex flex-wrap gap-3 animate-in fade-in slide-in-from-right duration-700">
        <button class="px-5 py-2.5 bg-white border border-slate-200 rounded-xl text-slate-700 font-bold hover:bg-slate-50 hover:border-slate-300 transition-all flex items-center gap-2 shadow-sm">
          <FileText class="w-4 h-4" />
          Báo cáo nhanh
        </button>
        <NuxtLink to="/payrolls" class="px-5 py-2.5 bg-primary-600 text-white rounded-xl font-bold hover:bg-primary-700 transition-all flex items-center gap-2 shadow-lg shadow-primary-100 ring-2 ring-primary-500/10">
          <Zap class="w-4 h-4" />
          Chốt lương tháng {{ new Date().getMonth() + 1 }}
        </NuxtLink>
      </div>
    </div>

    <!-- Analytics Dashboard Tiles -->
    <div v-if="loading" class="flex justify-center py-10">
      <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div v-for="(stat, idx) in mainStats" :key="idx" 
        class="card p-6 group hover:scale-[1.02] hover:shadow-xl hover:shadow-primary-100 transition-all duration-300 cursor-pointer border-b-4"
        :class="stat.border">
        <div class="flex items-center justify-between mb-6">
          <div :class="`w-14 h-14 rounded-2xl ${stat.bg} flex items-center justify-center transition-transform group-hover:rotate-6`">
            <component :is="stat.icon" class="w-7 h-7" :class="stat.text" />
          </div>
        </div>
        <div class="space-y-1">
          <p class="text-xs font-black text-slate-400 uppercase tracking-[0.2em]">{{ stat.label }}</p>
          <h3 class="text-3xl font-black text-slate-900 tracking-tighter">{{ stat.value }}</h3>
        </div>
      </div>
    </div>

    <!-- CMS Core Sections -->
    <div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
      
      <!-- Left Column: Operations & Monitoring -->
      <div class="xl:col-span-2 space-y-8">
        
        <!-- Activity Chart Placeholder / Visual -->
        <div class="card p-8 bg-white overflow-hidden relative">
          <div class="flex items-center justify-between mb-8">
            <div>
              <h3 class="text-xl font-black text-slate-900">Biểu đồ sản lượng</h3>
              <p class="text-sm text-slate-400 font-medium">Theo dõi năng suất 7 ngày gần nhất</p>
            </div>
            <select class="bg-slate-50 border-none rounded-lg text-xs font-bold px-3 py-2 text-slate-500">
              <option>Tuần này</option>
              <option>Tuần trước</option>
            </select>
          </div>
          
          <!-- Real Chart UI -->
          <div v-if="!loading" class="h-64 flex items-end justify-between gap-4 px-2">
            <div v-for="(day, i) in chartData" :key="i" class="flex-1 flex flex-col items-center gap-3 group">
              <div :class="`w-full rounded-t-xl transition-all duration-500 group-hover:bg-primary-500 relative ${i === 6 ? 'bg-primary-600' : 'bg-primary-100'}`"
                   :style="`height: ${Math.max(10, day.percentage)}%`">
                <div class="absolute -top-10 left-1/2 -translate-x-1/2 bg-slate-900 text-white text-[10px] font-bold px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-10">
                  {{ day.quantity.toLocaleString() }}
                </div>
              </div>
              <span class="text-[10px] font-black text-slate-400 uppercase">{{ day.shortDate }}</span>
            </div>
          </div>
        </div>

        <!-- Latest Production Records -->
        <div class="card overflow-hidden">
          <div class="p-6 border-b border-slate-50 flex items-center justify-between bg-slate-50/30">
            <h3 class="font-black text-slate-900">Nhật ký sản xuất trực tuyến</h3>
            <NuxtLink to="/production-records" class="text-primary-600 text-xs font-black uppercase tracking-widest hover:underline">Xem tất cả</NuxtLink>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-left">
              <thead>
                <tr class="text-slate-400 text-[10px] font-black uppercase tracking-widest">
                  <th class="px-8 py-4">Nhân viên</th>
                  <th class="px-8 py-4">Sản phẩm</th>
                  <th class="px-8 py-4">Công đoạn</th>
                  <th class="px-8 py-4">Số lượng</th>
                  <th class="px-8 py-4">Trạng thái</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-50">
                <tr v-if="latestRecords.length === 0" class="text-center py-4">
                  <td colspan="5" class="px-8 py-5 text-sm text-slate-400 font-medium">Chưa có nhật ký sản xuất</td>
                </tr>
                <tr v-for="(r, i) in latestRecords" :key="i" class="hover:bg-slate-50/50 transition-colors group">
                  <td class="px-8 py-5">
                    <div class="flex items-center gap-4">
                      <div class="w-9 h-9 rounded-full bg-slate-100 flex items-center justify-center text-xs font-black text-slate-500 group-hover:bg-primary-600 group-hover:text-white transition-all shadow-sm">
                        {{ r.employeeName.substring(0, 1).toUpperCase() }}
                      </div>
                      <span class="text-sm font-bold text-slate-700">{{ r.employeeName }}</span>
                    </div>
                  </td>
                  <td class="px-8 py-5 text-sm text-slate-500 font-medium">{{ r.productName }}</td>
                  <td class="px-8 py-5">
                    <span class="px-2 py-0.5 bg-slate-100 rounded text-[9px] font-black text-slate-500 uppercase">{{ r.stepName }}</span>
                  </td>
                  <td class="px-8 py-5 font-black text-slate-900">{{ (r.quantity).toLocaleString() }}</td>
                  <td class="px-8 py-5">
                    <span class="w-2 h-2 rounded-full bg-emerald-500 inline-block mr-2 shadow-sm shadow-emerald-200"></span>
                    <span class="text-[10px] font-black text-emerald-600 uppercase">{{ r.status }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Right Column: Shortcuts & Management Info -->
      <div class="space-y-8">
        <!-- System Quick Links -->
        <div class="card p-8 bg-slate-900 border-none relative overflow-hidden group">
          <Zap class="absolute -right-8 -top-8 w-40 h-40 text-white/5 rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
          <div class="relative z-10 space-y-6">
            <h3 class="text-2xl font-black text-white leading-tight">Phím tắt CMS</h3>
            <div class="grid grid-cols-2 gap-3">
              <NuxtLink v-for="link in shortcuts" :key="link.label" :to="link.to" class="p-4 bg-white/10 hover:bg-white border border-white/10 hover:border-white rounded-2xl transition-all group/item">
                <component :is="link.icon" class="w-5 h-5 text-white group-hover/item:text-primary-600 mb-2" />
                <p class="text-[10px] font-black text-white/60 group-hover/item:text-slate-900 uppercase tracking-widest">{{ link.label }}</p>
              </NuxtLink>
            </div>
          </div>
        </div>

        <!-- Organization Snapshot -->
        <div class="card p-8">
          <h3 class="font-black text-slate-900 mb-6">Phòng ban tiêu biểu</h3>
          <div class="space-y-6">
            <div v-if="topDepts.length === 0" class="text-sm text-slate-400">Chưa có dữ liệu phòng ban.</div>
            <div v-for="(dept, index) in topDepts" :key="index" class="space-y-3">
              <div class="flex justify-between items-center">
                <div class="flex items-center gap-3">
                  <div :class="`w-2 h-2 rounded-full ${deptColors[index % deptColors.length]}`"></div>
                  <span class="text-sm font-bold text-slate-700">{{ dept.name }}</span>
                </div>
                <span class="text-xs font-black text-slate-500 uppercase">{{ dept.employees }} Nhân sự</span>
              </div>
              <div class="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                <div :class="`h-full rounded-full ${deptColors[index % deptColors.length]}`" :style="`width: ${dept.percentage}%`"></div>
              </div>
            </div>
          </div>
          <NuxtLink to="/departments" class="block w-full mt-8">
            <UiButton class="w-full bg-slate-50 text-slate-600 border border-slate-200 hover:bg-slate-100 hover:text-slate-900">
              Xem cấu trúc tổ chức
            </UiButton>
          </NuxtLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { 
  Users, 
  Package, 
  CreditCard, 
  TrendingUp, 
  ArrowUpRight, 
  ArrowDownRight, 
  Calendar, 
  Zap, 
  FileText,
  Briefcase,
  Layers,
  Search,
  CheckCircle2,
  Clock,
  Menu
} from 'lucide-vue-next';

const { $api } = useNuxtApp();
const { user } = useAuth();

const todayFormatted = computed(() => {
  return new Intl.DateTimeFormat('vi-VN', { 
    weekday: 'long', 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric' 
  }).format(new Date());
});

const loading = ref(true);
const statsData = ref({
  activeEmployees: 0,
  todayProduction: 0,
  totalDepartments: 0,
  totalTeams: 0
});
const chartData = ref([]);
const latestRecords = ref([]);
const topDepts = ref([]);

const mainStats = computed(() => [
  { label: 'Nhân sự Active', value: statsData.value.activeEmployees.toLocaleString(), icon: Users, bg: 'bg-emerald-100', text: 'text-emerald-600', border: 'border-emerald-500' },
  { label: 'Sản lượng hôm nay', value: statsData.value.todayProduction.toLocaleString(), icon: Package, bg: 'bg-sky-100', text: 'text-sky-600', border: 'border-sky-500' },
  { label: 'Tổng số tổ đội', value: statsData.value.totalTeams.toLocaleString(), icon: CreditCard, bg: 'bg-orange-100', text: 'text-orange-600', border: 'border-orange-500' },
  { label: 'Tổng số phòng ban', value: statsData.value.totalDepartments.toLocaleString(), icon: TrendingUp, bg: 'bg-indigo-100', text: 'text-indigo-600', border: 'border-indigo-500' },
]);

const shortcuts = [
  { to: '/employees', label: 'Nhân viên', icon: Users },
  { to: '/attendances', label: 'Chấm công', icon: Clock },
  { to: '/products', label: 'Danh mục SP', icon: Package },
  { to: '/payrolls', label: 'Bảng lương', icon: CreditCard },
];

const deptColors = ['bg-emerald-500', 'bg-blue-500', 'bg-orange-500', 'bg-indigo-500'];

const fetchDashboardData = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/dashboard/stats');
    const data = res.data;
    
    statsData.value = {
      activeEmployees: data.activeEmployees || 0,
      todayProduction: data.todayProduction || 0,
      totalDepartments: data.totalDepartments || 0,
      totalTeams: data.totalTeams || 0
    };
    
    latestRecords.value = data.latestRecords || [];
    
    // Calculate dept percentages
    const maxEmp = Math.max(...(data.topDepartments.map(d => d.employees) || [1]));
    topDepts.value = (data.topDepartments || []).map(d => ({
      ...d,
      percentage: maxEmp > 0 ? (d.employees / maxEmp) * 100 : 0
    }));

    // Calculate chart height percentages & format dates
    const rawChart = data.productionChart || [];
    const maxQty = Math.max(...rawChart.map(c => c.quantity), 1);
    chartData.value = rawChart.map(c => {
      const d = new Date(c.date);
      return {
        ...c,
        percentage: (c.quantity / maxQty) * 100,
        shortDate: d.toLocaleDateString('vi-VN', { weekday: 'short' })
      };
    });
    
  } catch (err) {
    console.error('Lỗi khi tải dữ liệu dashboard', err);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchDashboardData();
});
</script>

<style scoped>
.animate-in {
  animation-fill-mode: forwards;
}
</style>
