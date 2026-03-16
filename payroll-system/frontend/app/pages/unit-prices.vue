<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Quản lý Đơn giá Sản phẩm</h2>
        <p class="text-slate-500 font-medium">Thiết lập đơn giá theo từng công đoạn và chất lượng</p>
      </div>
      <div class="flex items-center gap-3">
        <div class="relative group">
          <UiButton variant="outline">
            <Download class="w-4 h-4" />
            Xuất Excel
          </UiButton>
          <div class="absolute right-0 top-full mt-2 w-48 bg-white border border-slate-200 rounded-xl shadow-xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50 p-2">
            <button @click="exportExcel('list')" class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2">
              <Table2 class="w-4 h-4" />
              Dạng danh sách
            </button>
            <button @click="exportExcel('matrix')" class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2 border-t border-slate-50 mt-1 pt-3">
              <LayoutGrid class="w-4 h-4" />
              Dạng ma trận
            </button>
          </div>
        </div>
        <UiButton @click="openModal()">
          <Plus class="w-4 h-4" />
          Thiết lập đơn giá
        </UiButton>
      </div>
    </div>

    <!-- Filters (Only for List View) -->
    <div v-if="viewMode === 'list'" class="card p-4 flex flex-wrap gap-4 items-end animate-in fade-in slide-in-from-top-2 duration-300">
      <div class="space-y-1.5 flex-1 min-w-[200px]">
        <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Lọc theo Sản phẩm</label>
        <select v-model="filters.productId" class="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all">
          <option :value="null">Tất cả sản phẩm</option>
          <option v-for="p in products" :key="p.id" :value="p.id">{{ p.code }} ({{ p.thickness }}x{{ p.length }}x{{ p.width }})</option>
        </select>
      </div>
      <div class="space-y-1.5 flex-1 min-w-[200px]">
        <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Lọc theo Công đoạn</label>
        <select v-model="filters.stepId" class="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all">
          <option :value="null">Tất cả công đoạn</option>
          <option v-for="s in steps" :key="s.id" :value="s.id">{{ s.name }}</option>
        </select>
      </div>
      <UiButton variant="outline" @click="resetFilters">Làm mới</UiButton>
    </div>

    <!-- Table: List View -->
    <div v-if="viewMode === 'list'" class="card overflow-hidden animate-in fade-in duration-500">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">Đang tải dữ liệu...</p>
      </div>
      <table v-else class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-6 py-4">Sản phẩm</th>
            <th class="px-6 py-4">Công đoạn</th>
            <th class="px-6 py-4">Chất lượng</th>
            <th class="px-6 py-4 text-emerald-600">Giá High</th>
            <th class="px-6 py-4">Giá Low</th>
            <th class="px-6 py-4">Ngày hiệu lực</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="r in filteredRates" :key="r.id" class="hover:bg-slate-50/50 transition-colors group text-sm font-medium">
            <td class="px-6 py-4">
              <div class="flex flex-col text-xs">
                <span class="font-black text-slate-900">{{ r.product.code }}</span>
                <span class="text-[9px] text-slate-400 font-bold uppercase">{{ r.product.thickness }}x{{ r.product.length }}x{{ r.product.width }}</span>
              </div>
            </td>
            <td class="px-6 py-4">
              <span class="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-[9px] font-black uppercase">
                {{ r.productionStep.name }}
              </span>
            </td>
            <td class="px-6 py-4">
              <span :class="['px-2 py-0.5 rounded text-[9px] font-black uppercase', 
                r.quality.code === 'A' ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700']">
                {{ r.quality.code }}
              </span>
            </td>
            <td class="px-6 py-4 font-black text-emerald-600 text-xs">
              {{ formatCurrency(r.priceHigh) }}
            </td>
            <td class="px-6 py-4 font-black text-slate-600 text-xs">
              {{ formatCurrency(r.priceLow) }}
            </td>
            <td class="px-6 py-4 text-slate-500 text-xs">
              {{ formatDate(r.effectiveDate) }}
            </td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-1">
                <button @click="viewHistory(r)" title="Xem lịch sử giá" class="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition-all">
                  <History class="w-4 h-4" />
                </button>
                <button @click="openModal(r)" class="p-1.5 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(r.id)" class="p-1.5 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredRates.length === 0">
            <td colspan="7" class="px-6 py-12 text-center text-slate-400 font-bold">
              Không tìm thấy đơn giá nào phù hợp
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Matrix View -->
    <div v-else class="card overflow-hidden animate-in fade-in duration-500">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold">Đang tính toán ma trận...</p>
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-left border-collapse min-w-max">
          <thead>
            <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
              <th class="px-6 py-4 sticky left-0 bg-slate-50 z-10 border-r border-slate-100">Sản phẩm \ Công đoạn</th>
              <th v-for="s in steps" :key="s.id" class="px-6 py-4 text-center border-r border-slate-100">
                {{ s.name }}
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="p in products" :key="p.id" class="hover:bg-slate-50 transition-colors">
              <td class="px-6 py-4 sticky left-0 bg-white group-hover:bg-slate-50 z-10 border-r border-slate-100 font-black text-slate-900 text-xs">
                {{ p.code }} ({{ p.thickness }}x{{ p.length }}x{{ p.width }})
              </td>
              <td v-for="s in steps" :key="s.id" class="px-4 py-4 border-r border-slate-100">
                <div class="flex flex-col gap-2">
                  <div v-for="q in qualities" :key="q.id" class="group/cell relative">
                    <template v-if="getRate(p.id, s.id, q.id)">
                      <div class="p-2 bg-slate-50/50 rounded-lg border border-slate-200/50 hover:border-primary-200 hover:bg-white transition-all cursor-pointer" @click="openModal(getRate(p.id, s.id, q.id))">
                        <div class="flex items-center justify-between mb-1">
                          <span class="text-[8px] font-black text-slate-400 uppercase tracking-tighter">{{ q.code }}</span>
                          <span class="text-[8px] font-black text-emerald-600 italic">{{ formatCurrency(getRate(p.id, s.id, q.id).priceHigh) }}</span>
                        </div>
                        <div class="text-[8px] font-bold text-slate-500 flex justify-between">
                          <span>Thường:</span>
                          <span>{{ formatCurrency(getRate(p.id, s.id, q.id).priceLow) }}</span>
                        </div>
                      </div>
                    </template>
                    <template v-else>
                      <div class="p-2 bg-white rounded-lg border border-dashed border-slate-100 hover:border-primary-300 hover:bg-primary-50/30 transition-all cursor-pointer group/add ml-px" @click="openModalWith(p.id, s.id, q.id)">
                        <div class="flex items-center justify-between mb-1">
                          <span class="text-[8px] font-black text-slate-300 uppercase tracking-tighter">{{ q.code }}</span>
                          <span class="text-[8px] font-black text-slate-300">0 đ</span>
                        </div>
                        <div class="flex items-center justify-center opacity-0 group-hover/add:opacity-100 transition-opacity">
                          <Plus class="w-3 h-3 text-primary-400" />
                        </div>
                      </div>
                    </template>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-lg p-8 animate-in zoom-in duration-200">
        <div class="flex items-center justify-between mb-8">
          <h3 class="text-xl font-black text-slate-900">{{ currentRate.id ? 'Cập nhật' : 'Thiết lập' }} đơn giá</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-5">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="space-y-1.5">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Sản phẩm</label>
              <select v-model="form.productId" class="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all" required>
                <option value="" disabled>Chọn sản phẩm</option>
                <option v-for="p in products" :key="p.id" :value="p.id">{{ p.code }} ({{ p.thickness }}x{{ p.length }}x{{ p.width }})</option>
              </select>
            </div>
            <div class="space-y-1.5">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Công đoạn</label>
              <select v-model="form.productionStepId" class="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all" required>
                <option value="" disabled>Chọn công đoạn</option>
                <option v-for="s in steps" :key="s.id" :value="s.id">{{ s.name }}</option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="space-y-1.5">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Chất lượng</label>
              <select v-model="form.qualityId" class="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all" required>
                <option value="" disabled>Chọn chất lượng</option>
                <option v-for="q in qualities" :key="q.id" :value="q.id">{{ q.code }} - {{ q.name }}</option>
              </select>
            </div>
            <UiInput v-model="form.effectiveDate" type="date" label="Ngày hiệu lực" required />
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <UiInput v-model="form.priceHigh" type="number" step="0.01" label="Giá Cao (Khi đủ chuyên cần)" placeholder="0.00" required />
            <UiInput v-model="form.priceLow" type="number" step="0.01" label="Giá Thấp (Mặc định)" placeholder="0.00" required />
          </div>
          
          <div class="flex gap-3 pt-4">
            <button type="button" @click="showModal = false" class="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-600 font-bold hover:bg-slate-50 transition-all">Hủy</button>
            <UiButton type="submit" class="flex-1" :loading="saving">Lưu thiết lập</UiButton>
          </div>
        </form>
      </div>
    </div>
    <!-- History Modal -->
    <div v-if="showHistoryModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-2xl p-8 animate-in zoom-in duration-200">
        <div class="flex items-center justify-between mb-8">
          <div>
            <h3 class="text-xl font-black text-slate-900">Lịch sử biến động giá</h3>
            <p class="text-xs text-slate-500 font-medium">
              Sản phẩm: <span class="font-bold text-slate-900">{{ selectedForHistory?.product?.code }}</span> | 
              Công đoạn: <span class="font-bold text-slate-900">{{ selectedForHistory?.productionStep?.name }}</span> | 
              Chất lượng: <span class="font-bold text-slate-900">{{ selectedForHistory?.quality?.code }}</span>
            </p>
          </div>
          <button @click="showHistoryModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="max-h-[400px] overflow-y-auto rounded-xl border border-slate-100">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-slate-50 text-slate-500 text-[9px] font-black uppercase tracking-widest border-b border-slate-100">
                <th class="px-4 py-3">Ngày hiệu lực</th>
                <th class="px-4 py-3">Giá High</th>
                <th class="px-4 py-3">Giá Low</th>
                <th class="px-4 py-3">Ngày tạo</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="h in historyRates" :key="h.id" class="text-xs hover:bg-slate-50/50 transition-colors">
                <td class="px-4 py-3 font-bold text-primary-600">{{ formatDate(h.effectiveDate) }}</td>
                <td class="px-4 py-3 font-black text-emerald-600">{{ formatCurrency(h.priceHigh) }}</td>
                <td class="px-4 py-3 font-black text-slate-600">{{ formatCurrency(h.priceLow) }}</td>
                <td class="px-4 py-3 text-slate-400">{{ formatDate(h.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <div class="mt-6 flex justify-end">
          <UiButton variant="outline" @click="showHistoryModal = false">Đóng</UiButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, PencilLine, Trash2, X, Filter, Table2, History, Download, LayoutGrid } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const { exportExcel: dlExcel } = useExcel();

const exportExcel = async (type) => {
  try {
    const fileName = type === 'list' ? 'don_gia_danh_sach.xlsx' : 'don_gia_ma_tran.xlsx';
    await dlExcel(`/product-step-rates/export?type=${type}`, fileName);
  } catch (err) {
    alert('Không thể xuất dữ liệu');
  }
};
const rates = ref([]);
const products = ref([]);
const steps = ref([]);
const qualities = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);
const viewMode = ref('list');

const filters = reactive({
  productId: null,
  stepId: null
});

const currentRate = ref({});
const form = reactive({
  productId: '',
  productionStepId: '',
  qualityId: '',
  priceHigh: 0,
  priceLow: 0,
  effectiveDate: new Date().toISOString().split('T')[0]
});

const filteredRates = computed(() => {
  if (!Array.isArray(rates.value)) return [];
  return rates.value.filter(r => {
    const matchProduct = !filters.productId || r.product?.id === filters.productId;
    const matchStep = !filters.stepId || r.productionStep?.id === filters.stepId;
    return matchProduct && matchStep;
  });
});

const resetFilters = () => {
  filters.productId = null;
  filters.stepId = null;
};

const fetchData = async () => {
  loading.value = true;
  try {
    const [ratesRes, productsRes, stepsRes, qualitiesRes] = await Promise.all([
      $api.get('/product-step-rates'),
      $api.get('/products'),
      $api.get('/production-steps'),
      $api.get('/product-qualities')
    ]);
    // Kiểm tra cấu trúc ApiResponse (data.data)
    rates.value = ratesRes.success ? ratesRes.data : [];
    products.value = productsRes.success ? productsRes.data : [];
    steps.value = stepsRes.success ? stepsRes.data : [];
    qualities.value = qualitiesRes.success ? qualitiesRes.data : [];
    
    // Debug log to confirm data structure
    console.log('Fetched data:', { rates: rates.value, products: products.value });
  } catch (err) {
    console.error('Error fetching data:', err);
    // Vẫn gán mảng rỗng để không bị lỗi .filter / .map
    rates.value = [];
    products.value = [];
    steps.value = [];
    qualities.value = [];
  } finally {
    loading.value = false;
  }
};

const getRate = (productId, stepId, qualityId) => {
  return rates.value.find(r => 
    r.product?.id === productId && 
    r.productionStep?.id === stepId && 
    r.quality?.id === qualityId
  );
};

const openModalWith = (productId, stepId, qualityId) => {
  form.productId = productId;
  form.productionStepId = stepId;
  form.qualityId = qualityId;
  form.priceHigh = 0;
  form.priceLow = 0;
  
  // Mặc định ngày hiệu lực là 1 năm sau kể từ hiện tại
  const nextYear = new Date();
  nextYear.setFullYear(nextYear.getFullYear() + 1);
  form.effectiveDate = nextYear.toISOString().split('T')[0];
  
  currentRate.value = {};
  showModal.value = true;
};

const openModal = (r = null) => {
  if (r) {
    currentRate.value = { ...r };
    form.productId = r.product?.id;
    form.productionStepId = r.productionStep?.id;
    form.qualityId = r.quality?.id;
    form.priceHigh = r.priceHigh;
    form.priceLow = r.priceLow;
    form.effectiveDate = r.effectiveDate;
  } else {
    currentRate.value = {};
    form.productId = products.value[0]?.id || '';
    form.productionStepId = steps.value[0]?.id || '';
    form.qualityId = qualities.value[0]?.id || '';
    form.priceHigh = 0;
    form.priceLow = 0;
    
    // Mặc định ngày hiệu lực là 1 năm sau kể từ hiện tại
    const nextYear = new Date();
    nextYear.setFullYear(nextYear.getFullYear() + 1);
    form.effectiveDate = nextYear.toISOString().split('T')[0];
  }
  showModal.value = true;
};

const showHistoryModal = ref(false);
const historyRates = ref([]);
const selectedForHistory = ref(null);

const viewHistory = (r) => {
  selectedForHistory.value = r;
  // Lọc lịch sử giá của cùng bộ (Sản phẩm - Công đoạn - Chất lượng)
  historyRates.value = rates.value
    .filter(item => 
      item.product?.id === r.product?.id && 
      item.productionStep?.id === r.productionStep?.id && 
      item.quality?.id === r.quality?.id
    )
    .sort((a, b) => new Date(b.effectiveDate) - new Date(a.effectiveDate));
  showHistoryModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    const payload = {
      ...form,
      priceHigh: parseFloat(form.priceHigh),
      priceLow: parseFloat(form.priceLow)
    };
    
    if (currentRate.value.id) {
      await $api.put(`/product-step-rates/${currentRate.value.id}`, payload);
    } else {
      await $api.post('/product-step-rates', payload);
    }
    showModal.value = false;
    await fetchData(); // Refresh all data
  } catch (err) {
    alert(err.message || 'Lỗi khi lưu dữ liệu');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa đơn giá này?')) return;
  try {
    await $api.delete(`/product-step-rates/${id}`);
    await fetchData();
  } catch (err) {
    alert('Lỗi khi xóa dữ liệu');
  }
};

const formatCurrency = (val) => {
  if (val === undefined || val === null) return '0 đ';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(val);
};

const formatDate = (dateStr) => {
  if (!dateStr) return '---';
  const d = new Date(dateStr);
  return d.toLocaleDateString('vi-VN');
};

onMounted(fetchData);
</script>

<style scoped>
.sticky {
  position: sticky;
}
.left-0 {
  left: 0;
}
.z-10 {
  z-index: 10;
}
</style>
