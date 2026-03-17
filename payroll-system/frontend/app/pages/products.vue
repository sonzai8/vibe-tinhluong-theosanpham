<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">{{ $t('product.title') }}</h2>
        <p class="text-slate-500 font-medium">{{ $t('product.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-2">
        <UiButton variant="outline" @click="handleExport">
          <Download class="w-4 h-4" />
          {{ $t('common.export') }}
        </UiButton>
        <div class="relative group">
          <UiButton variant="outline">
            <Upload class="w-4 h-4" />
            {{ $t('common.import') }}
          </UiButton>
          <div class="absolute right-0 top-full mt-2 w-48 bg-white border border-slate-200 rounded-xl shadow-xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50 p-2">
            <button @click="downloadTemplate" class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2">
              <FileDown class="w-4 h-4" />
              {{ $t('common.download_template') }}
            </button>
            <label class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2 cursor-pointer">
              <FileUp class="w-4 h-4" />
              {{ $t('common.import') }}
              <input type="file" class="hidden" accept=".xlsx, .xls" @change="handleImport" />
            </label>
          </div>
        </div>
        <UiButton @click="openModal()">
          <Plus class="w-4 h-4" />
          {{ $t('product.add_new') }}
        </UiButton>
      </div>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">{{ $t('product.loading') }}</p>
      </div>

      <table v-else class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-6 py-4">{{ $t('product.name') }}</th>
            <th class="px-6 py-4">{{ $t('product.code') }}</th>
            <th class="px-6 py-4">{{ $t('product.category') }}</th>
            <th class="px-6 py-4">{{ $t('product.film_coating') }}</th>
            <th class="px-6 py-4">{{ $t('product.unit') }}</th>
            <th class="px-6 py-4 text-right pr-6">{{ $t('common.actions') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="p in paginatedProducts" :key="p.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4">
              <span class="font-black text-slate-900 uppercase tracking-tight">{{ p.name || '---' }}</span>
            </td>
            <td class="px-6 py-4">
              <span class="px-2 py-0.5 bg-primary-50 text-primary-700 rounded text-[10px] font-black uppercase tracking-wider border border-primary-100">
                {{ p.code }}
              </span>
            </td>
            <td class="px-6 py-4 font-bold text-slate-700">
              {{ p.thickness }}mm 
              <span class="text-slate-400 font-medium mx-1">x</span> 
              {{ p.length }}m 
              <span class="text-slate-400 font-medium mx-1">x</span> 
              {{ p.width }}m
            </td>
            <td class="px-6 py-4">
              <span v-if="p.filmCoatingType === 'NONE'" class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('product.no_film') }}</span>
              <span v-else-if="p.filmCoatingType === 'SIDE_1'" class="px-2 py-1 bg-blue-50 text-blue-600 rounded text-[10px] font-black uppercase tracking-widest border border-blue-100 italic">{{ $t('product.side_1') }}</span>
              <span v-else class="px-2 py-1 bg-indigo-50 text-indigo-600 rounded text-[10px] font-black uppercase tracking-widest border border-indigo-100 italic">{{ $t('product.side_2') }}</span>
            </td>
            <td class="px-6 py-4">
              <span v-if="p.unit" class="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-[10px] font-black uppercase tracking-widest">{{ p.unit.name }}</span>
              <span v-else class="text-slate-400 text-xs italic">---</span>
            </td>
            <td class="px-6 py-4 text-right pr-6">
              <div class="flex items-center justify-end gap-2 text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(p)" class="p-2 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" :title="$t('product.edit')">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(p.id)" class="p-2 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" :title="$t('common.delete')">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="products.length > 0" class="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('production.displaying') }}</span>
          <select v-model="itemsPerPage" class="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-bold text-slate-600 focus:ring-2 focus:ring-primary-500 outline-none">
            <option :value="10">10 {{ $t('common.rows') }}</option>
            <option :value="20">20 {{ $t('common.rows') }}</option>
            <option :value="50">50 {{ $t('common.rows') }}</option>
          </select>
          <span class="text-xs font-bold text-slate-500">
            {{ (currentPage - 1) * itemsPerPage + 1 }}-{{ Math.min(currentPage * itemsPerPage, products.length) }} {{ $t('common.of') }} {{ products.length }}
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
          <h3 class="text-xl font-black text-slate-900 tracking-tight">{{ currentPrd.id ? $t('common.update') : $t('common.add_new') }}</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div class="space-y-4">
            <UiInput v-model="form.name" :label="$t('product.name')" :placeholder="$t('product.placeholder_name')" required @input="handleNameInput" />
            <UiInput v-model="form.code" :label="$t('product.code')" :placeholder="$t('product.placeholder_code')" required @input="isCodeCustomized = true" />
            
            <div class="grid grid-cols-3 gap-4">
              <UiInput v-model="form.thickness" type="number" step="0.1" label="Dày (mm)" required />
              <UiInput v-model="form.length" type="number" step="0.01" label="Dài (m)" required />
              <UiInput v-model="form.width" type="number" step="0.01" label="Rộng (m)" required />
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-2">
                <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">{{ $t('product.unit') }}</label>
                <select v-model="form.unitId" class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 hover:bg-white hover:border-slate-200 outline-none transition-all appearance-none" required>
                  <option :value="null" disabled>{{ $t('product.not_specified') }}</option>
                  <option v-for="u in units" :key="u.id" :value="u.id">{{ u.name }}</option>
                </select>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">{{ $t('product.film_coating') }}</label>
                <select v-model="form.filmCoatingType" class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 hover:bg-white hover:border-slate-200 outline-none transition-all appearance-none" required>
                  <option value="NONE">{{ $t('product.no_film') }}</option>
                  <option value="SIDE_1">{{ $t('product.side_1') }}</option>
                  <option value="SIDE_2">{{ $t('product.side_2') }}</option>
                </select>
              </div>
            </div>
          </div>
          
          <div class="flex gap-3 pt-2">
            <button type="button" @click="showModal = false" class="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-600 font-bold hover:bg-slate-50 transition-all text-sm">{{ $t('common.cancel') }}</button>
            <UiButton type="submit" class="flex-1 font-black" :loading="saving">{{ $t('common.save') }}</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, Package, PencilLine, Trash2, X, ChevronLeft, ChevronRight, Download, Upload, FileDown, FileUp, ShieldCheck, LayoutGrid, Tags } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const { downloadTemplate: dlTemplate, importExcel, exportExcel } = useExcel();
const products = ref([]);
const units = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);
const isCodeCustomized = ref(false);

const handleExport = async () => {
  try {
    await exportExcel('/products/export', 'danh_sach_san_pham.xlsx');
  } catch (err) {
    alert('Không thể xuất dữ liệu');
  }
};

const downloadTemplate = async () => {
  try {
    await dlTemplate('/products/download-template', 'mau_nhap_san_pham.xlsx');
  } catch (err) {
    alert('Không thể tải file mẫu');
  }
};

const handleImport = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  try {
    loading.value = true;
    await importExcel('/products/import', file);
    alert('Nhập dữ liệu thành công');
    fetchAllData();
  } catch (err) {
    alert(err.response?.data?.message || 'Lỗi khi nhập dữ liệu');
  } finally {
    loading.value = false;
    event.target.value = ''; // Reset input
  }
};
const currentPrd = ref({});
const form = reactive({
  name: '',
  code: '',
  thickness: 0,
  length: 0,
  width: 0,
  unitId: null,
  filmCoatingType: 'NONE'
});

// Helper slugify to code
const slugify = (text) => {
  return text
    .toString()
    .normalize('NFD')                   // split accented characters into their base characters and diacritical marks
    .replace(/[\u0300-\u036f]/g, '')   // remove all the accents, which happen to be all in the \u03xx UNICODE range.
    .toUpperCase()
    .trim()
    .replace(/\s+/g, '_')              // Replace spaces with _
    .replace(/[^\w-]+/g, '')           // Remove all non-word chars
    .replace(/--+/g, '_');             // Replace multiple - with single _
};

const handleNameInput = () => {
  if (!currentPrd.value.id && !isCodeCustomized.value) {
    form.code = slugify(form.name);
  }
};

// Pagination
const currentPage = ref(1);
const itemsPerPage = ref(10);
const totalPages = computed(() => Math.ceil(products.value.length / itemsPerPage.value) || 1);

const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return products.value.slice(start, end);
});

watch(itemsPerPage, () => {
  currentPage.value = 1;
});

const fetchAllData = async () => {
  loading.value = true;
  try {
    const [prdRes, unitRes] = await Promise.all([
      $api.get('/products'),
      $api.get('/product-units')
    ]);
    products.value = prdRes.data;
    units.value = unitRes.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openModal = (p = null) => {
  isCodeCustomized.value = p ? true : false;
  if (p) {
    currentPrd.value = { ...p };
    form.name = p.name || '';
    form.code = p.code;
    form.thickness = p.thickness;
    form.length = p.length;
    form.width = p.width;
    form.unitId = p.unit?.id || null;
    form.filmCoatingType = p.filmCoatingType || 'NONE';
  } else {
    currentPrd.value = {};
    form.name = '';
    form.code = '';
    form.thickness = 0;
    form.length = 0;
    form.width = 0;
    form.unitId = units.value.length > 0 ? units.value[0].id : null;
    form.filmCoatingType = 'NONE';
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    const payload = {
      ...form,
      thickness: parseFloat(form.thickness),
      length: parseFloat(form.length),
      width: parseFloat(form.width)
    };

    if (currentPrd.value.id) {
      await $api.put(`/products/${currentPrd.value.id}`, payload);
    } else {
      await $api.post('/products', payload);
    }
    showModal.value = false;
    fetchAllData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm(currentPrd.value.name ? `Bạn có chắc chắn muốn xóa sản phẩm ${currentPrd.value.name}?` : 'Bạn có chắc chắn muốn xóa sản phẩm này?')) return;
  try {
    await $api.delete(`/products/${id}`);
    fetchAllData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  }
};

onMounted(fetchAllData);
</script>
