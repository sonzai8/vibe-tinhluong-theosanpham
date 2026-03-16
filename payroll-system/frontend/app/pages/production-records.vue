<template>
  <div class="space-y-8">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-3xl font-black text-slate-900 tracking-tight">Nhật ký Sản lượng</h2>
        <p class="text-slate-500 font-medium">Theo dõi năng suất làm việc của từng nhân viên</p>
      </div>
      <UiButton @click="openModal()" class="shadow-lg shadow-primary-100">
        <PlusCircle class="w-4 h-4" />
        Ghi nhận sản lượng
      </UiButton>
    </div>

    <div class="card p-6 flex flex-wrap gap-6 items-end">
      <div class="flex flex-col gap-1.5 min-w-[200px]">
        <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Từ ngày</label>
        <input v-model="filter.from" type="date" class="input-field py-2" />
      </div>
      <div class="flex flex-col gap-1.5 min-w-[200px]">
        <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Đến ngày</label>
        <input v-model="filter.to" type="date" class="input-field py-2" />
      </div>
      <UiButton @click="fetchRecords" variant="secondary" class="bg-slate-900 text-white hover:bg-slate-800">
        <Filter class="w-4 h-4" />
        Lọc dữ liệu
      </UiButton>
    </div>

    <div class="card overflow-hidden">
      <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
        <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold">Đang tải bản ghi...</p>
      </div>

      <div v-else-if="records.length === 0" class="p-20 text-center space-y-4">
        <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-300">
          <History class="w-10 h-10" />
        </div>
        <p class="text-slate-500 font-bold">Chưa có sản lượng nào được ghi nhận trong khoảng thời gian này.</p>
      </div>

      <table v-else class="w-full text-left">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-8 py-5">Ngày</th>
            <th class="px-8 py-5">Nhân viên</th>
            <th class="px-8 py-5">Sản phẩm</th>
            <th class="px-8 py-5">Công đoạn</th>
            <th class="px-8 py-5">Số lượng</th>
            <th class="px-8 py-5 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="r in records" :key="r.id" class="hover:bg-slate-50/50 transition-all group">
            <td class="px-8 py-5 text-sm font-bold text-slate-500">{{ r.recordDate }}</td>
            <td class="px-8 py-5 font-black text-slate-900">{{ r.employee?.fullName }}</td>
            <td class="px-8 py-5 text-sm font-bold text-primary-700">{{ r.product?.name }}</td>
            <td class="px-8 py-5">
              <span class="px-2.5 py-1 bg-slate-100 rounded text-[10px] font-black text-slate-500 uppercase">{{ r.step?.name }}</span>
            </td>
            <td class="px-8 py-5 font-black text-slate-900">{{ r.quantity.toLocaleString() }}</td>
            <td class="px-8 py-5 text-right">
              <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(r)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg"><PencilLine class="w-4 h-4" /></button>
                <button @click="handleDelete(r.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg"><Trash2 class="w-4 h-4" /></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-2xl p-10 animate-in zoom-in slide-in-from-bottom duration-300">
        <div class="flex items-center justify-between mb-10">
          <h3 class="text-2xl font-black text-slate-900">Báo cáo sản lượng</h3>
          <button @click="showModal = false" class="p-2.5 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-black text-slate-700">Nhân viên</label>
              <select v-model="form.employeeId" class="input-field" required>
                <option v-for="e in employees" :key="e.id" :value="e.id">{{ e.fullName }} ({{ e.code }})</option>
              </select>
            </div>
            
            <UiInput v-model="form.recordDate" label="Ngày ghi nhận" type="date" required />
            
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-black text-slate-700">Sản phẩm</label>
              <select v-model="form.productId" class="input-field" required>
                <option v-for="p in products" :key="p.id" :value="p.id">{{ p.name }}</option>
              </select>
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-black text-slate-700">Công đoạn</label>
              <select v-model="form.stepId" class="input-field" required>
                <option v-for="s in steps" :key="s.id" :value="s.id">{{ s.name }}</option>
              </select>
            </div>

            <UiInput v-model="form.quantity" label="Số lượng thực hiện" type="number" required />
          </div>

          <div class="flex gap-4 pt-6">
            <button type="button" @click="showModal = false" class="flex-1 py-3.5 rounded-2xl border border-slate-200 text-slate-500 font-black hover:bg-slate-50">Hủy</button>
            <UiButton type="submit" class="flex-[2] h-14" :loading="saving">Lưu sản lượng</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { PlusCircle, Filter, History, PencilLine, Trash2, X } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const records = ref([]);
const employees = ref([]);
const products = ref([]);
const steps = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const filter = reactive({
  from: new Date(new Date().setDate(1)).toISOString().substr(0, 10), // Đầu tháng
  to: new Date().toISOString().substr(0, 10)
});

const form = reactive({
  employeeId: null,
  productId: null,
  stepId: null,
  recordDate: new Date().toISOString().substr(0, 10),
  quantity: 0
});

const currentId = ref(null);

const fetchData = async () => {
  loading.value = true;
  try {
    const [recRes, empRes, prdRes, stepRes] = await Promise.all([
      $api.get('/production-records', { params: filter }),
      $api.get('/employees'),
      $api.get('/products'),
      $api.get('/production-steps')
    ]);
    records.value = recRes.data;
    employees.value = empRes.data;
    products.value = prdRes.data;
    steps.value = stepRes.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const fetchRecords = () => fetchData();

const openModal = (r = null) => {
  if (r) {
    currentId.value = r.id;
    form.employeeId = r.employee?.id;
    form.productId = r.product?.id;
    form.stepId = r.step?.id;
    form.recordDate = r.recordDate;
    form.quantity = r.quantity;
  } else {
    currentId.value = null;
    form.employeeId = null;
    form.productId = null;
    form.stepId = null;
    form.recordDate = new Date().toISOString().substr(0, 10);
    form.quantity = 0;
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    if (currentId.value) {
      await $api.put(`/production-records/${currentId.value}`, form);
    } else {
      await $api.post('/production-records', form);
    }
    showModal.value = false;
    fetchData();
  } catch (err) {
    alert(err.message || 'Lỗi');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Xóa bản ghi?')) return;
  try {
    await $api.delete(`/production-records/${id}`);
    fetchData();
  } catch (err) {
    alert(err.message);
  }
};

onMounted(fetchData);
</script>
