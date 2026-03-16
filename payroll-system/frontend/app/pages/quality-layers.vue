<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900 uppercase tracking-tight">Danh mục Lớp lỗi</h2>
        <p class="text-slate-500 font-medium text-sm">Quản lý các loại lớp lỗi và phụ phí/phạt tương ứng</p>
      </div>
      <UiButton @click="openModal()">
        <Plus class="w-4 h-4" />
        Thêm lớp lỗi
      </UiButton>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">Đang tải danh sách...</p>
      </div>

      <div v-else-if="layers.length === 0" class="p-12 text-center">
        <div class="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4 text-slate-300">
           <Layers class="w-8 h-8" />
        </div>
        <p class="text-slate-500 font-bold tracking-tight">Chưa có lớp lỗi nào được định nghĩa.</p>
        <button @click="openModal()" class="text-primary-600 font-black text-xs uppercase mt-2 hover:underline">Tạo ngay</button>
      </div>

      <table v-else class="w-full text-left">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-6 py-4">ID</th>
            <th class="px-6 py-4">Tên lớp lỗi</th>
            <th class="px-6 py-4">Phụ phí / Lớp (VNĐ)</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="l in layers" :key="l.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4 text-xs font-black text-slate-400">#{{ l.id }}</td>
            <td class="px-6 py-4">
              <span class="px-2 py-1 bg-primary-100 text-primary-700 rounded text-xs font-black uppercase tracking-widest">{{ l.layerType }}</span>
            </td>
            <td class="px-6 py-4 font-bold text-slate-900">{{ l.surchargePerLayer.toLocaleString() }}</td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(l)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" title="Sửa">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(l.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" title="Xóa">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-md p-8 animate-in zoom-in duration-200">
        <div class="flex items-center justify-between mb-8">
          <h3 class="text-xl font-black text-slate-900 tracking-tight">{{ currentLayer.id ? 'Cập nhật' : 'Thêm mới' }} lớp lỗi</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <UiInput 
            v-model="form.layerType" 
            label="Tên / Mã lớp lỗi" 
            placeholder="VD: Lớp B, Lớp C, Lớp A" 
            required 
          />
          <UiInput 
            v-model="form.surchargePerLayer" 
            label="Phụ phí / Phạt cho mỗi lớp (VNĐ)" 
            type="number" 
            placeholder="VD: 5000" 
            required 
          />
          
          <div class="flex gap-4 pt-4 border-t border-slate-100">
            <button type="button" @click="showModal = false" class="flex-1 py-3 rounded-xl border border-slate-200 text-slate-600 font-extrabold hover:bg-slate-50 transition-all">Huỷ</button>
            <UiButton type="submit" class="flex-[2] py-3 font-black uppercase tracking-widest text-xs" :loading="saving">Lưu dữ liệu</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, PencilLine, Trash2, X, Layers } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const layers = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const currentLayer = ref({});
const form = reactive({
  layerType: '',
  surchargePerLayer: 0
});

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/quality-layer-surcharges');
    layers.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openModal = (l = null) => {
  if (l) {
    currentLayer.value = { ...l };
    form.layerType = l.layerType;
    form.surchargePerLayer = l.surchargePerLayer;
  } else {
    currentLayer.value = {};
    form.layerType = '';
    form.surchargePerLayer = 0;
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    if (currentLayer.value.id) {
      await $api.put(`/quality-layer-surcharges/${currentLayer.value.id}`, form);
    } else {
      await $api.post('/quality-layer-surcharges', form);
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
  if (!confirm('Bạn có chắc chắn muốn xóa lớp lỗi này?')) return;
  try {
    await $api.delete(`/quality-layer-surcharges/${id}`);
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Lỗi');
  }
};

onMounted(fetchData);
</script>
