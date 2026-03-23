<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-3xl font-black text-slate-900 tracking-tight">{{ $t('penalty_bonus.title') }}</h2>
        <p class="text-slate-500 font-medium">{{ $t('penalty_bonus.subtitle') }}</p>
      </div>
      <div class="flex gap-3">
        <NuxtLink to="/penalty-bonus-types">
          <UiButton variant="outline">
            <Settings class="w-4 h-4" />
            {{ $t('penalty_bonus.type_mgmt') }}
          </UiButton>
        </NuxtLink>
        <UiButton @click="openModal()">
          <PlusCircle class="w-4 h-4" />
          {{ $t('penalty_bonus.add_new') }}
        </UiButton>
      </div>
    </div>

    <!-- Tabs -->
    <div class="flex p-1 bg-slate-100 rounded-2xl w-fit">
      <button 
        @click="activeTab = 'list'"
        :class="['px-6 py-2.5 rounded-xl text-xs font-black uppercase tracking-widest transition-all', 
                 activeTab === 'list' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-400 hover:text-slate-600']"
      >
        {{ $t('penalty_bonus.list') }}
      </button>
      <button 
        @click="activeTab = 'summary'"
        :class="['px-6 py-2.5 rounded-xl text-xs font-black uppercase tracking-widest transition-all', 
                 activeTab === 'summary' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-400 hover:text-slate-600']"
      >
        {{ $t('penalty_bonus.summary') }}
      </button>
    </div>

    <!-- List Tab -->
    <div v-if="activeTab === 'list'" class="space-y-6">
      <div class="card p-4 bg-white/80 backdrop-blur-md border-none shadow-sm flex flex-col md:flex-row gap-4 items-end">
        <div class="flex-1 grid grid-cols-1 md:grid-cols-2 gap-4 w-full">
          <SelectDepartment v-model="filterDeptId" :label="$t('common.department')" />
          <SelectTeam v-model="filterTeamId" :departmentId="filterDeptId" :label="$t('common.team')" />
        </div>
        <div class="w-full md:w-auto self-end">
          <UiButton variant="outline" @click="resetFilters" class="w-full">
            Đặt lại
          </UiButton>
        </div>
      </div>

      <div class="card overflow-hidden">
        <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
          <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
          <p class="text-slate-500 font-bold">Đang tải...</p>
        </div>

        <table v-else class="w-full text-left">
          <thead>
            <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
              <th class="px-8 py-5">{{ $t('penalty_bonus.date') }}</th>
              <th class="px-8 py-5">{{ $t('penalty_bonus.employee') }}</th>
              <th class="px-8 py-5">{{ $t('penalty_bonus.type') }}</th>
              <th class="px-8 py-5">{{ $t('penalty_bonus.reason') }}</th>
              <th class="px-8 py-5">{{ $t('penalty_bonus.amount') }}</th>
              <th class="px-8 py-5 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="item in paginatedItems" :key="item.id" class="hover:bg-slate-50/50 transition-all group">
              <td class="px-8 py-5 text-sm font-bold text-slate-500">{{ item.recordDate }}</td>
              <td class="px-8 py-5 font-black text-slate-900">{{ item.employee?.fullName }}</td>
              <td class="px-8 py-5">
                <span v-if="item.type" class="px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider bg-slate-100 text-slate-600">
                  {{ item.type?.name }}
                </span>
                <span v-else class="px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider bg-slate-50 text-slate-400">
                  Khác
                </span>
              </td>
              <td class="px-8 py-5 text-sm font-medium text-slate-600 truncate max-w-[200px]">{{ item.reason }}</td>
              <td :class="`px-8 py-5 font-black ${item.amount > 0 ? 'text-emerald-700' : 'text-red-700'}`">
                {{ item.amount > 0 ? '+' : '' }}{{ item.amount.toLocaleString() }}đ
              </td>
              <td class="px-8 py-5 text-right">
                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button @click="openModal(item)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg"><PencilLine class="w-4 h-4" /></button>
                  <button @click="handleDelete(item.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg"><Trash2 class="w-4 h-4" /></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div v-if="filteredItems.length > 0" class="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
          <div class="flex items-center gap-4">
            <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Hiển thị</span>
            <select v-model="itemsPerPage" class="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-bold text-slate-600 focus:ring-2 focus:ring-primary-500 outline-none">
              <option :value="10">10 dòng</option>
              <option :value="20">20 dòng</option>
              <option :value="50">50 dòng</option>
            </select>
            <span class="text-xs font-bold text-slate-500">
              {{ (currentPage - 1) * itemsPerPage + 1 }}-{{ Math.min(currentPage * itemsPerPage, filteredItems.length) }} của {{ filteredItems.length }}
            </span>
          </div>
          <div class="flex items-center gap-2">
            <button @click="currentPage--" :disabled="currentPage === 1" class="p-2 rounded-lg bg-white border border-slate-200 text-slate-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 shadow-sm"><ChevronLeft class="w-4 h-4" /></button>
            <div class="flex items-center gap-1">
              <button v-for="p in totalPages" :key="p" @click="currentPage = p" :class="['w-8 h-8 rounded-lg flex items-center justify-center text-xs font-black transition-all', currentPage === p ? 'bg-primary-600 text-white shadow-lg' : 'bg-white border text-slate-600 hover:bg-slate-50 shadow-sm']">{{ p }}</button>
            </div>
            <button @click="currentPage++" :disabled="currentPage === totalPages" class="p-2 rounded-lg bg-white border border-slate-200 text-slate-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 shadow-sm"><ChevronRight class="w-4 h-4" /></button>
          </div>
        </div>
      </div>
    </div>

    <!-- Summary Tab -->
    <PenaltySummary v-else-if="activeTab === 'summary'" />

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-md p-10 animate-in zoom-in duration-300">
        <div class="flex items-center justify-between mb-10">
          <h3 class="text-2xl font-black text-slate-900">Chi tiết khoản thu/chi</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <SelectEmployee v-model="form.employeeId" label="Nhân viên" placeholder="Chọn nhân viên" required />
          
          <UiSelect 
            v-model="form.typeId" 
            label="Loại thưởng/phạt" 
            placeholder="Chọn loại (không bắt buộc)" 
            :options="bonusTypes"
            labelKey="name"
            valueKey="id"
            @update:modelValue="onTypeChange"
          />

          <UiInput v-model="form.recordDate" label="Ngày ghi nhận" type="date" required />
          <UiInput v-model.number="form.amount" label="Số tiền (VNĐ)" type="number" required />
          <UiInput v-model="form.reason" label="Lý do / Ghi chú" placeholder="Nhập lý do cụ thể..." required />
          
          <div class="flex gap-4 pt-6">
            <button type="button" @click="showModal = false" class="flex-1 py-3.5 rounded-2xl border border-slate-200 text-slate-500 font-black hover:bg-slate-50">Hủy</button>
            <UiButton type="submit" class="flex-[2] h-14" :loading="saving">Lưu bản ghi</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { PlusCircle, PencilLine, Trash2, X, ChevronLeft, ChevronRight, Settings } from 'lucide-vue-next';
import PenaltySummary from '~/components/PenaltySummary.vue';

const { $api } = useNuxtApp();
const activeTab = ref('list');
const items = ref([]);
const bonusTypes = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);
const currentId = ref(null);
const filterDeptId = ref('');
const filterTeamId = ref('');
const currentPage = ref(1);
const itemsPerPage = ref(10);

const form = reactive({
  employeeId: null,
  typeId: null,
  amount: 0,
  reason: '',
  recordDate: new Date().toISOString().substr(0, 10)
});

const resetFilters = () => {
  filterDeptId.value = '';
  filterTeamId.value = '';
};

const fetchData = async () => {
  loading.value = true;
  try {
    const [penaltyRes, typesRes] = await Promise.all([
      $api.get('/penalty-bonuses'),
      $api.get('/penalty-bonus-types')
    ]);
    items.value = penaltyRes.data || [];
    bonusTypes.value = typesRes.data || [];
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const onTypeChange = (typeId) => {
  if (!typeId) return;
  const type = bonusTypes.value.find(t => t.id == typeId);
  if (type) {
    form.amount = type.defaultAmount;
    form.reason = type.name;
  }
};

const filteredItems = computed(() => {
  return items.value.filter(item => {
    const matchDept = !filterDeptId.value || item.employee?.department?.id == filterDeptId.value;
    const matchTeam = !filterTeamId.value || item.employee?.team?.id == filterTeamId.value;
    return matchDept && matchTeam;
  });
});

const paginatedItems = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return filteredItems.value.slice(start, end);
});

const totalPages = computed(() => Math.ceil(filteredItems.value.length / itemsPerPage.value) || 1);

watch([filterDeptId, filterTeamId], () => {
  currentPage.value = 1;
});

const openModal = (item = null) => {
  if (item) {
    currentId.value = item.id;
    form.employeeId = item.employee?.id;
    form.typeId = item.type?.id || null;
    form.amount = item.amount;
    form.reason = item.reason;
    form.recordDate = item.recordDate;
  } else {
    currentId.value = null;
    form.employeeId = null;
    form.typeId = null;
    form.amount = 0;
    form.reason = '';
    form.recordDate = new Date().toISOString().substr(0, 10);
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    if (currentId.value) {
      await $api.put(`/penalty-bonuses/${currentId.value}`, form);
    } else {
      await $api.post('/penalty-bonuses', form);
    }
    showModal.value = false;
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message);
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Xóa bản ghi này?')) return;
  try {
    await $api.delete(`/penalty-bonuses/${id}`);
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message);
  }
};

onMounted(fetchData);
</script>
