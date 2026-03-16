<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Quản lý Công đoạn</h2>
        <p class="text-slate-500 font-medium">Định nghĩa các công đoạn trong quy trình sản xuất</p>
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
          Thêm công đoạn
        </UiButton>
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
            <th class="px-6 py-4 w-20">ID</th>
            <th class="px-6 py-4">Tên công đoạn</th>
            <th class="px-6 py-4">Mô tả kỹ thuật</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="step in paginatedSteps" :key="step.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4 text-sm font-black text-slate-400">#{{ step.id }}</td>
            <td class="px-6 py-4 font-bold text-primary-700">{{ step.name }}</td>
            <td class="px-6 py-4 text-sm text-slate-500 font-medium">{{ step.description || '---' }}</td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-2 text-slate-400">
                <button @click="openModal(step)" class="p-2 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" title="Sửa">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(step.id)" class="p-2 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" title="Xóa">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="steps.length > 0" class="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Hiển thị</span>
          <select v-model="itemsPerPage" class="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-bold text-slate-600 focus:ring-2 focus:ring-primary-500 outline-none">
            <option :value="10">10 dòng</option>
            <option :value="20">20 dòng</option>
            <option :value="50">50 dòng</option>
          </select>
          <span class="text-xs font-bold text-slate-500">
            {{ (currentPage - 1) * itemsPerPage + 1 }}-{{ Math.min(currentPage * itemsPerPage, steps.length) }} của {{ steps.length }}
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
          <h3 class="text-xl font-black text-slate-900">{{ currentStep.id ? 'Cập nhật' : 'Thêm' }} công đoạn</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <UiInput v-model="form.name" label="Tên công đoạn" placeholder="VD: Ép Nhiệt" required />
          <UiInput v-model="form.description" label="Mô tả" placeholder="Mô tả về kỹ thuật công đoạn..." />
          
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
import { Plus, Layers, PencilLine, Trash2, X, ChevronLeft, ChevronRight, Download, Upload, FileDown, FileUp } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const { downloadTemplate: dlTemplate, importExcel, exportExcel } = useExcel();
const steps = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const handleExport = async () => {
  try {
    await exportExcel('/production-steps/export', 'danh_sach_cong_doan.xlsx');
  } catch (err) {
    alert('Không thể xuất dữ liệu');
  }
};

const downloadTemplate = async () => {
  try {
    await dlTemplate('/production-steps/download-template', 'mau_nhap_cong_doan.xlsx');
  } catch (err) {
    alert('Không thể tải file mẫu');
  }
};

const handleImport = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  try {
    loading.value = true;
    await importExcel('/production-steps/import', file);
    alert('Nhập dữ liệu thành công');
    fetchSteps();
  } catch (err) {
    alert(err.response?.data?.message || 'Lỗi khi nhập dữ liệu');
  } finally {
    loading.value = false;
    event.target.value = ''; // Reset input
  }
};
const currentStep = ref({});
const form = reactive({
  name: '',
  description: ''
});

// Pagination
const currentPage = ref(1);
const itemsPerPage = ref(10);
const totalPages = computed(() => Math.ceil(steps.value.length / itemsPerPage.value) || 1);

const paginatedSteps = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return steps.value.slice(start, end);
});

watch(itemsPerPage, () => {
  currentPage.value = 1;
});

const fetchSteps = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/production-steps');
    steps.value = res.data;
  } catch (err) {
    console.error(err);
    alert(err.response?.data?.message || err.message || 'Không thể tải dữ liệu công đoạn. Vui lòng thử lại sau.');
  } finally {
    loading.value = false;
  }
};

const openModal = (step = null) => {
  if (step) {
    currentStep.value = { ...step };
    form.name = step.name;
    form.description = step.description;
  } else {
    currentStep.value = {};
    form.name = '';
    form.description = '';
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    if (currentStep.value.id) {
      await $api.put(`/production-steps/${currentStep.value.id}`, form);
    } else {
      await $api.post('/production-steps', form);
    }
    showModal.value = false;
    fetchSteps();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa công đoạn này?')) return;
  try {
    await $api.delete(`/production-steps/${id}`);
    fetchSteps();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  }
};

onMounted(fetchSteps);
</script>
