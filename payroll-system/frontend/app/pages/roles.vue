<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Quản lý Chức vụ</h2>
        <p class="text-slate-500 font-medium">Quản lý danh mục chức vụ, phụ cấp và phân quyền hệ thống</p>
      </div>
      <div v-if="hasPermission('SYSTEM_ADMIN')" class="flex items-center gap-2">
        <UiButton variant="outline" @click="handleExport">
          <Download class="w-4 h-4" />
          Xuất Excel
        </UiButton>
        <div class="relative group">
          <UiButton variant="outline">
            <Upload class="w-4 h-4" />
            Nhập Excel
          </UiButton>
          <div class="absolute right-0 top-full mt-2 w-48 bg-white border border-slate-200 rounded-xl shadow-xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50 p-2">
            <button @click="downloadTemplate" class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2">
              <FileDown class="w-4 h-4" />
              Tải file mẫu
            </button>
            <label class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2 cursor-pointer">
              <FileUp class="w-4 h-4" />
              Chọn file nhập
              <input type="file" class="hidden" accept=".xlsx, .xls" @change="handleImport" />
            </label>
          </div>
        </div>
        <UiButton @click="openModal()">
          <Plus class="w-4 h-4" />
          Thêm chức vụ
        </UiButton>
      </div>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">Đang tải danh sách...</p>
      </div>

      <table v-else class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-6 py-4">Chức vụ</th>
            <th class="px-6 py-4">Phụ cấp ngày</th>
            <th class="px-6 py-4">Quyền hạn</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="role in roles" :key="role.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4 text-sm font-bold text-slate-900">{{ role.name }}</td>
            <td class="px-6 py-4 text-sm font-medium text-slate-600">{{ formatCurrency(role.dailyBenefit) }}</td>
            <td class="px-6 py-4">
              <div class="flex flex-wrap gap-1">
                <span 
                  v-if="role.permissions?.includes('SYSTEM_ADMIN')" 
                  class="px-2 py-0.5 rounded-full bg-red-50 text-red-600 text-[10px] font-black uppercase tracking-wider border border-red-100"
                >
                  FULL ACCESS
                </span>
                <span 
                  v-else
                  v-for="perm in role.permissions" 
                  :key="perm"
                  class="px-2 py-0.5 rounded-full bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-wider border border-slate-100"
                >
                  {{ perm }}
                </span>
                <span v-if="!role.permissions?.length" class="text-[10px] text-slate-300 italic font-bold">Chưa gán quyền</span>
              </div>
            </td>
            <td class="px-6 py-4 text-right">
              <div v-if="hasPermission('SYSTEM_ADMIN')" class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="openModal(role)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="handleDelete(role.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all">
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
      <div class="card w-full max-w-2xl p-8 animate-in zoom-in duration-200 max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-8">
          <h3 class="text-2xl font-black text-slate-900">{{ currentRole.id ? 'Sửa' : 'Thêm' }} chức vụ</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-6 h-6" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <UiInput v-model="form.name" label="Tên chức vụ" placeholder="VD: Tổ trưởng" required />
            <UiInput v-model="form.dailyBenefit" type="number" label="Phụ cấp ngày" placeholder="VD: 50000" required />
          </div>

          <div class="space-y-4">
            <label class="text-sm font-black text-slate-700 ml-1">Phân quyền chi tiết</label>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 p-4 bg-slate-50 rounded-2xl border border-slate-100">
              <div v-for="group in permissionGroups" :key="group.title" class="space-y-3 col-span-full mb-3 first:mt-0 mt-4">
                <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest pl-1">{{ group.title }}</p>
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <label 
                    v-for="perm in group.perms" 
                    :key="perm.id"
                    class="flex items-center gap-3 p-3 bg-white rounded-xl border border-slate-100 cursor-pointer hover:border-primary-200 transition-all group/perm"
                  >
                    <input 
                      type="checkbox" 
                      :value="perm.id" 
                      v-model="form.permissions"
                      class="w-4 h-4 text-primary-600 border-slate-300 rounded focus:ring-primary-500"
                    >
                    <div class="flex flex-col">
                      <span class="text-xs font-black text-slate-700 group-hover/perm:text-primary-700 transition-colors">{{ perm.label }}</span>
                      <span class="text-[9px] font-medium text-slate-400">{{ perm.id }}</span>
                    </div>
                  </label>
                </div>
              </div>
              
              <!-- System Admin Special -->
              <div class="col-span-full mt-4 p-4 bg-red-50 rounded-xl border border-red-100">
                <label class="flex items-center gap-3 cursor-pointer">
                  <input 
                    type="checkbox" 
                    value="SYSTEM_ADMIN" 
                    v-model="form.permissions"
                    class="w-4 h-4 text-red-600 border-red-300 rounded focus:ring-red-500"
                  >
                  <div>
                    <p class="text-xs font-black text-red-700">QUẢN TRỊ TỐI CAO (SYSTEM_ADMIN)</p>
                    <p class="text-[9px] font-medium text-red-500/70 uppercase">Cấp toàn bộ quyền hạn cao nhất trong hệ thống</p>
                  </div>
                </label>
              </div>
            </div>
          </div>
          
          <div class="flex gap-4 pt-4">
            <button type="button" @click="showModal = false" class="flex-1 py-3 rounded-xl border border-slate-200 text-slate-600 font-extrabold hover:bg-slate-50 transition-all">Bỏ qua</button>
            <UiButton type="submit" class="flex-[2] py-3 text-lg font-black" :loading="saving">Lưu chức vụ</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, PencilLine, Trash2, X, Download, Upload, FileDown, FileUp } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const { hasPermission } = useAuth();
const { downloadTemplate: dlTemplate, importExcel, exportExcel } = useExcel();
const roles = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const handleExport = async () => {
  try {
    await exportExcel('/roles/export', 'danh_sach_chuc_vu.xlsx');
  } catch (err) {
    alert('Không thể xuất dữ liệu');
  }
};

const downloadTemplate = async () => {
  try {
    await dlTemplate('/roles/download-template', 'mau_nhap_chuc_vu.xlsx');
  } catch (err) {
    alert('Không thể tải file mẫu');
  }
};

const handleImport = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  try {
    loading.value = true;
    await importExcel('/roles/import', file);
    alert('Nhập dữ liệu thành công');
    fetchRoles();
  } catch (err) {
    alert(err.response?.data?.message || 'Lỗi khi nhập dữ liệu');
  } finally {
    loading.value = false;
    event.target.value = ''; // Reset input
  }
};
const currentRole = ref({});

const form = reactive({
  name: '',
  dailyBenefit: 0,
  permissions: []
});

const permissionGroups = [
  {
    title: 'Nhân viên',
    perms: [
      { id: 'EMPLOYEE_VIEW', label: 'Xem danh sách' },
      { id: 'EMPLOYEE_EDIT', label: 'Sửa / Import' },
    ]
  },
  {
    title: 'Chấm công',
    perms: [
      { id: 'ATTENDANCE_VIEW', label: 'Xem bảng công' },
      { id: 'ATTENDANCE_EDIT', label: 'Chỉnh sửa / Chốt công' },
    ]
  },
  {
    title: 'Sản lượng',
    perms: [
      { id: 'PRODUCTION_VIEW', label: 'Xem sản lượng' },
      { id: 'PRODUCTION_EDIT', label: 'Nhập sản lượng' },
    ]
  },
  {
    title: 'Lương & Thưởng',
    perms: [
      { id: 'PAYROLL_VIEW', label: 'Xem bảng lương' },
      { id: 'PAYROLL_EDIT', label: 'Duyệt lương / Cài đặt' },
    ]
  }
];

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
    form.name = role.name;
    form.dailyBenefit = role.dailyBenefit;
    form.permissions = role.permissions ? [...role.permissions] : [];
  } else {
    currentRole.value = {};
    form.name = '';
    form.dailyBenefit = 0;
    form.permissions = [];
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
    alert(err.response?.data?.message || 'Có lỗi xảy ra');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Xóa chức vụ này?')) return;
  try {
    await $api.delete(`/roles/${id}`);
    fetchRoles();
  } catch (err) {
    alert(err.message || 'Lỗi');
  }
};

const formatCurrency = (val) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
};

onMounted(fetchRoles);
</script>
