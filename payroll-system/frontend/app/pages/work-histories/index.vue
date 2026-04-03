<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-black text-slate-900 tracking-tight">{{ $t('menu.work_histories') }}</h1>
        <p class="text-slate-500 text-sm font-medium mt-1">Quản lý quá trình công tác và điều chuyển nhân sự giữa các tổ</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card p-6 flex flex-wrap items-center gap-4 shadow-sm border border-slate-100">
      <div class="flex-1 min-w-[300px]">
        <UiInput v-model="filters.search" :placeholder="$t('common.search_placeholder')" @input="debouncedSearch">
          <template #prefix>
            <Search class="w-4 h-4 text-slate-400" />
          </template>
        </UiInput>
      </div>
      <div class="w-64">
        <SelectDepartment v-model="filters.departmentId" @update:modelValue="fetchEmployees" />
      </div>
    </div>

    <!-- Employee List -->
    <div class="card overflow-hidden border border-slate-100 shadow-sm transition-all duration-300">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-slate-50/50 border-b border-slate-100">
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Trạng thái</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Mã NV</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Họ và tên</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Phòng ban hiện tại</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Tổ hiện tại</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest text-right">{{ $t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-50">
            <tr v-if="loading" v-for="i in 5" :key="i" class="animate-pulse">
               <td colspan="6" class="px-6 py-4"><div class="h-4 bg-slate-100 rounded w-full"></div></td>
            </tr>
            <tr v-else-if="employees.length === 0">
               <td colspan="6" class="px-6 py-12 text-center text-slate-400 font-medium italic">{{ $t('common.no_data') }}</td>
            </tr>
            <tr v-for="emp in paginatedEmployees" :key="emp.id" class="hover:bg-slate-50/50 transition-colors group">
              <td class="px-6 py-4">
                <span :class="`px-2 py-0.5 rounded-lg text-[9px] font-black uppercase tracking-widest ${emp.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-600 border border-emerald-100' : 'bg-slate-50 text-slate-400 border border-slate-100'}`">
                  {{ emp.status === 'ACTIVE' ? 'Hoạt động' : 'Ngưng' }}
                </span>
              </td>
              <td class="px-6 py-4">
                <span class="px-2 py-1 bg-slate-100 text-slate-600 text-[10px] font-black rounded-lg border border-slate-200">
                  {{ emp.code }}
                </span>
              </td>
              <td class="px-6 py-4">
                <p class="text-sm font-black text-slate-900">{{ emp.fullName }}</p>
              </td>
              <td class="px-6 py-4 text-xs font-bold text-slate-600">
                {{ emp.departmentName || '---' }}
              </td>
              <td class="px-6 py-4">
                 <span class="px-3 py-1 bg-emerald-50 text-emerald-600 text-[10px] font-black rounded-full border border-emerald-100 uppercase tracking-tight">
                    {{ emp.teamName || '---' }}
                 </span>
              </td>
              <td class="px-6 py-4 text-right">
                <NuxtLink :to="`/work-histories/${emp.id}`" class="inline-flex items-center px-4 py-2 bg-primary-50 text-primary-600 text-xs font-black rounded-xl border border-primary-100 hover:bg-primary-100 transition-all shadow-sm">
                  <History class="w-4 h-4 mr-2" />
                  Xem lịch sử & điều chuyển
                </NuxtLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="employees.length > 0" class="p-4 bg-slate-50/50 border-t border-slate-100 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Hiển thị</span>
          <select v-model="itemsPerPage" class="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-bold text-slate-600 focus:ring-2 focus:ring-primary-500 outline-none">
            <option :value="10">10 dòng</option>
            <option :value="20">20 dòng</option>
            <option :value="50">50 dòng</option>
          </select>
          <span class="text-xs font-bold text-slate-500">
            {{ (currentPage - 1) * itemsPerPage + 1 }}-{{ Math.min(currentPage * itemsPerPage, employees.length) }} trên {{ employees.length }}
          </span>
        </div>
        <div class="flex items-center gap-2">
          <button 
            @click="currentPage--" 
            :disabled="currentPage === 1"
            class="p-2 rounded-lg bg-white border border-slate-200 text-slate-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 transition-all shadow-sm"
          >
            <ChevronLeft class="w-4 h-4" />
          </button>
          <div class="flex items-center gap-1">
            <button 
              v-for="p in totalPages" 
              :key="p"
              @click="currentPage = p"
              :class="['w-8 h-8 rounded-lg flex items-center justify-center text-xs font-black transition-all', 
                       currentPage === p ? 'bg-primary-600 text-white shadow-lg shadow-primary-200' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 shadow-sm']"
            >
              {{ p }}
            </button>
          </div>
          <button 
            @click="currentPage++" 
            :disabled="currentPage === totalPages"
            class="p-2 rounded-lg bg-white border border-slate-200 text-slate-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 transition-all shadow-sm"
          >
            <ChevronRight class="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- Error Modal Aria -->
    <UiErrorModal
      :show="showError"
      title="Lỗi xử lý"
      :message="errorMessage"
      @close="showError = false"
    />
  </div>
</template>

<script setup>
import { Search, History, ChevronLeft, ChevronRight } from 'lucide-vue-next';
import _ from 'lodash';

const { $api } = useNuxtApp();
const employees = ref([]);
const loading = ref(false);
const filters = reactive({
  search: '',
  departmentId: null
});

// Pagination state
const currentPage = ref(1);
const itemsPerPage = ref(10);
const totalPages = computed(() => Math.ceil(employees.value.length / itemsPerPage.value) || 1);

const paginatedEmployees = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return employees.value.slice(start, end);
});

const showError = ref(false);
const errorMessage = ref('');

const fetchEmployees = async () => {
  loading.value = true;
  currentPage.value = 1; // Reset to first page on search/filter
  try {
    // Backend GET /employees chỉ hỗ trợ param 'search', trả về flat List
    const params = {};
    if (filters.search && filters.search.trim().length >= 3) {
      params.search = filters.search.trim();
    }
    const res = await $api.get('/employees', { params });
    let allEmployees = res.data || [];

    if (filters.departmentId) {
      allEmployees = allEmployees.filter(e => e.departmentId == filters.departmentId);
    }

    employees.value = allEmployees;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const debouncedSearch = _.debounce(fetchEmployees, 500);

onMounted(fetchEmployees);
</script>
