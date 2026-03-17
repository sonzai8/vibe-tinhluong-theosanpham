<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900 tracking-tight">Quản lý Đơn vị</h2>
        <p class="text-slate-500 font-medium">Các đơn vị tính của sản phẩm (Tấm, Khúc, Miếng...)</p>
      </div>
      <UiButton @click="openModal()">
        <Plus class="w-4 h-4" />
        Thêm đơn vị
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
            <th class="px-6 py-4">ID</th>
            <th class="px-6 py-4">Tên đơn vị</th>
            <th class="px-6 py-4">Ngày tạo</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="u in units" :key="u.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4 text-xs font-bold text-slate-400">#{{ u.id }}</td>
            <td class="px-6 py-4 font-black text-slate-900 uppercase tracking-tight">{{ u.name }}</td>
            <td class="px-6 py-4 text-sm text-slate-500">{{ formatDate(u.createdAt) }}</td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-2 text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(u)" class="p-2 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" title="Sửa">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(u.id)" class="p-2 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" title="Xóa">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!loading && units.length === 0" class="p-12 text-center">
        <p class="text-slate-400 font-medium">Chưa có đơn vị nào.</p>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-md p-8 animate-in zoom-in duration-200">
        <div class="flex items-center justify-between mb-8">
          <h3 class="text-xl font-black text-slate-900 tracking-tight">{{ form.id ? 'Cập nhật' : 'Thêm' }} đơn vị</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <UiInput v-model="form.name" label="Tên đơn vị (TẤM, MIẾNG...)" placeholder="Nhập tên đơn vị" required />
          
          <div class="flex gap-3 pt-2">
            <button type="button" @click="showModal = false" class="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-600 font-bold hover:bg-slate-50 transition-all text-sm">Hủy bỏ</button>
            <UiButton type="submit" class="flex-1 font-black" :loading="saving">Lưu lại</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, PencilLine, Trash2, X } from 'lucide-vue-next';
const { $api } = useNuxtApp();

const units = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const form = reactive({
  id: null,
  name: ''
});

const fetchUnits = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/product-units');
    units.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openModal = (u = null) => {
  if (u) {
    form.id = u.id;
    form.name = u.name;
  } else {
    form.id = null;
    form.name = '';
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    if (form.id) {
      await $api.put(`/product-units/${form.id}`, { name: form.name });
    } else {
      await $api.post('/product-units', { name: form.name });
    }
    showModal.value = false;
    fetchUnits();
  } catch (err) {
    alert(err.response?.data?.message || 'Có lỗi xảy ra');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa đơn vị này?')) return;
  try {
    await $api.delete(`/product-units/${id}`);
    fetchUnits();
  } catch (err) {
    alert(err.response?.data?.message || 'Có lỗi xảy ra');
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return '---';
  return new Date(dateStr).toLocaleDateString('vi-VN');
};

onMounted(fetchUnits);
</script>
