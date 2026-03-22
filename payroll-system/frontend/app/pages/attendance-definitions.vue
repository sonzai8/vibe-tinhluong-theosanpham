<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-3xl font-black text-slate-900 tracking-tight">{{ $t('attendance.definition.title') }}</h2>
        <p class="text-slate-500 font-medium">{{ $t('attendance.definition.multiplier_note') }}</p>
      </div>
      <UiButton @click="openModal()" class="shadow-lg shadow-emerald-100">
        <Plus class="w-4 h-4 mr-2" />
        {{ $t('attendance.definition.add_new') }}
      </UiButton>
    </div>

    <!-- Common Error Modal -->
    <UiErrorModal
      :show="showErrorModal"
      :title="errorTitle"
      :message="errorMessage"
      :detail="errorDetail"
      @close="showErrorModal = false"
    />

    <!-- Table -->
    <div class="card overflow-hidden">
      <table class="w-full text-left">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-8 py-5">{{ $t('attendance.definition.code') }}</th>
            <th class="px-8 py-5">{{ $t('attendance.definition.name') }}</th>
            <th class="px-8 py-5 text-center">{{ $t('attendance.definition.multiplier') }}</th>
            <th class="px-8 py-5">{{ $t('attendance.definition.description') }}</th>
            <th class="px-8 py-5 text-right">{{ $t('common.actions') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="item in definitions" :key="item.id" class="hover:bg-slate-50/50 transition-all group">
            <td class="px-8 py-5">
              <span class="px-3 py-1 bg-primary-50 text-primary-700 rounded-lg font-black text-xs">{{ item.code }}</span>
            </td>
            <td class="px-8 py-5 font-bold text-slate-700">{{ item.name }}</td>
            <td class="px-8 py-5 text-center">
              <span class="px-2 py-1 bg-slate-100 rounded text-xs font-bold text-slate-600">x{{ item.multiplier }}</span>
            </td>
            <td class="px-8 py-5 text-slate-500 text-sm">{{ item.description || '-' }}</td>
            <td class="px-8 py-5 text-right">
              <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(item)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-xl transition-all">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(item.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-xl transition-all">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="definitions.length === 0">
            <td colspan="5" class="px-8 py-20 text-center text-slate-400 font-bold italic">{{ $t('common.no_data') }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-lg p-10 animate-in zoom-in slide-in-from-bottom duration-300">
        <div class="flex items-center justify-between mb-8">
          <h3 class="text-2xl font-black text-slate-900">{{ currentId ? $t('attendance.definition.edit_title') : $t('attendance.definition.add_new') }}</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:bg-slate-100 rounded-full"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div class="grid grid-cols-2 gap-6">
            <UiInput v-model="form.code" :label="$t('attendance.definition.code')" :placeholder="$t('attendance.definition.placeholder_code')" required />
            <UiInput v-model="form.multiplier" :label="$t('attendance.definition.multiplier')" type="number" step="0.1" required />
          </div>
          <UiInput v-model="form.name" :label="$t('attendance.definition.name')" :placeholder="$t('attendance.definition.placeholder_name')" required />
          <div class="flex flex-col gap-1.5">
            <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('attendance.definition.description') }}</label>
            <textarea v-model="form.description" class="input-field py-3 min-h-[100px] text-sm" placeholder="..."></textarea>
          </div>

          <div class="flex gap-4 pt-4">
            <button type="button" @click="showModal = false" class="flex-1 py-3 text-slate-500 font-bold hover:bg-slate-50 rounded-xl transition-all">{{ $t('common.cancel') }}</button>
            <UiButton type="submit" class="flex-[2] h-12 shadow-lg shadow-primary-100" :loading="saving">{{ $t('common.confirm') }}</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, PencilLine, Trash2, X } from 'lucide-vue-next';
const { $api } = useNuxtApp();

const definitions = ref([]);
const currentId = ref(null);

// Error Modal State
const showErrorModal = ref(false);
const errorTitle = ref('');
const errorMessage = ref('');
const errorDetail = ref('');

const triggerError = (title, message, detail = '') => {
  errorTitle.value = title;
  errorMessage.value = message;
  errorDetail.value = detail;
  showErrorModal.value = true;
};

const form = reactive({
  code: '',
  name: '',
  multiplier: 1.0,
  description: ''
});

const loadDefinitions = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/attendance-definitions');
    definitions.value = res.data;
  } catch (e) {
    triggerError('Lỗi tải dữ liệu', 'Không thể lấy danh sách định nghĩa loại công.', e.message);
  } finally {
    loading.value = false;
  }
};

const openModal = (item = null) => {
  if (item) {
    currentId.value = item.id;
    form.code = item.code;
    form.name = item.name;
    form.multiplier = item.multiplier;
    form.description = item.description;
  } else {
    currentId.value = null;
    form.code = '';
    form.name = '';
    form.multiplier = 1.0;
    form.description = '';
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    if (currentId.value) {
      await $api.put(`/attendance-definitions/${currentId.value}`, form);
    } else {
      await $api.post('/attendance-definitions', form);
    }
    showModal.value = false;
    loadDefinitions();
  } catch (e) {
    triggerError('Lỗi lưu định nghĩa', 'Đã xảy ra lỗi khi lưu loại công.', e.response?.data?.message || e.message);
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa loại công này?')) return;
  try {
    await $api.delete(`/attendance-definitions/${id}`);
    loadDefinitions();
  } catch (e) {
    triggerError('Lỗi xóa định nghĩa', 'Hệ thống gặp sự cố khi xóa loại công này.', e.response?.data?.message || e.message);
  }
};

onMounted(loadDefinitions);
</script>
