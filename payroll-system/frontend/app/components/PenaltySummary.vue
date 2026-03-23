<template>
  <div class="space-y-6">
    <!-- Filters -->
    <div class="card p-6 bg-white shadow-sm border-none flex flex-col md:flex-row gap-6 items-end">
      <div class="flex-1 grid grid-cols-1 md:grid-cols-4 gap-4 w-full">
        <div class="flex flex-col gap-1.5">
          <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Chế độ xem</label>
          <select v-model="viewMode" class="input-field py-3">
            <option value="month">Theo Tháng</option>
            <option value="quarter">Theo Quý</option>
            <option value="year">Theo Năm</option>
          </select>
        </div>

        <div v-if="viewMode === 'month'" class="flex flex-col gap-1.5">
          <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Tháng/Năm</label>
          <div class="flex gap-2">
            <select v-model="selectedMonth" class="input-field py-3 flex-1">
              <option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</option>
            </select>
            <select v-model="selectedYear" class="input-field py-3 flex-1">
              <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
            </select>
          </div>
        </div>

        <div v-if="viewMode === 'quarter'" class="flex flex-col gap-1.5">
          <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Quý/Năm</label>
          <div class="flex gap-2">
            <select v-model="selectedQuarter" class="input-field py-3 flex-1">
              <option v-for="q in 4" :key="q" :value="q">Quý {{ q }}</option>
            </select>
            <select v-model="selectedYear" class="input-field py-3 flex-1">
              <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
            </select>
          </div>
        </div>

        <div v-if="viewMode === 'year'" class="flex flex-col gap-1.5">
          <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Năm</label>
          <select v-model="selectedYear" class="input-field py-3">
            <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
          </select>
        </div>

        <SelectDepartment v-model="filterDeptId" label="Phòng ban" />
        <SelectTeam v-model="filterTeamId" :departmentId="filterDeptId" label="Tổ sản xuất" />
      </div>
      
      <UiButton @click="fetchSummary" :loading="loading" class="w-full md:w-auto px-8">
        Lấy dữ liệu
      </UiButton>
    </div>

    <!-- Summary Table -->
    <div class="card overflow-hidden min-h-[400px]">
      <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
        <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold">Đang tổng hợp dữ liệu...</p>
      </div>

      <div v-else-if="summaryData.length === 0" class="p-20 text-center space-y-4 text-slate-400">
        <BarChart3 class="w-16 h-16 mx-auto opacity-20" />
        <p class="font-bold uppercase tracking-widest text-xs">Không có dữ liệu thưởng phạt trong khoảng thời gian này</p>
      </div>

      <div v-else class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
              <th class="px-6 py-4">Nhân viên</th>
              <th class="px-6 py-4">Phòng / Tổ</th>
              <th class="px-6 py-4 text-center">Lần Thưởng</th>
              <th class="px-6 py-4 text-center">Lần Phạt</th>
              <th class="px-6 py-4 text-right">Tổng Thưởng</th>
              <th class="px-6 py-4 text-right">Tổng Phạt</th>
              <th class="px-6 py-4 text-right">Thực lãnh/Khấu trừ</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 italic-last-row">
            <tr v-for="row in summaryData" :key="row.employeeId" class="hover:bg-slate-50/50 transition-colors">
              <td class="px-6 py-4">
                <div class="flex flex-col">
                  <span class="font-black text-slate-900 leading-tight">{{ row.employeeName }}</span>
                  <span class="text-[10px] font-bold text-slate-400 uppercase tracking-tighter">{{ row.employeeCode }}</span>
                </div>
              </td>
              <td class="px-6 py-4">
                <div class="flex flex-col text-xs font-medium text-slate-500">
                  <span>{{ row.departmentName }}</span>
                  <span class="text-[10px] opacity-70">{{ row.teamName }}</span>
                </div>
              </td>
              <td class="px-6 py-4 text-center">
                <span class="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 font-black text-[10px]">{{ row.bonusCount }}</span>
              </td>
              <td class="px-6 py-4 text-center">
                <span class="px-2 py-0.5 rounded-full bg-red-50 text-red-600 font-black text-[10px]">{{ row.penaltyCount }}</span>
              </td>
              <td class="px-6 py-4 text-right font-bold text-emerald-600 text-sm">
                +{{ row.totalBonus.toLocaleString() }}đ
              </td>
              <td class="px-6 py-4 text-right font-bold text-red-600 text-sm">
                {{ row.totalPenalty.toLocaleString() }}đ
              </td>
              <td class="px-6 py-4 text-right">
                <span :class="['font-black text-base', row.netAmount >= 0 ? 'text-emerald-700' : 'text-red-700']">
                  {{ row.netAmount > 0 ? '+' : '' }}{{ row.netAmount.toLocaleString() }}đ
                </span>
              </td>
            </tr>
          </tbody>
          <tfoot class="bg-slate-900 text-white font-black uppercase tracking-widest text-[10px]">
            <tr>
              <td colspan="2" class="px-6 py-4 text-right">Tổng cộng</td>
              <td class="px-6 py-4 text-center">{{ totals.bonusCount }}</td>
              <td class="px-6 py-4 text-center">{{ totals.penaltyCount }}</td>
              <td class="px-6 py-4 text-right">+{{ totals.bonusAmount.toLocaleString() }}đ</td>
              <td class="px-6 py-4 text-right">{{ totals.penaltyAmount.toLocaleString() }}đ</td>
              <td class="px-6 py-4 text-right text-sm">
                {{ totals.netAmount > 0 ? '+' : '' }}{{ totals.netAmount.toLocaleString() }}đ
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { BarChart3 } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const loading = ref(false);
const summaryData = ref([]);
const viewMode = ref('month');
const selectedMonth = ref(new Date().getMonth() + 1);
const selectedQuarter = ref(Math.floor(new Date().getMonth() / 3) + 1);
const selectedYear = ref(new Date().getFullYear());
const filterDeptId = ref('');
const filterTeamId = ref('');

const yearOptions = computed(() => {
  const current = new Date().getFullYear();
  return Array.from({ length: 5 }, (_, i) => current - 2 + i);
});

const totals = computed(() => {
  return summaryData.value.reduce((acc, curr) => {
    acc.bonusCount += curr.bonusCount;
    acc.penaltyCount += curr.penaltyCount;
    acc.bonusAmount += curr.totalBonus;
    acc.penaltyAmount += curr.totalPenalty;
    acc.netAmount += curr.netAmount;
    return acc;
  }, { bonusCount: 0, penaltyCount: 0, bonusAmount: 0, penaltyAmount: 0, netAmount: 0 });
});

const fetchSummary = async () => {
  loading.value = true;
  let startDate, endDate;

  if (viewMode.value === 'month') {
    startDate = `${selectedYear.value}-${String(selectedMonth.value).padStart(2, '0')}-01`;
    endDate = new Date(selectedYear.value, selectedMonth.value, 0).toISOString().split('T')[0];
  } else if (viewMode.value === 'quarter') {
    const startMonth = (selectedQuarter.value - 1) * 3 + 1;
    startDate = `${selectedYear.value}-${String(startMonth).padStart(2, '0')}-01`;
    endDate = new Date(selectedYear.value, startMonth + 2, 0).toISOString().split('T')[0];
  } else {
    startDate = `${selectedYear.value}-01-01`;
    endDate = `${selectedYear.value}-12-31`;
  }

  try {
    const res = await $api.get('/penalty-bonuses/summary', {
      params: {
        startDate,
        endDate,
        departmentId: filterDeptId.value || undefined,
        teamId: filterTeamId.value || undefined
      }
    });
    summaryData.value = res.data || [];
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchSummary);
</script>
