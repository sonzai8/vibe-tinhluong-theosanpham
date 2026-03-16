<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Quản lý Sản phẩm</h2>
        <p class="text-slate-500 font-medium">Danh mục các loại ván ép gỗ Plywood</p>
      </div>
      <UiButton @click="openModal()">
        <Plus class="w-4 h-4" />
        Thêm sản phẩm
      </UiButton>
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
            <th class="px-6 py-4">Mã sản phẩm</th>
            <th class="px-6 py-4">Thông số kỹ thuật (Dày x Dài x Rộng)</th>
            <th class="px-6 py-4">Đơn vị</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="p in products" :key="p.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4">
              <span class="px-2 py-1 bg-primary-50 text-primary-700 rounded text-xs font-black uppercase">
                {{ p.code }}
              </span>
            </td>
            <td class="px-6 py-4 font-bold text-slate-900">
              {{ p.thickness }}mm 
              <span class="text-slate-400 font-medium mx-1">x</span> 
              {{ p.length }}m 
              <span class="text-slate-400 font-medium mx-1">x</span> 
              {{ p.width }}m
            </td>
            <td class="px-6 py-4 text-xs font-black text-slate-400 uppercase tracking-tighter">Tấm</td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-2">
                <button @click="openModal(p)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" title="Sửa">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(p.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" title="Xóa">
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
          <h3 class="text-xl font-black text-slate-900">{{ currentPrd.id ? 'Cập nhật' : 'Thêm' }} sản phẩm</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <UiInput v-model="form.code" label="Mã sản phẩm" placeholder="VD: BE-12" required />
          <div class="grid grid-cols-3 gap-4">
            <UiInput v-model="form.thickness" type="number" step="0.1" label="Độ dày (mm)" required />
            <UiInput v-model="form.length" type="number" step="0.01" label="Dài (m)" required />
            <UiInput v-model="form.width" type="number" step="0.01" label="Rộng (m)" required />
          </div>
          
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
import { Plus, Package, PencilLine, Trash2, X } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const products = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const currentPrd = ref({});
const form = reactive({
  code: '',
  thickness: 0,
  length: 0,
  width: 0
});

const fetchProducts = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/products');
    products.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openModal = (p = null) => {
  if (p) {
    currentPrd.value = { ...p };
    form.code = p.code;
    form.thickness = p.thickness;
    form.length = p.length;
    form.width = p.width;
  } else {
    currentPrd.value = {};
    form.code = '';
    form.thickness = 0;
    form.length = 0;
    form.width = 0;
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
    fetchProducts();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa sản phẩm này?')) return;
  try {
    await $api.delete(`/products/${id}`);
    fetchProducts();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  }
};

onMounted(fetchProducts);
</script>
