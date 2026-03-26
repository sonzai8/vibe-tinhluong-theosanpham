<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-8 rounded-3xl shadow-sm border border-slate-100">
      <div>
        <h1 class="text-3xl font-black text-slate-900 tracking-tight italic">{{ $t('salary_processes.title') }}</h1>
        <p class="text-slate-400 font-medium mt-1">{{ $t('salary_processes.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-3">
        <UiButton variant="primary" size="lg" @click="showAddModal = true">
          <Plus class="w-5 h-5 mr-2" />
          {{ $t('salary_processes.add_new') }}
        </UiButton>
        <UiButton variant="outline" size="lg" @click="fetchData">
          <RefreshCcw class="w-5 h-5 mr-2" />
          {{ $t('common.refresh') }}
        </UiButton>
      </div>
    </div>

    <!-- Common Error Modal -->
    <UiErrorModal
      :show="showErrorModal"
      :title="errorTitle"
      :message="errorMessage"
      :detail="errorDetail"
      @close="showErrorModal = false"
    />

    <!-- Common Confirm Modal -->
    <UiConfirmModal
      :show="confirmModal.show"
      :title="confirmModal.title"
      :message="confirmModal.message"
      :variant="confirmModal.variant"
      :loading="confirmModal.loading"
      @confirm="confirmModal.onConfirm"
      @cancel="confirmModal.show = false"
    />

    <!-- Filters -->
    <div class="bg-white p-6 rounded-3xl shadow-sm border border-slate-100 flex flex-wrap items-center gap-4">
      <div class="flex-1 min-w-[300px]">
        <UiInput 
          v-model="filters.search" 
          :placeholder="$t('salary_processes.filters.search')"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <Search class="w-4 h-4 text-slate-400" />
          </template>
        </UiInput>
      </div>
      
      <div class="w-64">
        <SelectTeam v-model="filters.teamId" />
      </div>

      <div class="w-64">
        <SelectEmployee v-model="filters.employeeId" />
      </div>

      <UiButton variant="secondary" @click="handleSearch">
        {{ $t('common.filter') }}
      </UiButton>
    </div>

    <!-- Table -->
    <div class="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-slate-50/50 border-b border-slate-100">
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('salary_processes.table.employee') }}</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('salary_processes.table.team') }}</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('salary_processes.table.type') }}</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('salary_processes.table.base') }}</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('salary_processes.table.insurance') }}</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('salary_processes.table.start') }}</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('salary_processes.table.end') }}</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest text-center">{{ $t('salary_processes.table.active') }}</th>
              <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest text-right">{{ $t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-50">
            <template v-if="loading">
              <tr v-for="i in 5" :key="i">
                <td colspan="9" class="px-6 py-8">
                  <div class="h-6 bg-slate-100 rounded-lg animate-pulse"></div>
                </td>
              </tr>
            </template>
            <template v-else-if="items.length === 0">
              <tr>
                <td colspan="9" class="px-6 py-20 text-center">
                  <div class="flex flex-col items-center gap-2">
                    <FileQuestion class="w-12 h-12 text-slate-200" />
                    <p class="text-slate-400 font-bold">{{ $t('common.no_data') }}</p>
                  </div>
                </td>
              </tr>
            </template>
            <tr v-for="item in items" :key="item.id" class="hover:bg-slate-50/50 transition-colors group">
              <td class="px-6 py-4">
                <div class="flex flex-col">
                  <span class="font-bold text-slate-900">{{ item.employeeName }}</span>
                  <span class="text-[10px] font-black text-primary-600 uppercase tracking-wider">{{ item.employeeCode }}</span>
                </div>
              </td>
              <td class="px-6 py-4">
                <span class="inline-flex items-center px-2.5 py-1 rounded-lg bg-slate-100 text-slate-600 text-xs font-bold">
                  {{ item.teamName || $t('common.no_team_assigned') }}
                </span>
              </td>
              <td class="px-6 py-4">
                <select 
                  v-model="item.salaryType" 
                  class="bg-transparent border-0 font-bold text-sm focus:ring-0 cursor-pointer p-0"
                  @change="quickUpdate(item)"
                >
                  <option value="PRODUCT_BASED">{{ $t('employee.salary_type_product') }}</option>
                  <option value="FIXED_MONTHLY">{{ $t('employee.salary_type_monthly') }}</option>
                  <option value="FIXED_DAILY">{{ $t('employee.salary_type_daily') }}</option>
                </select>
              </td>
              <td class="px-6 py-4">
                <input 
                  v-model.number="item.baseSalary" 
                  type="number" 
                  class="w-32 bg-transparent border-b border-transparent focus:border-primary-500 focus:ring-0 font-bold text-sm p-0"
                  @blur="quickUpdate(item)"
                />
              </td>
              <td class="px-6 py-4">
                <input 
                  v-model.number="item.insuranceSalary" 
                  type="number" 
                  class="w-32 bg-transparent border-b border-transparent focus:border-primary-500 focus:ring-0 font-bold text-sm p-0"
                  @blur="quickUpdate(item)"
                />
              </td>
              <td class="px-6 py-4">
                <input 
                  v-model="item.startDate" 
                  type="date" 
                  class="bg-transparent border-0 font-medium text-xs focus:ring-0 p-0"
                  @change="quickUpdate(item)"
                />
              </td>
              <td class="px-6 py-4">
                <input 
                  v-model="item.endDate" 
                  type="date" 
                  class="bg-transparent border-0 font-medium text-xs focus:ring-0 p-0"
                  @change="quickUpdate(item)"
                />
              </td>
              <td class="px-6 py-4 text-center">
                <div :class="['w-2.5 h-2.5 rounded-full mx-auto', (item.isCurrent || item.current) ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]' : 'bg-slate-200']"></div>
              </td>
              <td class="px-6 py-4 text-right">
                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button 
                    class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-xl transition-all"
                    :title="$t('common.edit')"
                    @click="openEditModal(item)"
                  >
                    <Edit2 class="w-4 h-4" />
                  </button>
                  <button 
                    v-if="item.isCurrent || item.current"
                    class="p-2 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-xl transition-all"
                    :title="$t('salary_processes.table.deactivate')"
                    @click="handleDeactivate(item)"
                  >
                    <XCircle class="w-4 h-4" />
                  </button>
                  <button 
                    class="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-all"
                    :title="$t('common.delete')"
                    @click="handleDelete(item)"
                  >
                    <Trash2 class="w-4 h-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="px-6 py-4 border-t border-slate-100 flex items-center justify-between">
        <p class="text-xs font-bold text-slate-400 italic">
          {{ $t('common.displaying') }} {{ items.length }} {{ $t('common.rows') }} {{ $t('common.of') }} {{ totalElements }}
        </p>
        <div class="flex items-center gap-2">
          <UiButton 
            variant="outline" 
            size="sm" 
            :disabled="page === 0"
            @click="page--; fetchData()"
          >
            {{ $t('common.prev') }}
          </UiButton>
          <div class="flex items-center gap-1">
            <span class="text-xs font-black text-slate-900">{{ page + 1 }}</span>
            <span class="text-xs font-bold text-slate-400">/</span>
            <span class="text-xs font-bold text-slate-400">{{ totalPages }}</span>
          </div>
          <UiButton 
            variant="outline" 
            size="sm" 
            :disabled="page >= totalPages - 1"
            @click="page++; fetchData()"
          >
            {{ $t('common.next') }}
          </UiButton>
        </div>
      </div>
    </div>

    <!-- Add Modal -->
    <div v-if="showAddModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="bg-white w-full max-w-2xl p-10 rounded-3xl shadow-2xl animate-in zoom-in duration-300 overflow-y-auto max-h-[90vh]">
        <div class="flex items-center justify-between mb-8">
          <h3 class="text-2xl font-black text-slate-900">{{ $t('salary_processes.add_new') }}</h3>
          <button @click="showAddModal = false" class="p-2 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="md:col-span-2">
              <SelectEmployee v-model="form.employeeId" :label="$t('salary_processes.table.employee')" />
            </div>
            
            <div>
              <UiSelect 
                v-model="form.salaryType"
                :label="$t('salary_processes.table.type')"
                :options="[
                  { value: 'PRODUCT_BASED', label: $t('employee.salary_type_product') },
                  { value: 'FIXED_MONTHLY', label: $t('employee.salary_type_monthly') },
                  { value: 'FIXED_DAILY', label: $t('employee.salary_type_daily') }
                ]"
              />
            </div>

            <UiInput v-model.number="form.baseSalary" type="number" :label="$t('salary_processes.table.base')" />
            <UiInput v-model.number="form.insuranceSalary" type="number" :label="$t('salary_processes.table.insurance')" />
            <UiInput v-model="form.startDate" type="date" :label="$t('salary_processes.table.start')" />
            <UiInput v-model="form.endDate" type="date" :label="$t('salary_processes.table.end')" />
          </div>

          <div class="flex justify-end gap-3 pt-6 border-t border-slate-50">
            <UiButton variant="outline" type="button" @click="showAddModal = false">{{ $t('common.cancel') }}</UiButton>
            <UiButton variant="primary" type="submit" :loading="submitting">{{ $t('common.save') }}</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { 
  Plus, RefreshCcw, Search, Filter, FileQuestion, 
  MoreVertical, Edit2, History, X, Trash2, XCircle, AlertTriangle
} from 'lucide-vue-next';

const { $api } = useNuxtApp();
const { t } = useI18n();

const loading = ref(false);
const submitting = ref(false);
const showAddModal = ref(false);
const items = ref([]);
const totalElements = ref(0);
const totalPages = ref(0);
const page = ref(0);
const size = ref(20);

// Error Modal State
const showErrorModal = ref(false);
const errorTitle = ref('');
const errorMessage = ref('');
const errorDetail = ref('');

const triggerError = (title, message, detail = '') => {
  errorTitle.value = title;
  errorMessage.value = message;
  errorDetail.value = detail;
  showErrorModal.value = true;
};

// Confirm Modal State
const confirmModal = reactive({
  show: false,
  title: '',
  message: '',
  variant: 'primary',
  loading: false,
  onConfirm: () => {}
});

const triggerConfirm = (title, message, onConfirm, variant = 'primary') => {
  confirmModal.title = title;
  confirmModal.message = message;
  confirmModal.onConfirm = onConfirm;
  confirmModal.variant = variant;
  confirmModal.show = true;
};

const filters = ref({
  search: '',
  teamId: null,
  employeeId: null
});

const today = new Date().toISOString().split('T')[0];
const nextYear = new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString().split('T')[0];

const form = ref({
  employeeId: null,
  salaryType: 'PRODUCT_BASED',
  baseSalary: 0,
  insuranceSalary: 0,
  startDate: today,
  endDate: nextYear
});

const fetchData = async () => {
  loading.value = true;
  try {
    const params = {
      page: page.value,
      size: size.value,
      search: filters.value.search || undefined,
      teamId: filters.value.teamId || undefined,
      employeeId: filters.value.employeeId || undefined
    };
    
    const res = await $api.get('/salary-processes', { params });
    if (res.success) {
      items.value = res.data.content;
      totalElements.value = res.data.totalElements;
      totalPages.value = res.data.totalPages;
    }
  } catch (e) {
    triggerError('Lỗi tải dữ liệu', 'Không thể lấy danh sách quy trình lương.', e.message);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 0;
  fetchData();
};

const handleSubmit = async () => {
  if (form.value.id) {
    await handleUpdate();
  } else {
    await handleCreate();
  }
};

const handleCreate = async () => {
  if (!form.value.employeeId) return alert('Vui lòng chọn nhân viên');
  
  // Kiểm tra xem nhân viên đã có bản ghi active chưa (trong data hiện tại)
  const hasActive = items.value.some(base => base.employeeId === form.value.employeeId && (base.isCurrent || base.current));
  
  if (hasActive) {
    triggerConfirm(
      t('common.confirm_title'),
      t('salary_processes.warning.active_exists'),
      () => {
        confirmModal.show = false;
        executeCreate();
      }
    );
    return;
  }

  executeCreate();
};

const executeCreate = async () => {
  submitting.value = true;
  try {
    const res = await $api.post('/salary-processes', form.value);
    if (res.success) {
      showAddModal.value = false;
      fetchData();
      // Reset form
      form.value = {
        employeeId: null,
        salaryType: 'PRODUCT_BASED',
        baseSalary: 0,
        insuranceSalary: 0,
        startDate: today,
        endDate: nextYear
      };
    }
  } catch (e) {
    triggerError('Lỗi khi thêm mới', 'Đã xảy ra lỗi khi tạo quy trình lương.', e.response?.data?.message || e.message);
  } finally {
    submitting.value = false;
  }
};

const handleUpdate = async () => {
  submitting.value = true;
  try {
    const res = await $api.put(`/salary-processes/${form.value.id}`, form.value);
    if (res.success) {
      showAddModal.value = false;
      fetchData();
    }
  } catch (e) {
    triggerError('Lỗi khi cập nhật', 'Không thể lưu thay đổi.', e.response?.data?.message || e.message);
  } finally {
    submitting.value = false;
  }
};

const quickUpdate = async (item) => {
  try {
    const res = await $api.put(`/salary-processes/${item.id}`, {
      salaryType: item.salaryType,
      baseSalary: item.baseSalary,
      insuranceSalary: item.insuranceSalary,
      startDate: item.startDate,
      endDate: item.endDate
    });
    if (res.success) {
      fetchData();
    }
  } catch (e) {
    triggerError('Lỗi khi cập nhật', 'Hệ thống gặp sự cố khi cập nhật quy trình lương.', e.response?.data?.message || e.message);
  }
};

const handleDelete = (item) => {
  triggerConfirm(
    t('common.confirm_title'),
    t('salary_processes.warning.delete_confirm'),
    async () => {
      confirmModal.loading = true;
      try {
        const res = await $api.delete(`/salary-processes/${item.id}`);
        if (res.success) {
          confirmModal.show = false;
          fetchData();
        }
      } catch (e) {
        triggerError('Lỗi khi xóa', 'Không thể xóa bản ghi quy trình lương.', e.response?.data?.message || e.message);
      } finally {
        confirmModal.loading = false;
      }
    },
    'danger'
  );
};

const handleDeactivate = (item) => {
  triggerConfirm(
    t('common.confirm_title'),
    t('salary_processes.warning.deactivate_confirm'),
    async () => {
      confirmModal.loading = true;
      try {
        const yesterday = new Date(new Date().setDate(new Date().getDate() - 1)).toISOString().split('T')[0];
        const res = await $api.put(`/salary-processes/${item.id}`, {
          ...item,
          endDate: yesterday
        });
        if (res.success) {
          confirmModal.show = false;
          fetchData();
        }
      } catch (e) {
        triggerError('Lỗi khi dừng áp dụng', 'Không thể cập nhật trạng thái bản ghi.', e.response?.data?.message || e.message);
      } finally {
        confirmModal.loading = false;
      }
    }
  );
};

const openEditModal = (item) => {
  form.value = { ...item };
  showAddModal.value = true;
};

onMounted(() => {
  fetchData();
});
</script>
