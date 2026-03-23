<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900 uppercase tracking-tight">{{ $t('quality.title') }}</h2>
        <p class="text-slate-500 font-medium text-sm">{{ $t('quality.subtitle') }}</p>
      </div>
      <UiButton @click="openModal()">
        <PlusCircle class="w-4 h-4" />
        {{ $t('quality.add_new') }}
      </UiButton>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">Đang tải danh sách...</p>
      </div>

      <div v-else-if="qualities.length === 0" class="p-12 text-center">
        <div class="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4 text-slate-300">
           <ShieldAlert class="w-8 h-8" />
        </div>
        <p class="text-slate-500 font-bold tracking-tight">Chưa có phân loại chất lượng nào.</p>
        <button @click="openModal()" class="text-primary-600 font-black text-xs uppercase mt-2 hover:underline">Tạo mới ngay</button>
      </div>

      <table v-else class="w-full text-left">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-6 py-4">{{ $t('quality.priority') }}</th>
            <th class="px-6 py-4">{{ $t('quality.code') }}</th>
            <th class="px-6 py-4">{{ $t('quality.description') }}</th>
            <th class="px-6 py-4">{{ $t('quality.layers') }}</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="q in paginatedQualities" :key="q.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4">
              <span class="px-2 py-1 bg-slate-100 border border-slate-200 rounded-lg text-[10px] font-black text-slate-400">
                #{{ q.priority }}
              </span>
            </td>
            <td class="px-6 py-4 font-black text-slate-900">{{ q.code }}</td>
            <td class="px-6 py-4 text-sm text-slate-500">{{ q.description || '---' }}</td>
            <td class="px-6 py-4">
              <div class="flex flex-wrap gap-2">
                <span v-for="layer in q.layers" :key="layer.id" class="px-2 py-0.5 bg-slate-100 border border-slate-200 rounded text-[10px] font-black text-slate-600 uppercase">
                  {{ layer.quantity }}x {{ layer.layer?.layerType }}
                </span>
              </div>
            </td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(q)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" title="Sửa">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(q.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" title="Xóa">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="qualities.length > 0" class="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Hiển thị</span>
          <select v-model="itemsPerPage" class="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-bold text-slate-600 focus:ring-2 focus:ring-primary-500 outline-none">
            <option :value="10">10 dòng</option>
            <option :value="20">20 dòng</option>
            <option :value="50">50 dòng</option>
          </select>
          <span class="text-xs font-bold text-slate-500">
            {{ (currentPage - 1) * itemsPerPage + 1 }}-{{ Math.min(currentPage * itemsPerPage, qualities.length) }} của {{ qualities.length }}
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
      <div class="card w-full max-w-2xl p-8 animate-in zoom-in duration-200 max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-8 sticky top-0 bg-white z-10 pb-4">
          <h3 class="text-xl font-black text-slate-900 tracking-tight">{{ currentQual.id ? 'Cập nhật' : 'Thêm mới' }} cấu hình chất lượng</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <UiInput v-model="form.priority" :label="$t('quality.priority')" type="number" :placeholder="$t('quality.priority_help')" required />
            <UiInput v-model="form.code" :label="$t('quality.code')" placeholder="VD: 2B3CA, 1BA" required />
            <UiInput v-model="form.description" :label="$t('quality.description')" placeholder="Ghi chú thêm nếu có" />
          </div>

          <!-- Layers Configuration -->
          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <h4 class="text-xs font-black text-slate-400 uppercase tracking-widest">{{ $t('quality.layers') }}</h4>
              <button type="button" @click="addLayer" class="text-xs font-black text-primary-600 flex items-center gap-1 hover:underline">
                <Plus class="w-3 h-3" />
                {{ $t('quality.add_layer') }}
              </button>
            </div>

            <div v-if="form.layers.length === 0" class="p-4 bg-slate-50 rounded-xl border-2 border-dashed border-slate-200 text-center text-slate-400 text-sm italic">
              Nhấn "Thêm lớp lỗi mới" để bắt đầu cấu hình.
            </div>

            <div v-for="(l, index) in form.layers" :key="index" class="p-4 bg-slate-50 rounded-xl flex items-end gap-4 animate-in slide-in-from-left duration-200">
              <div class="flex-1">
                 <UiSelect 
                   v-model="l.layerId" 
                   label="Lớp lỗi" 
                   :options="layerOptions" 
                   placeholder="Chọn lớp"
                   required
                 />
              </div>
              <div class="w-24">
                 <UiInput v-model="l.quantity" label="Số lượng" type="number" min="1" required />
              </div>
              <button type="button" @click="removeLayer(index)" class="p-2.5 text-red-400 hover:text-red-500 hover:bg-red-50 rounded-lg mb-0.5">
                <Trash2 class="w-4 h-4" />
              </button>
            </div>
          </div>
          
          <div class="flex gap-4 pt-6 border-t border-slate-100 sticky bottom-0 bg-white pb-2 mt-4">
            <button type="button" @click="showModal = false" class="flex-1 py-3 rounded-xl border border-slate-200 text-slate-600 font-extrabold hover:bg-slate-50 transition-all">{{ $t('common.cancel') }}</button>
            <UiButton type="submit" class="flex-[2] py-4 text-md font-black uppercase tracking-widest" :loading="saving">{{ $t('quality.save') }}</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, PencilLine, Trash2, X, PlusCircle, ShieldAlert, ChevronLeft, ChevronRight } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const qualities = ref([]);
const availableLayers = ref([]);

const layerOptions = computed(() => {
  return availableLayers.value.map(l => ({
    value: l.id,
    label: l.layerType
  }));
});

const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const currentQual = ref({});
const form = reactive({
  code: '',
  description: '',
  priority: 0,
  layers: []
});

// Pagination
const currentPage = ref(1);
const itemsPerPage = ref(10);
const totalPages = computed(() => Math.ceil(qualities.value.length / itemsPerPage.value) || 1);

const paginatedQualities = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return qualities.value.slice(start, end);
});

watch(itemsPerPage, () => {
  currentPage.value = 1;
});

const fetchData = async () => {
  loading.value = true;
  try {
    const [qRes, lRes] = await Promise.all([
      $api.get('/product-qualities'),
      $api.get('/quality-layer-surcharges')
    ]);
    qualities.value = qRes.data;
    availableLayers.value = lRes.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const addLayer = () => {
  form.layers.push({ layerId: null, quantity: 1 });
};

const removeLayer = (index) => {
  form.layers.splice(index, 1);
};

const openModal = (q = null) => {
  if (q) {
    currentQual.value = { ...q };
    form.code = q.code;
    form.description = q.description;
    form.priority = q.priority;
    form.layers = q.layers.map(l => ({
      layerId: l.layer?.id,
      quantity: l.quantity
    }));
  } else {
    currentQual.value = {};
    form.code = '';
    form.description = '';
    form.priority = 0;
    form.layers = [];
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  if (form.layers.length === 0) {
    alert($t('quality.no_layers_alert'));
    return;
  }
  
  saving.value = true;
  try {
    const payload = { ...form };
    // Ensure numeric types
    payload.layers = payload.layers.map(l => ({
      layerId: parseInt(l.layerId),
      quantity: parseInt(l.quantity)
    }));
    payload.priority = parseInt(payload.priority) || 0;

    if (currentQual.value.id) {
      await $api.put(`/product-qualities/${currentQual.value.id}`, payload);
    } else {
      await $api.post('/product-qualities', payload);
    }
    showModal.value = false;
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm($t('quality.delete_confirm'))) return;
  try {
    await $api.delete(`/product-qualities/${id}`);
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Lỗi');
  }
};

onMounted(fetchData);
</script>
