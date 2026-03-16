<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Quản lý Chức vụ</h2>
        <p class="text-slate-500 font-medium">Định nghĩa các chức vụ và quyền hạn</p>
      </div>
      <UiButton @click="openModal()">
        <Plus class="w-4 h-4" />
        Thêm chức vụ
      </UiButton>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">Đang tải dữ liệu...</p>
      </div>

      <div v-else-if="roles.length === 0" class="p-12 text-center space-y-4">
        <div class="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-400">
          <Users2 class="w-8 h-8" />
        </div>
        <p class="text-slate-500 font-bold">Chưa có chức vụ nào được tạo.</p>
        <UiButton @click="openModal()" class="mx-auto">Bắt đầu thêm mới</UiButton>
      </div>

      <table v-else class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-6 py-4">Mã chức vụ</th>
            <th class="px-6 py-4">Tên chức vụ</th>
            <th class="px-6 py-4">Mô tả</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="role in roles" :key="role.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4">
              <span class="px-2 py-1 bg-slate-100 rounded text-xs font-black text-slate-600 group-hover:bg-primary-100 group-hover:text-primary-700 transition-colors">
                {{ role.code }}
              </span>
            </td>
            <td class="px-6 py-4 font-bold text-slate-900">{{ role.name }}</td>
            <td class="px-6 py-4 text-sm text-slate-500 font-medium">{{ role.description || '---' }}</td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-2">
                <button @click="openModal(role)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" title="Sửa">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(role.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" title="Xóa">
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
          <h3 class="text-xl font-black text-slate-900">{{ currentRole.id ? 'Cập nhật' : 'Thêm' }} chức vụ</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <UiInput v-model="form.code" label="Mã chức vụ" placeholder="VD: GDS" required />
          <UiInput v-model="form.name" label="Tên chức vụ" placeholder="VD: Giám Đốc Xưởng" required />
          <UiInput v-model="form.dailyBenefit" label="Phụ cấp hằng ngày" placeholder="VD: 100000" required />
          <UiInput v-model="form.description" label="Mô tả" placeholder="Mô tả chức năng nhiệm vụ..." />
          
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
import { Plus, Users2, PencilLine, Trash2, X } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const roles = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const currentRole = ref({});
const form = reactive({
  code: '',
  name: '',
  dailyBenefit: 0,
  description: ''
});

const fetchRoles = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/roles');
    roles.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openModal = (role = null) => {
  if (role) {
    currentRole.value = { ...role };
    form.code = role.code;
    form.name = role.name;
    form.description = role.description;
  } else {
    currentRole.value = {};
    form.code = '';
    form.name = '';
    form.description = '';
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    if (currentRole.value.id) {
      await $api.put(`/roles/${currentRole.value.id}`, form);
    } else {
      await $api.post('/roles', form);
    }
    showModal.value = false;
    fetchRoles();
  } catch (err) {
    alert(err.message || 'Có lỗi xảy ra');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa chức vụ này?')) return;
  try {
    await $api.delete(`/roles/${id}`);
    fetchRoles();
  } catch (err) {
    alert(err.message || 'Có lỗi xảy ra');
  }
};

onMounted(fetchRoles);
</script>
