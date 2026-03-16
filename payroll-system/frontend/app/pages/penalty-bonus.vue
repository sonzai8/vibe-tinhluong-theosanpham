<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-3xl font-black text-slate-900 tracking-tight">Khen thưởng & Kỷ luật</h2>
        <p class="text-slate-500 font-medium">Quản lý các khoản cộng/trừ ngoài lương</p>
      </div>
      <UiButton @click="openModal()">
        <PlusCircle class="w-4 h-4" />
        Thêm bản ghi mới
      </UiButton>
    </div>

    <div class="card overflow-hidden">
      <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
        <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold">Đang tải...</p>
      </div>

      <table v-else class="w-full text-left">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-8 py-5">Ngày</th>
            <th class="px-8 py-5">Nhân viên</th>
            <th class="px-8 py-5">Loại</th>
            <th class="px-8 py-5">Lý do</th>
            <th class="px-8 py-5">Số tiền</th>
            <th class="px-8 py-5 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="item in items" :key="item.id" class="hover:bg-slate-50/50 transition-all group">
            <td class="px-8 py-5 text-sm font-bold text-slate-500">{{ item.recordDate }}</td>
            <td class="px-8 py-5 font-black text-slate-900">{{ item.employee?.fullName }}</td>
            <td class="px-8 py-5">
              <span :class="`px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${item.type === 'BONUS' ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-600'}`">
                {{ item.type === 'BONUS' ? 'Thưởng' : 'Phạt' }}
              </span>
            </td>
            <td class="px-8 py-5 text-sm font-medium text-slate-600 truncate max-w-[200px]">{{ item.reason }}</td>
            <td :class="`px-8 py-5 font-black ${item.type === 'BONUS' ? 'text-emerald-700' : 'text-red-700'}`">
              {{ item.type === 'BONUS' ? '+' : '-' }}{{ item.amount.toLocaleString() }}đ
            </td>
            <td class="px-8 py-5 text-right">
              <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(item)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg"><PencilLine class="w-4 h-4" /></button>
                <button @click="handleDelete(item.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg"><Trash2 class="w-4 h-4" /></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-md p-10 animate-in zoom-in duration-300">
        <div class="flex items-center justify-between mb-10">
          <h3 class="text-2xl font-black text-slate-900">Chi tiết khoản thu/chi</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-black text-slate-700">Nhân viên</label>
            <select v-model="form.employeeId" class="input-field" required>
              <option v-for="e in employees" :key="e.id" :value="e.id">{{ e.fullName }} ({{ e.code }})</option>
            </select>
          </div>
          
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-black text-slate-700">Loại hình</label>
            <div class="grid grid-cols-2 gap-3">
              <button 
                type="button" 
                @click="form.type = 'BONUS'"
                :class="`py-2 rounded-lg font-black text-sm transition-all ${form.type === 'BONUS' ? 'bg-emerald-600 text-white' : 'bg-slate-50 text-slate-400'}`"
              >
                Khen thưởng
              </button>
              <button 
                type="button" 
                @click="form.type = 'PENALTY'"
                :class="`py-2 rounded-lg font-black text-sm transition-all ${form.type === 'PENALTY' ? 'bg-red-600 text-white' : 'bg-slate-50 text-slate-400'}`"
              >
                Kỷ luật
              </button>
            </div>
          </div>

          <UiInput v-model="form.recordDate" label="Ngày ghi nhận" type="date" required />
          <UiInput v-model="form.amount" label="Số tiền (VNĐ)" type="number" required />
          <UiInput v-model="form.reason" label="Lý do" placeholder="Nhập lý do cụ thể..." required />
          
          <div class="flex gap-4 pt-6">
            <button type="button" @click="showModal = false" class="flex-1 py-3.5 rounded-2xl border border-slate-200 text-slate-500 font-black hover:bg-slate-50">Hủy</button>
            <UiButton type="submit" class="flex-[2] h-14" :loading="saving">Lưu bản ghi</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { PlusCircle, PencilLine, Trash2, X } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const items = ref([]);
const employees = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const form = reactive({
  employeeId: null,
  type: 'BONUS',
  amount: 0,
  reason: '',
  recordDate: new Date().toISOString().substr(0, 10)
});

const currentId = ref(null);

const fetchData = async () => {
  loading.value = true;
  try {
    const [res, empRes] = await Promise.all([
      $api.get('/penalty-bonus'),
      $api.get('/employees')
    ]);
    items.value = res.data;
    employees.value = empRes.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openModal = (item = null) => {
  if (item) {
    currentId.value = item.id;
    form.employeeId = item.employee?.id;
    form.type = item.type;
    form.amount = item.amount;
    form.reason = item.reason;
    form.recordDate = item.recordDate;
  } else {
    currentId.value = null;
    form.employeeId = null;
    form.type = 'BONUS';
    form.amount = 0;
    form.reason = '';
    form.recordDate = new Date().toISOString().substr(0, 10);
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    if (currentId.value) {
      await $api.put(`/penalty-bonus/${currentId.value}`, form);
    } else {
      await $api.post('/penalty-bonus', form);
    }
    showModal.value = false;
    fetchData();
  } catch (err) {
    alert(err.message);
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Xóa bản ghi này?')) return;
  try {
    await $api.delete(`/penalty-bonus/${id}`);
    fetchData();
  } catch (err) {
    alert(err.message);
  }
};

onMounted(fetchData);
</script>
