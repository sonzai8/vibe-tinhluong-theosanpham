<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Quản lý Tổ sản xuất</h2>
        <p class="text-slate-500 font-medium">Danh sách các tổ, đội sản xuất trong xưởng</p>
      </div>
      <UiButton @click="openModal()">
        <Plus class="w-4 h-4" />
        Thêm tổ mới
      </UiButton>
    </div>

    <!-- Stats Grid (Mini) -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="card p-6 bg-primary-600 text-white shadow-xl shadow-primary-100 flex items-center gap-4">
        <div class="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center">
          <Users2 class="w-6 h-6" />
        </div>
        <div>
          <p class="text-xs font-bold text-primary-100 uppercase tracking-widest">Tổng số tổ</p>
          <h3 class="text-2xl font-black">{{ teams.length }}</h3>
        </div>
      </div>
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
            <th class="px-6 py-4">Tên tổ</th>
            <th class="px-6 py-4">Phòng ban</th>
            <th class="px-6 py-4">Công đoạn sản xuất</th>
            <th class="px-6 py-4 text-center">Thành viên</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="team in teams" :key="team.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4 text-sm font-black text-slate-400">#{{ team.id }}</td>
            <td class="px-6 py-4 font-bold text-slate-900">{{ team.name }}</td>
            <td class="px-6 py-4">
              <span class="text-sm font-bold text-slate-600">{{ team.department?.name || '---' }}</span>
            </td>
            <td class="px-6 py-4">
              <span class="px-2.5 py-1 rounded-full bg-primary-50 text-primary-600 text-[10px] font-black uppercase tracking-wider">
                {{ team.productionStep?.name || '---' }}
              </span>
            </td>
            <td class="px-6 py-4 text-center">
              <span class="font-bold text-slate-600">{{ team.memberCount || 0 }}</span>
            </td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-2">
                <button @click="openModal(team)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" title="Sửa">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(team.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" title="Xóa">
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
          <h3 class="text-xl font-black text-slate-900">{{ currentTeam.id ? 'Cập nhật' : 'Thêm' }} tổ sản xuất</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <UiSelect
            v-model="form.departmentId"
            label="Phòng ban"
            :options="deptOptions"
            placeholder="Chọn phòng ban..."
          />
          <UiSelect
            v-model="form.productionStepId"
            label="Công đoạn sản xuất"
            :options="stepOptions"
            placeholder="Chọn công đoạn..."
            required
          />
          <UiInput v-model="form.name" label="Tên tổ" placeholder="VD: Tổ Sấy Phôi" required />
          
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
const teams = ref([]);
const productionSteps = ref([]);
const departments = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const currentTeam = ref({});
const form = reactive({
  name: '',
  productionStepId: '',
  departmentId: ''
});

const stepOptions = computed(() => 
  productionSteps.value.map(step => ({
    label: step.name,
    value: step.id
  }))
);

const deptOptions = computed(() =>
  departments.value.map(d => ({
    label: d.name,
    value: d.id
  }))
);

const fetchData = async () => {
  loading.value = true;
  try {
    const [teamsRes, stepsRes, deptsRes] = await Promise.all([
      $api.get('/teams'),
      $api.get('/production-steps'),
      $api.get('/departments')
    ]);
    teams.value = teamsRes.data;
    productionSteps.value = stepsRes.data;
    departments.value = deptsRes.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const openModal = (team = null) => {
  if (team) {
    currentTeam.value = { ...team };
    form.name = team.name;
    form.productionStepId = team.productionStep?.id || '';
    form.departmentId = team.department?.id || '';
  } else {
    currentTeam.value = {};
    form.name = '';
    form.productionStepId = '';
    form.departmentId = '';
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    const payload = {
      name: form.name,
      productionStepId: parseInt(form.productionStepId),
      departmentId: form.departmentId ? parseInt(form.departmentId) : null
    };
    
    if (currentTeam.value.id) {
      await $api.put(`/teams/${currentTeam.value.id}`, payload);
    } else {
      await $api.post('/teams', payload);
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
  if (!confirm('Bạn có chắc chắn muốn xóa tổ này?')) return;
  try {
    await $api.delete(`/teams/${id}`);
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  }
};

onMounted(fetchData);
</script>
