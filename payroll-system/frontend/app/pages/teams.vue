<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Quản lý Tổ sản xuất</h2>
        <p class="text-slate-500 font-medium">Danh sách các tổ, đội sản xuất trong xưởng</p>
      </div>
      <div class="flex items-center gap-2">
        <UiButton variant="outline" @click="handleExport">
          <Download class="w-4 h-4" />
          Xuất Excel
        </UiButton>
        <div class="relative group">
          <UiButton variant="outline">
            <Upload class="w-4 h-4" />
            Nhập Excel
          </UiButton>
          <div class="absolute right-0 top-full mt-2 w-48 bg-white border border-slate-200 rounded-xl shadow-xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50 p-2">
            <button @click="downloadTemplate" class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2">
              <FileDown class="w-4 h-4" />
              Tải file mẫu
            </button>
            <label class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2 cursor-pointer">
              <FileUp class="w-4 h-4" />
              Chọn file nhập
              <input type="file" class="hidden" accept=".xlsx, .xls" @change="handleImport" />
            </label>
          </div>
        </div>
        <UiButton @click="openModal()">
          <Plus class="w-4 h-4" />
          Thêm tổ mới
        </UiButton>
      </div>
    <!-- Common Error Modal -->
    <UiErrorModal
      :show="showErrorModal"
      :title="errorTitle"
      :message="errorMessage"
      :detail="errorDetail"
      @close="showErrorModal = false"
    />

    <!-- Import Preview Dialog -->
    <ImportPreviewDialog
      :show="showImportPreview"
      :data="previewData"
      :errors="importErrors"
      :loading="importing"
      :columns="importCols"
      title="Xem trước nhập tổ đội"
      @close="showImportPreview = false"
      @confirm="handleConfirmImport"
    />
    </div>

    <!-- Filters & Stats -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
      <div class="md:col-span-1 card p-4 flex flex-col gap-1.5">
        <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-1">Lọc theo phòng ban</label>
        <SelectDepartment 
          v-model="filterDeptId" 
          :placeholder="$t('employee.all_departments') || 'Tất cả phòng ban'" 
        />
      </div>
      <div class="md:col-span-3 card p-6 bg-primary-600 text-white shadow-xl shadow-primary-50 flex items-center gap-4">
        <div class="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center">
          <Users2 class="w-6 h-6" />
        </div>
        <div>
          <p class="text-xs font-bold text-primary-100 uppercase tracking-widest">Tổng số tổ đang hiển thị</p>
          <h3 class="text-2xl font-black">{{ filteredTeams.length }} / {{ teams.length }}</h3>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">Đang tải dữ liệu...</p>
      </div>

      <table v-else class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-6 py-4">ID</th>
            <th class="px-6 py-4">Tên tổ</th>
            <th class="px-6 py-4">Phòng ban</th>
            <th class="px-6 py-4">Công đoạn sản xuất</th>
            <th class="px-6 py-4 text-center">Thành viên</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="team in paginatedTeams" :key="team.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4 text-sm font-black text-slate-400">#{{ team.id }}</td>
            <td class="px-6 py-4">
              <NuxtLink :to="`/employees?teamId=${team.id}`" class="font-bold text-slate-900 hover:text-primary-600 flex items-center gap-2 group/link">
                {{ team.name }}
                <ChevronRight class="w-3 h-3 opacity-0 group-hover/link:opacity-100 -translate-x-2 group-hover/link:translate-x-0 transition-all" />
              </NuxtLink>
            </td>
            <td class="px-6 py-4">
              <span class="text-sm font-bold text-slate-600">{{ team.departmentName || '---' }}</span>
            </td>
            <td class="px-6 py-4">
              <span class="px-2.5 py-1 rounded-full bg-primary-50 text-primary-600 text-[10px] font-black uppercase tracking-wider">
                {{ team.productionStepName || '---' }}
              </span>
            </td>
            <td class="px-6 py-4 text-center">
              <span v-if="team.memberCount > 0" 
                class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-100 text-slate-600 font-black text-[10px] cursor-help relative group/tooltip"
                :title="team.memberNames?.join(', ')"
              >
                {{ team.memberCount }} người
                <!-- Custom Tooltip -->
                <div class="absolute top-full left-1/2 -translate-x-1/2 mt-2 w-48 bg-slate-900 text-white p-3 rounded-xl text-[10px] leading-relaxed shadow-xl opacity-0 invisible group-hover/tooltip:opacity-100 group-hover/tooltip:visible transition-all z-50 text-left">
                  <p class="font-black border-b border-white/10 pb-1.5 mb-1.5 uppercase tracking-widest text-white/50">Thành viên trong tổ:</p>
                  <div class="max-h-40 overflow-y-auto pr-1 flex flex-col gap-1">
                    <p v-for="name in team.memberNames" :key="name" class="flex items-center gap-1.5 py-0.5 font-bold">
                      <span class="w-1 h-1 rounded-full bg-primary-400"></span>
                      {{ name }}
                    </p>
                  </div>
                  <div class="absolute bottom-full left-1/2 -translate-x-1/2 border-8 border-transparent border-b-slate-900"></div>
                </div>
              </span>
              <span v-else class="text-[10px] font-black text-slate-300 uppercase italic">Chưa có thành viên</span>
            </td>
            <td class="px-6 py-4 text-right pr-6">
              <div class="flex items-center justify-end gap-2 text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(team)" class="p-2 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" title="Sửa">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(team.id)" class="p-2 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" title="Xóa">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="filteredTeams.length > 0" class="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Hiển thị</span>
          <select v-model="itemsPerPage" class="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-bold text-slate-600 focus:ring-2 focus:ring-primary-500 outline-none">
            <option :value="10">10 dòng</option>
            <option :value="20">20 dòng</option>
            <option :value="50">50 dòng</option>
          </select>
          <span class="text-xs font-bold text-slate-500">
            {{ (currentPage - 1) * itemsPerPage + 1 }}-{{ Math.min(currentPage * itemsPerPage, filteredTeams.length) }} của {{ filteredTeams.length }}
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

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-md p-8 animate-in zoom-in duration-200">
        <div class="flex items-center justify-between mb-8">
          <h3 class="text-xl font-black text-slate-900">{{ currentTeam.id ? 'Cập nhật' : 'Thêm' }} tổ sản xuất</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <SelectDepartment
            v-model="form.departmentId"
            label="Phòng ban"
            placeholder="Chọn phòng ban..."
            :allowAll="false"
          />
          <UiSelect
            v-model="form.productionStepId"
            label="Công đoạn sản xuất"
            :options="stepOptions"
            placeholder="Chọn công đoạn..."
            required
          />
          <UiInput v-model="form.name" label="Tên tổ" placeholder="VD: Tổ Sấy Phôi" required />
          
          <div class="flex gap-3 pt-2">
            <button type="button" @click="showModal = false" class="flex-1 py-2.5 rounded-lg border border-slate-200 text-slate-600 font-bold hover:bg-slate-50 transition-all">Hủy</button>
            <UiButton type="submit" class="flex-1" :loading="saving">Lưu lại</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, Users2, PencilLine, Trash2, X, ChevronRight, ChevronLeft, Download, Upload, FileDown, FileUp } from 'lucide-vue-next';

const route = useRoute();
const { $api } = useNuxtApp();
const { downloadTemplate: dlTemplate, importExcel, exportExcel } = useExcel();
const teams = ref([]);
const productionSteps = ref([]);
const showModal = ref(false);
const loading = ref(true);
const saving = ref(false);
// Error Modal State
const showErrorModal = ref(false);
const errorTitle = ref('');
const errorMessage = ref('');
const errorDetail = ref('');

// Import Preview State
const showImportPreview = ref(false);
const previewData = ref([]);
const importErrors = ref([]);
const importing = ref(false);

const importCols = [
  { label: 'Tên tổ đội', key: 'name' },
];

const triggerError = (title, message, detail = '') => {
  errorTitle.value = title;
  errorMessage.value = message;
  errorDetail.value = detail;
  showErrorModal.value = true;
};

const handleExport = async () => {
  try {
    await exportExcel('/teams/export', 'danh_sach_to_doi.xlsx');
  } catch (err) {
    triggerError('Lỗi xuất file', 'Không thể xuất danh sách tổ đội ra Excel.', err.message);
  }
};

const downloadTemplate = async () => {
  try {
    await dlTemplate('/teams/download-template', 'mau_nhap_to_doi.xlsx');
  } catch (err) {
    triggerError('Lỗi tải mẫu', 'Không thể tải xuống tệp tin mẫu nhập liệu.', err.message);
  }
};

const handleImport = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  const formData = new FormData();
  formData.append('file', file);

  try {
    loading.value = true;
    const res = await $api.post('/teams/import/preview', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    
    previewData.value = res.data.data;
    importErrors.value = res.data.errors;
    showImportPreview.value = true;
  } catch (err) {
    const msg = err.response?.data?.message || 'Hệ thống không thể đọc nội dung file Excel này. Vui lòng kiểm tra lại định dạng hoặc template.';
    triggerError('Lỗi nhập dữ liệu', msg, err.response?.data?.errors?.join('\n') || err.message);
  } finally {
    loading.value = false;
    event.target.value = ''; // Reset input
  }
};

const handleConfirmImport = async () => {
  if (previewData.value.length === 0) return;
  
  importing.value = true;
  try {
    await $api.post('/teams/import/confirm', previewData.value);
    alert('Nhập dữ liệu thành công');
    showImportPreview.value = false;
    fetchData();
  } catch (err) {
    triggerError('Lỗi lưu dữ liệu', 'Đã xảy ra lỗi khi lưu danh sách tổ đội mới vào hệ thống.', err.response?.data?.message || err.message);
  } finally {
    importing.value = false;
  }
};

const filterDeptId = ref(route.query.departmentId ? parseInt(route.query.departmentId) : '');

const filteredTeams = computed(() => {
  if (!filterDeptId.value) return teams.value;
  return teams.value.filter(t => t.departmentId === parseInt(filterDeptId.value));
});

// Pagination
const currentPage = ref(1);
const itemsPerPage = ref(10);
const totalPages = computed(() => Math.ceil(filteredTeams.value.length / itemsPerPage.value) || 1);

const paginatedTeams = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return filteredTeams.value.slice(start, end);
});

watch([filterDeptId, itemsPerPage], () => {
  currentPage.value = 1;
});

const currentTeam = ref({});
const form = reactive({
  name: '',
  productionStepId: '',
  departmentId: ''
});

const stepOptions = computed(() => 
  productionSteps.value.map(step => ({
    label: step.name,
    value: step.id
  }))
);

const fetchData = async () => {
  loading.value = true;
  try {
    const [teamsRes, stepsRes] = await Promise.all([
      $api.get('/teams'),
      $api.get('/production-steps')
    ]);
    teams.value = teamsRes.data;
    productionSteps.value = stepsRes.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openModal = (team = null) => {
  if (team) {
    currentTeam.value = { ...team };
    form.name = team.name;
    form.productionStepId = team.productionStepId || '';
    form.departmentId = team.departmentId || '';
  } else {
    currentTeam.value = {};
    form.name = '';
    form.productionStepId = '';
    form.departmentId = '';
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    const payload = {
      name: form.name,
      productionStepId: parseInt(form.productionStepId),
      departmentId: form.departmentId ? parseInt(form.departmentId) : null
    };
    
    if (currentTeam.value.id) {
      await $api.put(`/teams/${currentTeam.value.id}`, payload);
    } else {
      await $api.post('/teams', payload);
    }
    showModal.value = false;
    fetchData();
  } catch (err) {
    triggerError('Lỗi lưu tổ đội', 'Đã xảy ra lỗi khi lưu thông tin tổ đội sản xuất.', err.response?.data?.message || err.message);
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa tổ này?')) return;
  try {
    await $api.delete(`/teams/${id}`);
    fetchData();
  } catch (err) {
    triggerError('Lỗi xóa tổ đội', 'Hệ thống gặp sự cố khi xóa tổ đội này.', err.response?.data?.message || err.message);
  }
};

onMounted(fetchData);
</script>
