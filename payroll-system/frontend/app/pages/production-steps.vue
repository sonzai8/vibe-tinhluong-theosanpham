<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Quản lý Công đoạn</h2>
        <p class="text-slate-500 font-medium">Định nghĩa các công đoạn trong quy trình sản xuất</p>
      </div>
      <UiButton @click="openModal()">
        <Plus class="w-4 h-4" />
        Thêm công đoạn
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
            <th class="px-6 py-4">Mã công đoạn</th>
            <th class="px-6 py-4">Tên công đoạn</th>
            <th class="px-6 py-4">Mô tả</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="step in steps" :key="step.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4 font-black text-slate-900">{{ step.code }}</td>
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
          <UiInput v-model="form.code" label="Mã công đoạn" placeholder="VD: CD-EP-NHIET" required />
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
import { Plus, Layers, PencilLine, Trash2, X } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const steps = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const currentStep = ref({});
const form = reactive({
  code: '',
  name: '',
  description: ''
});

const fetchSteps = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/production-steps');
    steps.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openModal = (step = null) => {
  if (step) {
    currentStep.value = { ...step };
    form.code = step.code;
    form.name = step.name;
    form.description = step.description;
  } else {
    currentStep.value = {};
    form.code = '';
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
    alert(err.message || 'Có lỗi xảy ra');
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
    alert(err.message || 'Có lỗi xảy ra');
  }
};

onMounted(fetchSteps);
</script>
