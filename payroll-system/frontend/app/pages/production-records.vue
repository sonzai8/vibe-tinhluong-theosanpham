<template>
  <div class="space-y-8">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-3xl font-black text-slate-900 tracking-tight">Nhật ký Sản lượng</h2>
        <p class="text-slate-500 font-medium">Ghi nhận năng suất theo tổ đội sản xuất</p>
      </div>
      <UiButton @click="openModal()" class="shadow-lg shadow-primary-100">
        <PlusCircle class="w-4 h-4" />
        Ghi nhận sản lượng
      </UiButton>
    </div>

    <!-- Filter -->
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

    <!-- Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
        <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold">Đang tải bản ghi...</p>
      </div>

      <div v-else-if="records.length === 0" class="p-20 text-center space-y-4">
        <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-300">
          <History class="w-10 h-10" />
        </div>
        <p class="text-slate-500 font-bold">Chưa có sản lượng nào được ghi nhận.</p>
      </div>

      <table v-else class="w-full text-left">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-8 py-5">Ngày</th>
            <th class="px-8 py-5">Tổ sản xuất</th>
            <th class="px-8 py-5">Sản phẩm</th>
            <th class="px-8 py-5">Chất lượng</th>
            <th class="px-8 py-5">Số lượng</th>
            <th class="px-8 py-5 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="r in records" :key="r.id" class="hover:bg-slate-50/50 transition-all group">
            <td class="px-8 py-5 text-sm font-bold text-slate-500">{{ r.productionDate }}</td>
            <td class="px-8 py-5 font-black text-slate-900">
              <div class="flex flex-col">
                <span>{{ r.team?.name }}</span>
                <span class="text-[10px] text-slate-400 font-black uppercase tracking-tighter">{{ r.team?.productionStep?.name }}</span>
              </div>
            </td>
            <td class="px-8 py-5">
              <div class="flex flex-col">
                <span class="font-bold text-primary-700">{{ r.product?.code }}</span>
                <span class="text-[10px] text-slate-400 font-medium">
                  {{ r.product?.thickness }}mm x {{ r.product?.length }}m x {{ r.product?.width }}m
                </span>
              </div>
            </td>
            <td class="px-8 py-5">
              <span class="px-2.5 py-1 bg-primary-100 rounded-lg text-[10px] font-black text-primary-700 uppercase tracking-widest">
                {{ r.quality?.code }}
              </span>
            </td>
            <td class="px-8 py-5 font-black text-slate-900 text-lg">{{ r.quantity?.toLocaleString() }}</td>
            <td class="px-8 py-5 text-right">
              <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(r)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all"><PencilLine class="w-4 h-4" /></button>
                <button @click="handleDelete(r.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"><Trash2 class="w-4 h-4" /></button>
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
          <h3 class="text-2xl font-black text-slate-900">{{ currentId ? 'Cập nhật' : 'Báo cáo' }} sản lượng</h3>
          <button @click="showModal = false" class="p-2.5 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
            <UiSelect 
              v-model="form.teamId" 
              label="Tổ sản xuất" 
              :options="teamOptions" 
              placeholder="Chọn tổ sản xuất"
              required
            />
            
            <UiInput v-model="form.productionDate" label="Ngày sản xuất" type="date" required />
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <UiSelect 
              v-model="form.productId" 
              label="Sản phẩm" 
              :options="productOptions" 
              placeholder="Chọn sản phẩm"
              required
            />

            <UiSelect 
              v-model="form.qualityId" 
              label="Chất lượng" 
              :options="qualityOptions" 
              placeholder="Chọn chất lượng"
              required
            />

            <UiInput v-model="form.quantity" label="Số lượng (Tấm)" type="number" min="1" required />
          </div>

          <div class="flex gap-4 pt-6">
            <button type="button" @click="showModal = false" class="flex-1 py-3.5 rounded-2xl border border-slate-200 text-slate-500 font-black hover:bg-slate-50 transition-all">Hủy</button>
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
const teams = ref([]);
const products = ref([]);
const qualities = ref([]);

const teamOptions = computed(() => teams.value.map(t => ({ value: t.id, label: t.name })));
const productOptions = computed(() => products.value.map(p => ({ value: p.id, label: `${p.code} (${p.thickness}x${p.length}x${p.width})` })));
const qualityOptions = computed(() => qualities.value.map(q => ({ value: q.id, label: q.code })));

const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const filter = reactive({
  from: new Date(new Date().setDate(1)).toISOString().substr(0, 10), // Đầu tháng
  to: new Date().toISOString().substr(0, 10)
});

const form = reactive({
  teamId: null,
  productId: null,
  qualityId: null,
  productionDate: new Date().toISOString().substr(0, 10),
  quantity: 0
});

const currentId = ref(null);

const fetchData = async () => {
  loading.value = true;
  try {
    const [recRes, teamRes, prdRes, qualRes] = await Promise.all([
      $api.get('/production-records', { params: filter }),
      $api.get('/teams'),
      $api.get('/products'),
      $api.get('/product-qualities')
    ]);
    records.value = recRes.data;
    teams.value = teamRes.data;
    products.value = prdRes.data;
    qualities.value = qualRes.data;
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
    form.teamId = r.team?.id;
    form.productId = r.product?.id;
    form.qualityId = r.quality?.id;
    form.productionDate = r.productionDate;
    form.quantity = r.quantity;
  } else {
    currentId.value = null;
    form.teamId = null;
    form.productId = null;
    form.qualityId = null;
    form.productionDate = new Date().toISOString().substr(0, 10);
    form.quantity = 0;
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    // Backend expects numeric quantity
    const payload = {
      ...form,
      quantity: parseInt(form.quantity)
    };

    if (currentId.value) {
      await $api.put(`/production-records/${currentId.value}`, payload);
    } else {
      await $api.post('/production-records', payload);
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
  if (!confirm('Bạn có chắc chắn muốn xóa bản ghi này?')) return;
  try {
    await $api.delete(`/production-records/${id}`);
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Lỗi');
  }
};

onMounted(fetchData);
</script>
