<template>
  <div class="space-y-6">
    <!-- Header Section -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white dark:bg-slate-900 p-6 rounded-3xl shadow-sm border border-slate-100 dark:border-slate-800">
      <div>
        <h1 class="text-2xl font-black text-slate-900 dark:text-slate-100 tracking-tight">{{ $t('team_wages.title') }}</h1>
        <p class="text-sm text-slate-500 dark:text-slate-400 mt-1 font-medium">{{ $t('team_wages.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-3">
        <div class="flex items-center gap-2 bg-slate-50 dark:bg-slate-800 p-1.5 rounded-2xl border border-slate-100 dark:border-slate-700">
          <UiInput
            v-model="selectedMonth"
            type="month"
            class="!w-40 !h-10 !bg-transparent !border-none !shadow-none !ring-0 font-bold text-slate-700 dark:text-slate-200"
            @change="fetchData"
          />
        </div>
        <UiButton 
          variant="primary" 
          class="!rounded-2xl !h-11 font-bold gap-2 shadow-lg shadow-primary-200 dark:shadow-none"
          @click="fetchData"
        >
          <RefreshCw :class="['w-4 h-4', loading ? 'animate-spin' : '']" />
          {{ $t('common.refresh') }}
        </UiButton>
      </div>
    </div>

    <!-- Matrix View Card -->
    <div class="bg-white dark:bg-slate-900 rounded-3xl shadow-sm border border-slate-100 dark:border-slate-800 overflow-hidden min-h-[600px] flex flex-col">
      <div class="p-6 border-b border-slate-50 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/50 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center text-primary-600">
            <LayoutGrid class="w-5 h-5" />
          </div>
          <h3 class="font-black text-slate-800 dark:text-slate-200 tracking-tight">{{ $t('team_wages.matrix_view') }}</h3>
        </div>
        
        <!-- Legend -->
        <div class="flex items-center gap-6">
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 rounded-full bg-emerald-500"></div>
            <span class="text-[11px] font-black uppercase tracking-wider text-slate-500">{{ $t('team_wages.team_income') }}</span>
          </div>
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 rounded-full bg-blue-500"></div>
            <span class="text-[11px] font-black uppercase tracking-wider text-slate-500">{{ $t('team_wages.internal_cost') }}</span>
          </div>
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 rounded-full bg-amber-500"></div>
            <span class="text-[11px] font-black uppercase tracking-wider text-slate-500">{{ $t('team_wages.borrowed_cost') }}</span>
          </div>
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 rounded-full bg-indigo-500"></div>
            <span class="text-[11px] font-black uppercase tracking-wider text-slate-500">{{ $t('team_wages.lend_cost') }}</span>
          </div>
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 rounded-full bg-rose-500"></div>
            <span class="text-[11px] font-black uppercase tracking-wider text-slate-500">Quỹ đầu chuyền</span>
          </div>
        </div>
      </div>

      <div class="flex-1 overflow-auto relative custom-scrollbar">
        <table v-if="!loading && matrixData.length" class="w-full border-collapse">
          <thead class="sticky top-0 z-20">
            <tr>
              <th class="sticky left-0 z-30 bg-white dark:bg-slate-900 p-4 text-left border-b border-r border-slate-100 dark:border-slate-800 min-w-[200px]">
                <span class="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400">{{ $t('common.team') }}</span>
              </th>
              <th 
                v-for="day in daysInMonth" 
                :key="day"
                class="p-4 bg-slate-50 dark:bg-slate-800 border-b border-r border-slate-100 dark:border-slate-800 text-center min-w-[140px]"
                :class="{ 'bg-emerald-50 dark:bg-emerald-950/20': isToday(day) }"
              >
                <div class="flex flex-col items-center">
                  <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ getDayName(day) }}</span>
                  <span class="text-lg font-black text-slate-700 dark:text-slate-200 mt-0.5">{{ day }}</span>
                </div>
              </th>
              <th class="p-4 bg-slate-100 dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700 text-center min-w-[160px] sticky right-0 z-20">
                <span class="text-[10px] font-black uppercase tracking-[0.2em] text-slate-600 dark:text-slate-400">{{ $t('common.total') }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="teamId in sortedTeamIds" :key="teamId" class="group hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
              <td class="sticky left-0 z-10 bg-white dark:bg-slate-900 p-4 border-b border-r border-slate-100 dark:border-slate-800 font-bold text-slate-700 dark:text-slate-300 shadow-[4px_0_12px_rgba(0,0,0,0.02)]">
                {{ getTeamName(teamId) }}
              </td>
              <td 
                v-for="day in daysInMonth" 
                :key="day" 
                class="p-2 border-b border-r border-slate-100 dark:border-slate-800 text-center"
              >
                <div 
                  v-if="getDayData(teamId, day)" 
                  class="p-2 rounded-2xl bg-white dark:bg-slate-800 border border-slate-100 dark:border-slate-700 shadow-sm hover:shadow-md hover:scale-[1.02] transition-all cursor-pointer relative group/cell"
                  @click="showDetails(teamId, day)"
                >
                  <div class="flex flex-col gap-1">
                    <!-- Income -->
                    <div class="flex items-center justify-between px-2 py-1 rounded-lg bg-emerald-50 dark:bg-emerald-900/20">
                      <span class="text-[9px] font-black text-emerald-600 dark:text-emerald-400">IN</span>
                      <span class="text-[11px] font-black text-emerald-700 dark:text-emerald-300 underline decoration-dotted decoration-emerald-300">
                        {{ formatMoney(getDayData(teamId, day).totalTeamIncome) }}
                      </span>
                    </div>
                    <!-- Labor Costs -->
                    <div class="grid grid-cols-1 gap-0.5">
                      <div v-if="getDayData(teamId, day).leadFundAmount" class="flex items-center justify-between px-2 text-[10px] font-bold text-rose-600">
                        <span>LFUN</span>
                        <span>-{{ formatMoney(getDayData(teamId, day).leadFundAmount) }}</span>
                      </div>
                      <div class="flex items-center justify-between px-2 text-[10px] font-bold text-blue-600">
                        <span>LCL</span>
                        <span>{{ formatMoney(getDayData(teamId, day).internalLaborCost) }}</span>
                      </div>
                      <div class="flex items-center justify-between px-2 text-[10px] font-bold text-amber-600">
                        <span>BRW</span>
                        <span>{{ formatMoney(getDayData(teamId, day).borrowedLaborCost) }}</span>
                      </div>
                      <div class="flex items-center justify-between px-2 text-[10px] font-bold text-indigo-600">
                        <span>LND</span>
                        <span>{{ formatMoney(getDayData(teamId, day).lendLaborCost) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </td>
              <td class="p-4 bg-slate-50/80 dark:bg-slate-800/80 border-b border-slate-200 dark:border-slate-700 sticky right-0 z-10 shadow-[-4px_0_12px_rgba(0,0,0,0.02)]">
                <div class="flex flex-col gap-1.5 items-center">
                  <div class="w-full flex items-center justify-between px-3 py-1 rounded-xl bg-emerald-100 dark:bg-emerald-900/40 border border-emerald-200 dark:border-emerald-800">
                    <span class="text-[10px] font-black text-emerald-700 dark:text-emerald-400">T.IN</span>
                    <span class="text-xs font-black text-emerald-800 dark:text-emerald-200">{{ formatMoney(getTeamTotal(teamId, 'totalTeamIncome')) }}</span>
                  </div>
                  <div class="w-full flex flex-col gap-0.5 px-1">
                    <div class="flex items-center justify-between text-[10px] font-bold text-rose-600">
                      <span>T.LFUN</span>
                      <span>-{{ formatMoney(getTeamTotal(teamId, 'leadFundAmount')) }}</span>
                    </div>
                    <div class="flex items-center justify-between text-[10px] font-bold text-amber-600">
                      <span>T.BRW</span>
                      <span>{{ formatMoney(getTeamTotal(teamId, 'borrowedLaborCost')) }}</span>
                    </div>
                    <div class="flex items-center justify-between text-[10px] font-bold text-indigo-600">
                      <span>T.LND</span>
                      <span>{{ formatMoney(getTeamTotal(teamId, 'lendLaborCost')) }}</span>
                    </div>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Loading / Empty States -->
        <div v-else-if="loading" class="absolute inset-0 flex flex-col items-center justify-center bg-white/50 backdrop-blur-sm z-50">
          <div class="w-16 h-16 border-4 border-primary-100 border-t-primary-600 rounded-full animate-spin"></div>
          <p class="mt-4 text-slate-400 font-bold uppercase tracking-widest text-xs">{{ $t('common.loading') }}</p>
        </div>
        <div v-else class="absolute inset-0 flex flex-col items-center justify-center p-20 text-center">
          <div class="w-24 h-24 bg-slate-50 dark:bg-slate-800 rounded-full flex items-center justify-center text-slate-200 mb-6">
            <LayoutGrid class="w-12 h-12" />
          </div>
          <h4 class="text-lg font-black text-slate-400 dark:text-slate-600">{{ $t('common.no_data') }}</h4>
          <p class="text-sm text-slate-300 mt-2">{{ $t('common.not_found') }}</p>
        </div>
      </div>
    </div>

    <!-- Detail Dialog -->
    <UiDialog v-model:open="showDialog" class="!max-w-3xl">
      <template #header>
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-2xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center text-primary-600">
            <Users class="w-6 h-6" />
          </div>
          <div>
            <h2 class="text-xl font-black text-slate-900 dark:text-slate-100 tracking-tight">
              {{ $t('team_wages.worker_details', { date: formatDateVi(selectedDate) }) }}
            </h2>
            <p class="text-sm font-bold text-slate-400 uppercase tracking-widest mt-0.5">
              {{ selectedTeamName }}
            </p>
          </div>
        </div>
      </template>

      <div class="mt-6">
        <div class="overflow-hidden rounded-2xl border border-slate-100 dark:border-slate-800">
          <table class="w-full">
            <thead>
              <tr class="bg-slate-50 dark:bg-slate-800/50">
                <th class="px-4 py-3 text-left text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('common.employee') }}</th>
                <th class="px-4 py-3 text-left text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('attendance.original_team') }}</th>
                <th class="px-4 py-3 text-left text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('attendance.actual_team') }}</th>
                <th class="px-4 py-3 text-right text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('team_wages.amount') }}</th>
                <th class="px-4 py-3 text-center text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('common.status') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
              <tr v-for="worker in activeWorkerDetails" :key="worker.employeeId" class="hover:bg-slate-50 transition-colors">
                <td class="px-4 py-3">
                  <div class="flex flex-col">
                    <span class="font-bold text-slate-700 dark:text-slate-200">{{ worker.employeeName }}</span>
                    <span class="text-[10px] font-black text-slate-400">{{ worker.employeeCode }}</span>
                  </div>
                </td>
                <td class="px-4 py-3">
                  <span class="text-sm font-bold text-slate-500">{{ worker.originalTeamName }}</span>
                </td>
                <td class="px-4 py-3">
                  <span class="text-sm font-bold text-slate-500">{{ worker.actualTeamName }}</span>
                </td>
                <td class="px-4 py-3 text-right">
                  <span class="font-black text-slate-800 dark:text-slate-200">{{ formatMoney(worker.amount) }}</span>
                </td>
                <td class="px-4 py-3 text-center">
                  <div 
                    v-if="worker.status === 'BORROWED'"
                    class="inline-flex px-2 py-0.5 rounded-full bg-amber-50 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400 text-[9px] font-black uppercase tracking-tighter"
                  >
                    {{ $t('team_wages.is_borrowed') }}
                  </div>
                  <div 
                    v-else-if="worker.status === 'LEND'"
                    class="inline-flex px-2 py-0.5 rounded-full bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 text-[9px] font-black uppercase tracking-tighter"
                  >
                    {{ $t('team_wages.lend_cost') }}
                  </div>
                  <div 
                    v-else
                    class="inline-flex px-2 py-0.5 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 text-[9px] font-black uppercase tracking-tighter"
                  >
                    Nội bộ
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </UiDialog>

  </div>
</template>

<script setup>
import { 
  LayoutGrid, RefreshCw, Users, ArrowUpRight, ArrowDownRight, 
  HelpCircle, ChevronRight, Filter 
} from 'lucide-vue-next';
import { getCurrentMonth, formatDate } from '~/utils/date';
import dayjs from 'dayjs';

const { $api } = useNuxtApp();
const { t } = useI18n();

const selectedMonth = ref(getCurrentMonth());
const loading = ref(false);
const matrixData = ref([]);
const teams = ref({});

const showDialog = ref(false);
const showResetDialog = ref(false);
const selectedDate = ref(null);
const selectedTeamId = ref(null);
const selectedTeamName = ref('');
const activeWorkerDetails = ref([]);

const currentMonthVal = computed(() => dayjs(selectedMonth.value).month() + 1);
const currentYearVal = computed(() => dayjs(selectedMonth.value).year());

const daysInMonth = computed(() => {
  const date = dayjs(selectedMonth.value);
  const count = date.daysInMonth();
  return Array.from({ length: count }, (_, i) => i + 1);
});

const sortedTeamIds = computed(() => {
  return Object.keys(teams.value).sort((a, b) => teams.value[a].localeCompare(teams.value[b]));
});

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await $api.get(`/payrolls/${currentYearVal.value}/${currentMonthVal.value}/team-wages`);
    if (res.success) {
      matrixData.value = res.data;
      
      // Update team map
      const teamMap = {};
      res.data.forEach(item => {
        teamMap[item.teamId] = item.teamName;
      });
      teams.value = teamMap;
    }
  } catch (error) {
    console.error('Error fetching team wages:', error);
  } finally {
    loading.value = false;
  }
};

const getDayData = (teamId, day) => {
  const dateStr = dayjs(selectedMonth.value).date(day).format('YYYY-MM-DD');
  return matrixData.value.find(item => item.teamId == teamId && item.date === dateStr);
};

const getTeamName = (teamId) => teams.value[teamId] || 'Unknown';

const getDayName = (day) => {
  const date = dayjs(selectedMonth.value).date(day);
  const names = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
  return names[date.day()];
};

const isToday = (day) => {
  const today = dayjs();
  const current = dayjs(selectedMonth.value).date(day);
  return today.isSame(current, 'day');
};

const getTeamTotal = (teamId, field = 'totalTeamIncome') => {
  return matrixData.value
    .filter(item => item.teamId == teamId)
    .reduce((sum, item) => sum + (item[field] || 0), 0);
};

const showDetails = (teamId, day) => {
  const data = getDayData(teamId, day);
  if (!data) return;
  
  selectedTeamId.value = teamId;
  selectedTeamName.value = data.teamName;
  selectedDate.value = data.date;
  activeWorkerDetails.value = data.details || [];
  showDialog.value = true;
};


const formatMoney = (val) => {
  if (!val) return '0';
  return new Intl.NumberFormat('vi-VN').format(val);
};

const formatDateVi = (date) => {
  if (!date) return '';
  return dayjs(date).format('DD/MM/YYYY');
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  @apply bg-slate-200 dark:bg-slate-800;
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  @apply bg-slate-300 dark:bg-slate-700;
}
</style>
