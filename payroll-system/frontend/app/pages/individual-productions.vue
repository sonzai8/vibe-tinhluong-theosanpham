<template>
  <div class="space-y-8 animate-in fade-in duration-500">
    <!-- Header Section -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-6">
      <div>
        <h1 class="text-3xl font-black text-slate-900 tracking-tight flex items-center gap-3">
          <div class="w-12 h-12 rounded-2xl bg-primary-600 flex items-center justify-center text-white shadow-lg shadow-primary-200">
            <Users class="w-6 h-6" />
          </div>
          {{ $t('individual_production.title') }}
        </h1>
        <p class="text-slate-500 mt-2 font-medium">{{ $t('individual_production.subtitle') }}</p>
      </div>

      <div class="flex items-center gap-3">
        <div class="bg-white p-1.5 rounded-2xl shadow-sm border border-slate-100 flex items-center">
          <button 
            @click="viewMode = 'matrix'"
            :class="['px-6 py-2.5 rounded-xl text-xs font-black uppercase tracking-widest transition-all', viewMode === 'matrix' ? 'bg-primary-600 text-white shadow-lg shadow-primary-100' : 'text-slate-400 hover:text-slate-600']"
          >
            <div class="flex items-center gap-2">
              <LayoutGrid class="w-4 h-4" />
              {{ $t('common.matrix_view') }}
            </div>
          </button>
        </div>
        <UiButton @click="fetchData" variant="secondary" :loading="loading">
          <RefreshCw :class="['w-4 h-4 mr-2', loading ? 'animate-spin' : '']" />
          {{ $t('common.refresh') || 'Làm mới' }}
        </UiButton>
      </div>
    </div>

    <!-- Filter Section -->
    <div class="card p-8">
      <div class="grid grid-cols-1 md:grid-cols-4 lg:grid-cols-5 gap-6">
        <div class="space-y-2">
          <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">{{ $t('common.reporting_month') }}</label>
          <div class="flex gap-2">
            <select v-model="filter.month" class="flex-1 bg-slate-50 border-none rounded-xl px-4 py-3 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all">
              <option v-for="m in 12" :key="m" :value="m">{{ $t('common.month') }} {{ m }}</option>
            </select>
            <select v-model="filter.year" class="flex-1 bg-slate-50 border-none rounded-xl px-4 py-3 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all">
              <option v-for="y in [2024, 2025, 2026, 2027]" :key="y" :value="y">{{ y }}</option>
            </select>
          </div>
        </div>

        <div class="space-y-2">
          <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">{{ $t('common.department') }}</label>
          <select v-model="filter.departmentId" class="w-full bg-slate-50 border-none rounded-xl px-4 py-3 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all">
            <option value="">{{ $t('common.all') }}</option>
            <option v-for="d in departments" :key="d.id" :value="d.id">{{ d.name }}</option>
          </select>
        </div>

        <div class="space-y-2">
          <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">{{ $t('common.team') }}</label>
          <select v-model="filter.teamId" class="w-full bg-slate-50 border-none rounded-xl px-4 py-3 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all">
            <option value="">{{ $t('common.all') }}</option>
            <option v-for="t in filteredTeams" :key="t.id" :value="t.id">{{ t.name }}</option>
          </select>
        </div>

        <div class="md:col-span-1 lg:col-span-2 flex items-end">
           <div class="p-4 bg-primary-50 rounded-2xl border border-primary-100 flex items-center gap-3 w-full">
             <Info class="w-5 h-5 text-primary-600 shrink-0" />
             <p class="text-[10px] text-primary-700 font-bold leading-tight">
               {{ $t('individual_production.matrix_desc') }}
             </p>
           </div>
        </div>
      </div>
    </div>

    <!-- Main Content: Matrix View -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-24 flex flex-col items-center justify-center gap-6">
        <div class="w-16 h-16 border-4 border-primary-100 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-black animate-pulse uppercase tracking-widest text-xs">{{ $t('production.loading') }}</p>
      </div>

      <div v-else-if="employees.length === 0" class="p-24 text-center">
        <div class="w-20 h-20 bg-slate-50 rounded-full flex items-center justify-center mx-auto text-slate-200 mb-6">
          <Users class="w-10 h-10" />
        </div>
        <p class="text-slate-500 font-black">{{ $t('common.not_found') }}</p>
      </div>

      <div v-else class="overflow-x-auto custom-scrollbar">
        <table class="w-full text-left table-fixed border-collapse">
          <thead class="sticky top-0 z-20 bg-white">
            <tr class="text-[10px] font-black uppercase text-slate-400 tracking-widest border-b border-slate-100">
              <th class="p-5 w-64 bg-white sticky left-0 z-30 border-r border-slate-100 shadow-[2px_0_5px_rgba(0,0,0,0.02)]">{{ $t('production.employee') }}</th>
              <th class="p-5 w-24 text-center bg-slate-50 font-black text-slate-900 border-r border-slate-100 sticky left-64 z-30 shadow-[2px_0_5px_rgba(0,0,0,0.05)]">{{ $t('common.total') }}</th>
              
              <!-- Daily Header -->
              <template v-if="viewType === 'daily'">
                <th v-for="d in days" :key="d" :class="['p-2 w-14 text-center border-r border-slate-50', isSunday(d) ? 'bg-red-50 text-red-500' : '']">
                  <div class="text-[11px]">{{ d }}</div>
                  <div class="text-[8px] opacity-60">{{ getDayName(d) }}</div>
                </th>
              </template>

              <!-- Weekly Header -->
              <template v-else>
                <th v-for="w in 5" :key="w" class="p-2 w-32 text-center border-r border-slate-50 bg-slate-50/50">
                  <div class="text-[11px]">{{ $t('common.week') || 'Tuần' }} {{ w }}</div>
                  <div class="text-[8px] opacity-60">{{ getWeekRange(w) }}</div>
                </th>
              </template>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-50 text-[11px]">
            <tr v-for="emp in filteredEmployees" :key="emp.id" class="hover:bg-slate-50/50 group transition-colors">
              <td class="p-5 bg-white sticky left-0 z-10 border-r border-slate-100 shadow-[2px_0_5px_rgba(0,0,0,0.02)] group-hover:bg-slate-50/80">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-xl bg-slate-100 flex items-center justify-center text-[10px] font-black text-slate-400 group-hover:bg-primary-600 group-hover:text-white transition-all shadow-sm">
                    {{ emp.fullName[0] }}
                  </div>
                  <div>
                    <p class="font-black text-slate-900 line-clamp-1">{{ emp.fullName }}</p>
                    <div class="flex items-center gap-2">
                      <p class="text-[9px] font-bold text-slate-400 uppercase tracking-tighter">{{ emp.code }}</p>
                      <span v-if="emp.team" class="text-[8px] font-black text-primary-600 bg-primary-50 px-1.5 py-0.5 rounded uppercase">{{ emp.team.name }}</span>
                    </div>
                  </div>
                </div>
              </td>
              <td class="p-5 text-center bg-slate-50/80 font-black text-primary-600 sticky left-64 z-10 shadow-[2px_0_5px_rgba(0,0,0,0.05)] border-r border-slate-100 group-hover:bg-slate-100">
                {{ formatNumber(getEmployeeTotal(emp.id)) }}
              </td>

              <!-- Daily Cells -->
              <template v-if="viewType === 'daily'">
                <td v-for="d in days" :key="d" :class="['p-2 text-center border-r border-slate-50 relative group/cell', isSunday(d) ? 'bg-red-50/20' : '']">
                  <div v-if="getProduction(emp.id, d)" class="space-y-0.5">
                    <div class="font-black text-slate-700">{{ formatNumber(getProduction(emp.id, d).quantity) }}</div>
                    <div v-if="getProduction(emp.id, d).actualTeamName !== emp.team?.name" class="text-[7px] text-amber-600 font-black uppercase tracking-tighter bg-amber-50 px-1 rounded inline-block">
                      {{ getProduction(emp.id, d).actualTeamName }}
                    </div>
                  </div>
                  <div v-else class="text-slate-200">0</div>
                </td>
              </template>

              <!-- Weekly Cells -->
              <template v-else>
                <td v-for="w in 5" :key="w" class="p-2 text-center border-r border-slate-50">
                  <div v-if="getWeeklyTotal(emp.id, w) > 0" class="font-black text-primary-700 bg-primary-50/50 py-2 rounded-lg border border-primary-100/50">
                    {{ formatNumber(getWeeklyTotal(emp.id, w)) }}
                  </div>
                  <div v-else class="text-slate-200">-</div>
                </td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Users, LayoutGrid, RefreshCw, Info, History } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const viewMode = ref('matrix');
const viewType = ref('daily');
const loading = ref(false);

const filter = reactive({
  month: new Date().getMonth() + 1,
  year: new Date().getFullYear(),
  departmentId: '',
  teamId: ''
});

const employees = ref([]);
const departments = ref([]);
const teams = ref([]);
const productions = ref([]);

const filteredTeams = computed(() => {
  if (!Array.isArray(teams.value)) return [];
  if (!filter.departmentId) return teams.value;
  return teams.value.filter(t => t.department?.id == filter.departmentId);
});

const days = computed(() => {
  const date = new Date(filter.year, filter.month, 0);
  return date.getDate();
});

const getWeekRange = (week) => {
  const startDay = (week - 1) * 7 + 1;
  let endDay = week * 7;
  const maxDays = days.value;
  if (startDay > maxDays) return '---';
  if (endDay > maxDays) endDay = maxDays;
  return `${startDay}/${filter.month} - ${endDay}/${filter.month}`;
};

const isSunday = (day) => {
  const date = new Date(filter.year, filter.month - 1, day);
  return date.getDay() === 0;
};

const getDayName = (day) => {
  const date = new Date(filter.year, filter.month - 1, day);
  return date.toLocaleDateString('vi-VN', { weekday: 'short' }).replace('Th ', 'T');
};

const fetchData = async () => {
  loading.value = true;
  try {
    const firstDay = `${filter.year}-${String(filter.month).padStart(2, '0')}-01`;
    const lastDay = `${filter.year}-${String(filter.month).padStart(2, '0')}-${days.value}`;

    const [empRes, deptRes, teamRes, prodRes] = await Promise.all([
      $api.get('/employees').catch(() => ({ data: [] })),
      $api.get('/departments').catch(() => ({ data: [] })),
      $api.get('/teams').catch(() => ({ data: [] })),
      $api.get('/individual-productions', { 
        params: { 
          from: firstDay, 
          to: lastDay,
          departmentIds: filter.departmentId ? [filter.departmentId] : null,
          teamIds: filter.teamId ? [filter.teamId] : null
        } 
      }).catch(() => ({ data: [] }))
    ]);

    employees.value = empRes?.data || [];
    departments.value = deptRes?.data || [];
    teams.value = teamRes?.data || [];
    productions.value = prodRes?.data || [];
  } catch (err) {
    console.error('Lỗi tải dữ liệu:', err);
    employees.value = [];
    productions.value = [];
  } finally {
    loading.value = false;
  }
};

const filteredEmployees = computed(() => {
  if (!Array.isArray(employees.value)) return [];
  return employees.value.filter(e => {
    if (!e || e.status !== 'ACTIVE') return false;
    const matchDept = !filter.departmentId || (e.department?.id || e.team?.department?.id) == filter.departmentId;
    const matchTeam = !filter.teamId || e.team?.id == filter.teamId;
    return matchDept && matchTeam;
  });
});

const getProduction = (empId, day) => {
  if (!Array.isArray(productions.value)) return null;
  const dateStr = `${filter.year}-${String(filter.month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  return productions.value.find(p => p.employeeId === empId && p.date === dateStr);
};

const getWeeklyTotal = (empId, week) => {
  if (!Array.isArray(productions.value)) return 0;
  const startDay = (week - 1) * 7 + 1;
  const endDay = week * 7;
  const maxDays = days.value;
  
  return productions.value
    .filter(p => {
      if (p.employeeId !== empId) return false;
      const d = parseInt(p.date.split('-')[2]);
      return d >= startDay && d <= endDay && d <= maxDays;
    })
    .reduce((sum, p) => sum + p.quantity, 0);
};

const getEmployeeTotal = (empId) => {
  if (!Array.isArray(productions.value)) return 0;
  return productions.value
    .filter(p => p.employeeId === empId)
    .reduce((sum, p) => sum + p.quantity, 0);
};

const formatNumber = (val) => {
  if (!val && val !== 0) return '0';
  return val.toLocaleString('vi-VN', { 
    minimumFractionDigits: 0, 
    maximumFractionDigits: 1 
  });
};

watch([() => filter.month, () => filter.year, () => filter.departmentId, () => filter.teamId], () => {
  fetchData();
});

onMounted(() => {
  fetchData();
});
</script>
