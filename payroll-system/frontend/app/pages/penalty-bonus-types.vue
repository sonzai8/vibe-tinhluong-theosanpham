<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Định nghĩa Loại Thưởng/Phạt</h2>
        <p class="text-slate-500 font-medium">Cấu hình các loại thưởng và phạt có sẵn trong hệ thống</p>
      </div>
      <UiButton @click="openModal()">
        <Plus class="w-4 h-4" />
        Thêm loại mới
      </UiButton>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">Đang tải dữ liệu...</p>
      </div>

      <div v-else-if="types.length === 0" class="p-12 text-center space-y-4">
        <div class="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-400">
          <BookOpen class="w-8 h-8" />
        </div>
        <p class="text-slate-500 font-bold">Chưa có loại thưởng/phạt nào được định nghĩa.</p>
        <UiButton @click="openModal()" class="mx-auto">Bắt đầu thêm mới</UiButton>
      </div>

      <table v-else class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-6 py-4">Tên loại</th>
            <th class="px-6 py-4">Số tiền mặc định</th>
            <th class="px-6 py-4">Mô tả</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="type in types" :key="type.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4">
              <span class="font-bold text-slate-900">{{ type.name }}</span>
            </td>
            <td class="px-6 py-4">
              <span :class="['font-black', type.defaultAmount > 0 ? 'text-emerald-600' : 'text-red-600']">
                {{ type.defaultAmount > 0 ? '+' : '' }}{{ type.defaultAmount.toLocaleString() }}đ
              </span>
            </td>
            <td class="px-6 py-4 text-sm text-slate-500">{{ type.description || '---' }}</td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-2 text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(type)" class="p-2 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" title="Sửa">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(type.id)" class="p-2 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" title="Xóa">
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
      <div class="card w-full max-w-md p-8 animate-in zoom-in duration-200 shadow-2xl">
        <div class="flex items-center justify-between mb-8">
          <h3 class="text-xl font-black text-slate-900">{{ currentId ? 'Cập nhật' : 'Thêm' }} loại thưởng/phạt</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <UiInput v-model="form.name" label="Tên loại" placeholder="VD: Nghỉ không phép" required />
          <UiInput v-model.number="form.defaultAmount" type="number" label="Số tiền mặc định" placeholder="VD: -500000" required />
          <UiInput v-model="form.description" label="Mô tả" placeholder="Nhập mô tả thêm..." />
          
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
import { Plus, BookOpen, PencilLine, Trash2, X } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const types = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);
const currentId = ref(null);

const form = reactive({
  name: '',
  defaultAmount: 0,
  description: ''
});

const fetchTypes = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/penalty-bonus-types');
    types.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openModal = (type = null) => {
  if (type) {
    currentId.value = type.id;
    form.name = type.name;
    form.defaultAmount = type.defaultAmount;
    form.description = type.description;
  } else {
    currentId.value = null;
    form.name = '';
    form.defaultAmount = 0;
    form.description = '';
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    if (currentId.value) {
      await $api.put(`/penalty-bonus-types/${currentId.value}`, form);
    } else {
      await $api.post('/penalty-bonus-types', form);
    }
    showModal.value = false;
    fetchTypes();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa loại thưởng/phạt này?')) return;
  try {
    await $api.delete(`/penalty-bonus-types/${id}`);
    fetchTypes();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  }
};

onMounted(fetchTypes);
</script>
