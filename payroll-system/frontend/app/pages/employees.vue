<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Quản lý Nhân viên</h2>
        <p class="text-slate-500 font-medium">Danh sách toàn bộ cán bộ công nhân viên trong xưởng</p>
      </div>
      <UiButton @click="openModal()">
        <UserPlus class="w-4 h-4" />
        Thêm nhân viên
      </UiButton>
    </div>

    <!-- Filters & Search -->
    <div class="card p-4 flex flex-col md:flex-row gap-4 items-center">
      <div class="relative flex-1 w-full">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
        <input 
          v-model="searchQuery"
          type="text" 
          placeholder="Tìm theo mã hoặc tên nhân viên..." 
          class="w-full pl-10 pr-4 py-2 bg-slate-50 border-none rounded-lg focus:ring-2 focus:ring-primary-500 transition-all font-medium text-sm"
        />
      </div>
      <div class="flex gap-3 w-full md:w-auto">
        <select v-model="filterDept" class="bg-slate-50 border-none rounded-lg px-4 py-2 text-sm font-bold text-slate-600 focus:ring-2 focus:ring-primary-500">
          <option value="">Tất cả phòng ban</option>
          <option v-for="d in departments" :key="d.id" :value="d.id">{{ d.name }}</option>
        </select>
        <select v-model="filterStatus" class="bg-slate-50 border-none rounded-lg px-4 py-2 text-sm font-bold text-slate-600 focus:ring-2 focus:ring-primary-500">
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Đang làm việc</option>
          <option value="INACTIVE">Đã nghỉ việc</option>
        </select>
      </div>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">Đang tải danh sách nhân viên...</p>
      </div>

      <table v-else class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-6 py-4">Nhân viên</th>
            <th class="px-6 py-4">Phòng ban</th>
            <th class="px-6 py-4">Tổ đội</th>
            <th class="px-6 py-4">Chức vụ</th>
            <th class="px-6 py-4">Trạng thái</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="emp in filteredEmployees" :key="emp.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-black text-xs">
                  {{ emp.fullName.substring(0, 1).toUpperCase() }}
                </div>
                <div>
                  <p class="font-bold text-slate-900">{{ emp.fullName }}</p>
                  <p class="text-[10px] font-black text-slate-400 uppercase tracking-tighter">{{ emp.code }}</p>
                </div>
              </div>
            </td>
            <td class="px-6 py-4">
              <span class="text-sm font-bold text-slate-600">{{ emp.department?.name || emp.team?.department?.name || '---' }}</span>
            </td>
            <td class="px-6 py-4">
              <span v-if="emp.team" class="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[10px] font-black uppercase tracking-wider">
                {{ emp.team.name }}
              </span>
              <span v-else class="text-sm text-slate-400">---</span>
            </td>
            <td class="px-6 py-4">
              <span class="text-sm font-medium text-slate-500">{{ emp.role?.name || '---' }}</span>
            </td>
            <td class="px-6 py-4">
              <div class="flex items-center gap-1.5">
                <div :class="`w-2 h-2 rounded-full ${emp.status === 'ACTIVE' ? 'bg-emerald-500 shadow-sm shadow-emerald-200' : 'bg-slate-300'}`"></div>
                <span :class="`text-xs font-black uppercase tracking-wider ${emp.status === 'ACTIVE' ? 'text-emerald-600' : 'text-slate-400'}`">
                  {{ emp.status === 'ACTIVE' ? 'Đang làm' : 'Đã nghỉ' }}
                </span>
              </div>
            </td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-2 text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(emp)" class="p-2 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" title="Sửa">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(emp.id)" class="p-2 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" title="Xóa">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Employee Modal (Simplified) -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-2xl p-8 animate-in zoom-in duration-200 h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-8 sticky top-0 bg-white z-10 pb-4">
          <h3 class="text-2xl font-black text-slate-900">{{ currentEmp.id ? 'Sửa thông tin' : 'Thêm mới' }} nhân viên</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-6 h-6" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <UiInput v-model="form.code" label="Mã nhân viên" placeholder="VD: NV001" required />
            <UiInput v-model="form.fullName" label="Họ và tên" placeholder="VD: Nguyễn Văn A" required />
            <UiInput v-model="form.password" type="password" label="Mật khẩu" :placeholder="currentEmp.id ? 'Để trống nếu không đổi' : 'Nhập mật khẩu'" :required="!currentEmp.id" />
            
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-slate-700 ml-1">Trạng thái</label>
              <select v-model="form.status" class="input-field py-2.5">
                <option value="ACTIVE">Đang làm việc</option>
                <option value="INACTIVE">Đã nghỉ việc</option>
              </select>
            </div>

            <UiSelect 
              v-model="form.departmentId" 
              label="Phòng ban" 
              :options="deptOptions" 
              placeholder="Chọn phòng ban"
            />

            <UiSelect 
              v-model="form.teamId" 
              label="Tổ đội" 
              :options="teamOptions" 
              placeholder="Chọn tổ đội"
            />

            <UiSelect 
              v-model="form.roleId" 
              label="Chức vụ" 
              :options="roleOptions" 
              placeholder="Chọn chức vụ"
            />
          </div>
          
          <div class="flex gap-4 pt-4 border-t border-slate-100">
            <button type="button" @click="showModal = false" class="flex-1 py-3 rounded-xl border border-slate-200 text-slate-600 font-extrabold hover:bg-slate-50 transition-all">Bỏ qua</button>
            <UiButton type="submit" class="flex-[2] py-3 text-lg font-black" :loading="saving">Lưu nhân viên</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, UserPlus, PencilLine, Trash2, X, Search, Briefcase } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const employees = ref([]);
const departments = ref([]);
const roles = ref([]);
const teams = ref([]);

const deptOptions = computed(() => departments.value.map(d => ({ value: d.id, label: d.name })));
const roleOptions = computed(() => roles.value.map(r => ({ value: r.id, label: r.name })));
const teamOptions = computed(() => teams.value.map(t => ({ value: t.id, label: t.name })));

const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const searchQuery = ref('');
const filterDept = ref('');
const filterStatus = ref('');

const currentEmp = ref({});
const form = reactive({
  code: '',
  fullName: '',
  username: '',
  password: '',
  status: 'ACTIVE',
  departmentId: null,
  teamId: null,
  roleId: null
});

// Tự động chọn phòng ban khi chọn tổ đội
watch(() => form.teamId, (newTeamId) => {
  if (newTeamId && teams.value.length > 0) {
    const selectedTeam = teams.value.find(t => t.id === newTeamId);
    if (selectedTeam && selectedTeam.department) {
      form.departmentId = selectedTeam.department.id;
    }
  }
});

const fetchData = async () => {
  loading.value = true;
  try {
    const [empRes, deptRes, roleRes, teamRes] = await Promise.all([
      $api.get('/employees'),
      $api.get('/departments'),
      $api.get('/roles'),
      $api.get('/teams')
    ]);
    employees.value = empRes.data;
    departments.value = deptRes.data;
    roles.value = roleRes.data;
    teams.value = teamRes.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const filteredEmployees = computed(() => {
  return employees.value.filter(e => {
    const matchSearch = e.fullName.toLowerCase().includes(searchQuery.value.toLowerCase()) || 
                      e.code.toLowerCase().includes(searchQuery.value.toLowerCase());
    const matchDept = !filterDept.value || e.department?.id === filterDept.value;
    const matchStatus = !filterStatus.value || e.status === filterStatus.value;
    return matchSearch && matchDept && matchStatus;
  });
});

const openModal = (emp = null) => {
  if (emp) {
    currentEmp.value = { ...emp };
    form.code = emp.code;
    form.fullName = emp.fullName;
    form.username = emp.username;
    form.password = '';
    form.status = emp.status;
    form.departmentId = emp.department?.id || null;
    form.teamId = emp.team?.id || null;
    form.roleId = emp.role?.id || null;
  } else {
    currentEmp.value = {};
    form.code = '';
    form.fullName = '';
    form.username = '';
    form.password = '';
    form.status = 'ACTIVE';
    form.departmentId = null;
    form.teamId = null;
    form.roleId = null;
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    // If password is empty in edit mode, don't send it or handle based on backend
    const payload = { ...form };
    if (currentEmp.value.id && !payload.password) {
      delete payload.password;
    }

    if (currentEmp.value.id) {
      await $api.put(`/employees/${currentEmp.value.id}`, payload);
    } else {
      await $api.post('/employees', payload);
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
  if (!confirm('Bạn có chắc chắn muốn xóa nhân viên này?')) return;
  try {
    await $api.delete(`/employees/${id}`);
    fetchData();
  } catch (err) {
    alert(err.message || 'Có lỗi xảy ra');
  }
};

onMounted(fetchData);
</script>
