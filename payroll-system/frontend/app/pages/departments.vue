<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Quản lý Phòng ban</h2>
        <p class="text-slate-500 font-medium">Danh sách các phòng ban trong công ty</p>
      </div>
      <UiButton @click="openModal()">
        <Plus class="w-4 h-4" />
        Thêm phòng ban
      </UiButton>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">Đang tải dữ liệu...</p>
      </div>

      <div v-else-if="departments.length === 0" class="p-12 text-center space-y-4">
        <div class="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-400">
          <Briefcase class="w-8 h-8" />
        </div>
        <p class="text-slate-500 font-bold">Chưa có phòng ban nào được tạo.</p>
        <UiButton @click="openModal()" class="mx-auto">Bắt đầu thêm mới</UiButton>
      </div>

      <table v-else class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-6 py-4 w-20">ID</th>
            <th class="px-6 py-4">Tên phòng ban</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="dept in departments" :key="dept.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4 text-sm font-black text-slate-400">#{{ dept.id }}</td>
            <td class="px-6 py-4 font-bold text-slate-900">{{ dept.name }}</td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-2">
                <button @click="openModal(dept)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" title="Sửa">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(dept.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" title="Xóa">
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
          <h3 class="text-xl font-black text-slate-900">{{ currentDept.id ? 'Cập nhật' : 'Thêm' }} phòng ban</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <UiInput v-model="form.name" label="Tên phòng ban" placeholder="VD: Phòng Kỹ Thuật" required />
          
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
import { Plus, Briefcase, PencilLine, Trash2, X } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const departments = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const currentDept = ref({});
const form = reactive({
  name: ''
});

const fetchDepartments = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/departments');
    departments.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openModal = (dept = null) => {
  if (dept) {
    currentDept.value = { ...dept };
    form.name = dept.name;
  } else {
    currentDept.value = {};
    form.name = '';
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    if (currentDept.value.id) {
      await $api.put(`/departments/${currentDept.value.id}`, form);
    } else {
      await $api.post('/departments', form);
    }
    showModal.value = false;
    fetchDepartments();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa phòng ban này?')) return;
  try {
    await $api.delete(`/departments/${id}`);
    fetchDepartments();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  }
};

onMounted(fetchDepartments);
</script>
