<template>
  <div class="space-y-8">
    <!-- Header with Breadcrumbs & Actions -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-3xl font-black text-slate-900 tracking-tight">Nhật ký Chấm công</h2>
        <p class="text-slate-500 font-medium">Ghi lại danh sách nhân viên đi làm hàng ngày</p>
      </div>
      <div class="flex gap-3">
        <UiButton variant="outline" @click="handleDownloadTemplate" >
          <FileSpreadsheet class="w-4 h-4" />
          Tải file mẫu
        </UiButton>
        <UiButton variant="outline" @click="handleExport" :loading="exporting">
          <Download class="w-4 h-4" />
          Xuất Excel
        </UiButton>
        <UiButton variant="outline" @click="$refs.fileInput.click()" :loading="importing">
          <Upload class="w-4 h-4" />
          Nhập Excel
        </UiButton>
        <input type="file" ref="fileInput" class="hidden" accept=".xlsx, .xls" @change="handleImport" />
        
        <UiButton @click="() => openModal(null)" class="shadow-lg shadow-emerald-100">
          <CalendarPlus class="w-4 h-4" />
          Chấm công mới
        </UiButton>
      </div>
    </div>

    <!-- Filters & Summary -->
    <div class="space-y-6">
      
      <!-- Top Filter Bar -->
      <div class="card p-6 flex flex-col lg:flex-row gap-4 lg:items-end justify-between">
        <div class="flex flex-col md:flex-row gap-4 w-full lg:w-3/4">
          <div class="flex flex-col gap-1.5 flex-1">
            <label class="text-xs font-black text-slate-400 uppercase">Ngày chấm công</label>
            <input v-model="filterDate" type="date" class="input-field py-2.5 text-sm font-bold w-full" @change="fetchData" />
          </div>

          <div class="flex flex-col gap-1.5 flex-[1.2]">
            <label class="text-xs font-black text-slate-400 uppercase">Phòng ban</label>
            <UiSelect 
              v-model="filterDept"
              :options="deptOptions"
              placeholder="Tất cả phòng ban"
              class="w-full"
            />
          </div>

          <div class="flex flex-col gap-1.5 flex-[1.2]">
            <label class="text-xs font-black text-slate-400 uppercase">Tổ đội</label>
            <UiSelect 
              v-model="filterTeam"
              :options="teamFilterOptions"
              placeholder="Tất cả tổ đội"
              class="w-full"
            />
          </div>

          <div class="flex flex-col gap-1.5 flex-[1.5]">
            <label class="text-xs font-black text-slate-400 uppercase">Tìm nhân viên</label>
            <div class="relative w-full">
              <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-300" />
              <input v-model="search" type="text" placeholder="Tên hoặc mã nhân viên..." class="input-field py-2.5 pl-9 text-sm w-full" />
            </div>
          </div>
        </div>
        
        <div class="flex gap-4 w-full xl:w-1/4 justify-end shrink-0">
          <UiButton @click="showAbsentModal = true" variant="outline" class="h-[42px] px-4 font-bold text-xs" v-if="absentEmployees.length > 0">
            <UserX class="w-4 h-4 mr-1.5 text-amber-500" /> Vắng ({{ absentEmployees.length }})
          </UiButton>
          <UiButton @click="fetchAttendances" class="h-[42px] px-6 bg-slate-900 hover:bg-slate-800 transition-all font-black uppercase tracking-widest text-xs">
            Lọc
          </UiButton>
        </div>
      </div>

      <!-- Main Layout: Table + Summary Stats -->
      <div class="flex flex-col xl:flex-row gap-6">
        
        <!-- Attendance Table -->
        <div class="card w-full xl:w-3/4">
          <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
            <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
            <p class="text-slate-500 font-bold animate-pulse">Đang lấy dữ liệu chấm công...</p>
          </div>

          <div v-else-if="attendances.length === 0" class="p-20 text-center space-y-4">
            <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-300">
              <CalendarX class="w-10 h-10" />
            </div>
            <p class="text-slate-500 font-bold">Không tìm thấy dữ liệu trong ngày {{ filterDate }}.</p>
            <UiButton @click="() => openModal(null)" variant="outline">Chấm công ngay</UiButton>
          </div>

          <div v-else class="overflow-x-auto">
            <table class="w-full text-left">
              <thead>
                <tr class="bg-slate-50/50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
                  <th class="px-8 py-5">Nhân viên</th>
                  <th class="px-8 py-5">Tổ biên chế</th>
                  <th class="px-8 py-5">Tổ thực tế</th>
                  <th class="px-8 py-5">Trạng thái</th>
                  <th class="px-8 py-5 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr v-for="att in filteredAttendances" :key="att.id" class="hover:bg-slate-50/50 transition-all group">
                  <td class="px-8 py-5">
                    <div class="flex items-center gap-4">
                      <div class="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center text-xs font-black text-slate-400 group-hover:bg-primary-600 group-hover:text-white transition-all shadow-sm">
                        {{ att.employee?.fullName[0] }}
                      </div>
                      <div>
                        <p class="font-black text-slate-900">{{ att.employee?.fullName }}</p>
                        <p class="text-[10px] font-bold text-slate-400 uppercase tracking-tighter">{{ att.employee?.code }}</p>
                      </div>
                    </div>
                  </td>
                  <td class="px-8 py-5">
                    <span v-if="att.originalTeam" class="px-3 py-1 bg-slate-100 rounded-full text-[10px] font-black text-slate-500 uppercase">{{ att.originalTeam?.name }}</span>
                    <span v-else class="text-slate-300 text-xs italic">Chưa gán tổ</span>
                  </td>
                  <td class="px-8 py-5">
                    <span v-if="att.actualTeam" :class="`px-3 py-1 rounded-full text-[10px] font-black uppercase ${att.actualTeam?.id !== att.originalTeam?.id ? 'bg-amber-100 text-amber-700' : 'bg-emerald-100 text-emerald-700'}`">
                      {{ att.actualTeam?.name }}
                      <span v-if="att.actualTeam?.id !== att.originalTeam?.id" class="ml-1 text-[8px] opacity-70">(Mượn)</span>
                    </span>
                  </td>
                  <td class="px-8 py-5">
                     <span class="flex items-center gap-1.5 text-[10px] font-black text-emerald-600 uppercase">
                       <div class="w-1.5 h-1.5 rounded-full bg-emerald-500"></div>
                       Đã chấm công
                     </span>
                  </td>
                  <td class="px-8 py-5 text-right">
                    <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button @click="openModal(att)" class="p-2.5 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-xl transition-all"><PencilLine class="w-4 h-4" /></button>
                      <button @click="handleDelete(att.id)" class="p-2.5 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-xl transition-all"><Trash2 class="w-4 h-4" /></button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Static Summary Stats Container Right -->
        <div class="w-full xl:w-1/4 space-y-6">
          <div class="card p-6 bg-primary-50 border-primary-100 space-y-4">
            <h3 class="font-black text-slate-900 text-sm uppercase tracking-widest mb-4">Tổng quan</h3>
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 rounded-xl bg-primary-600 flex items-center justify-center text-white">
                <CheckCircle2 class="w-6 h-6" />
              </div>
              <div>
                <p class="text-[10px] font-black text-primary-600 uppercase">Quân số đi làm</p>
                <p class="text-2xl font-black text-slate-900">{{ statistics.present }} / <span class="text-lg text-slate-400">{{ statistics.total }}</span></p>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- Attendance Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-xl p-10 animate-in zoom-in slide-in-from-bottom duration-300">
        <div class="flex items-center justify-between mb-10">
          <h3 class="text-2xl font-black text-slate-900 tracking-tight">{{ currentId ? 'Sửa' : 'Báo cáo' }} chấm công</h3>
          <button @click="showModal = false" class="p-2.5 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full transition-all"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
            <UiSelect 
              v-model="form.employeeId" 
              label="Nhân viên" 
              :options="employeeOptions" 
              placeholder="Chọn nhân viên"
              @update:modelValue="onEmployeeSelect"
              required
            />
            
            <UiInput v-model="form.attendanceDate" label="Ngày chấm công" type="date" required />

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 md:col-span-2">
              <UiSelect 
                v-model="form.originalTeamId" 
                label="Tổ biên chế" 
                :options="teamOptions" 
                placeholder="Chọn tổ"
                required
              />
              <UiSelect 
                v-model="form.actualTeamId" 
                label="Tổ thực tế làm việc" 
                :options="teamOptions" 
                placeholder="Chọn tổ"
                required
              />
            </div>
          </div>

          <div class="p-4 bg-amber-50 rounded-2xl border border-amber-100 flex gap-3">
             <Info class="w-5 h-5 text-amber-600 shrink-0" />
             <p class="text-xs text-amber-700 font-medium leading-relaxed">
               Ghi chú: Nếu "Tổ làm việc thực tế" khác với "Tổ biên chế", nhân viên sẽ được tính là "Công nhân đi mượn" cho ngày hôm đó.
             </p>
          </div>
          
          <div class="flex gap-4 pt-6">
            <button type="button" @click="showModal = false" class="flex-1 py-3.5 rounded-2xl border border-slate-200 text-slate-500 font-black hover:bg-slate-50 transition-all">Đóng</button>
            <UiButton type="submit" class="flex-[2] h-14 text-lg shadow-xl shadow-primary-200" :loading="saving">Xác nhận chấm công</UiButton>
          </div>
        </form>
      </div>
    </div>

    <!-- Absent Employees Modal -->
    <div v-if="showAbsentModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-2xl p-8 animate-in zoom-in slide-in-from-bottom duration-300 max-h-[90vh] flex flex-col">
        <div class="flex items-center justify-between mb-6 shrink-0">
          <div>
            <h3 class="text-2xl font-black text-slate-900 tracking-tight">Danh sách chưa chấm công</h3>
            <p class="text-sm text-slate-500 mt-1">Danh sách nhân viên Active nhưng chưa có dữ liệu chấm công ngày {{ filterDate }}</p>
          </div>
          <button @click="showAbsentModal = false" class="p-2.5 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full transition-all"><X class="w-5 h-5" /></button>
        </div>

        <div class="overflow-y-auto flex-1 border border-slate-100 rounded-xl">
          <table class="w-full text-left relative">
            <thead class="sticky top-0 bg-slate-50 border-b border-slate-200 z-10">
              <tr class="text-slate-500 text-[10px] font-black uppercase tracking-widest">
                <th class="px-6 py-4">Nhân viên</th>
                <th class="px-6 py-4">Tổ biên chế</th>
                <th class="px-6 py-4">SĐT</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-if="absentEmployees.length === 0" class="text-center">
                 <td colspan="3" class="px-6 py-8 text-sm text-slate-500">Tất cả nhân viên đã đi làm đầy đủ!</td>
              </tr>
              <tr v-for="emp in absentEmployees" :key="emp.id" class="hover:bg-slate-50 transition-colors">
                <td class="px-6 py-3">
                  <div class="font-bold text-slate-900 text-sm">{{ emp.fullName }}</div>
                  <div class="text-[10px] font-black text-slate-400">{{ emp.code }}</div>
                </td>
                <td class="px-6 py-3 text-xs text-slate-600">{{ emp.team ? emp.team.name : 'Chưa có tổ' }}</td>
                <td class="px-6 py-3 text-xs text-slate-500">{{ emp.phone || 'N/A' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mt-6 flex justify-end shrink-0">
          <UiButton @click="showAbsentModal = false" variant="outline" class="px-6">Đóng</UiButton>
        </div>
      </div>
    </div>
    <!-- Duplicate Warning Modal -->
    <div v-if="showDuplicateModal" class="fixed inset-0 z-[110] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-lg p-8 animate-in zoom-in slide-in-from-bottom duration-300">
        <div class="flex items-center gap-4 mb-6">
          <div class="w-12 h-12 rounded-full bg-amber-100 flex items-center justify-center text-amber-600 shrink-0">
            <Info class="w-6 h-6" />
          </div>
          <div>
            <h3 class="text-xl font-black text-slate-900 tracking-tight">Trùng lặp dữ liệu</h3>
            <p class="text-sm text-slate-500">Nhân viên này đã được chấm công.</p>
          </div>
        </div>
        
        <div class="bg-slate-50 p-4 rounded-2xl border border-slate-100 mb-6 space-y-2">
          <div class="flex justify-between text-sm">
            <span class="text-slate-500">Nhân viên:</span>
            <span class="font-bold text-slate-900">{{ duplicateRecord?.employee?.fullName }}</span>
          </div>
          <div class="flex justify-between text-sm">
            <span class="text-slate-500">Ngày chấm công:</span>
            <span class="font-bold text-slate-900">{{ duplicateRecord?.attendanceDate }}</span>
          </div>
          <div class="flex justify-between text-sm">
            <span class="text-slate-500">Tổ thực tế:</span>
            <span class="font-bold text-slate-900">{{ duplicateRecord?.actualTeam?.name || 'Không rõ' }}</span>
          </div>
        </div>

        <div class="flex gap-3">
          <UiButton @click="showDuplicateModal = false" variant="outline" class="flex-1 border-slate-200">Đóng lại</UiButton>
          <UiButton @click="switchToEditDuplicate" class="flex-1 bg-amber-500 hover:bg-amber-600 text-white border-none shadow-lg shadow-amber-200">
            Chuyển sang Sửa
          </UiButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { 
  CalendarPlus, Search, CalendarX, CheckCircle2, PencilLine, Trash2, X, Info,
  Download, Upload, FileSpreadsheet, UserX
} from 'lucide-vue-next';

const { $api } = useNuxtApp();
const attendances = ref([]);
const employees = ref([]);
const teams = ref([]);
const departments = ref([]);

const employeeOptions = computed(() => employees.value.map(e => ({ value: e.id, label: e.fullName })));
const teamOptions = computed(() => teams.value.map(t => ({ value: t.id, label: t.name })));
const teamFilterOptions = computed(() => [
  { value: '', label: 'Tất cả tổ đội' },
  ...teams.value.map(t => ({ value: t.id, label: t.name }))
]);
const deptOptions = computed(() => [
  { value: '', label: 'Tất cả phòng ban' },
  ...departments.value.map(d => ({ value: d.id, label: d.name }))
]);

const loading = ref(true);
const saving = ref(false);
const exporting = ref(false);
const importing = ref(false);
const showModal = ref(false);
const showAbsentModal = ref(false);
const showDuplicateModal = ref(false);
const duplicateRecord = ref(null);

const filterDate = ref(new Date().toISOString().substr(0, 10));
const filterDept = ref('');
const filterTeam = ref('');
const search = ref('');

const form = reactive({
  employeeId: null,
  originalTeamId: null,
  actualTeamId: null,
  attendanceDate: filterDate.value,
});

const currentId = ref(null);

const fetchData = async () => {
  loading.value = true;
  try {
    const [attRes, empRes, teamRes, deptRes] = await Promise.all([
      $api.get(`/attendances/date/${filterDate.value}`),
      $api.get('/employees'),
      $api.get('/teams'),
      $api.get('/departments')
    ]);
    attendances.value = attRes.data;
    employees.value = empRes.data;
    teams.value = teamRes.data;
    departments.value = deptRes.data;
  } catch (err) {
    console.error(err);
    // fallback if date endpoint fails
    if (err.response?.status === 404) attendances.value = [];
  } finally {
    loading.value = false;
  }
};

const fetchAttendances = () => fetchData();

const isFilterActive = (val) => val && val !== 'null' && val !== '';

const filteredAttendances = computed(() => {
  return attendances.value.filter(a => {
    // Tìm kiếm text
    const matchSearch = !search.value || 
                       a.employee?.fullName.toLowerCase().includes(search.value.toLowerCase()) ||
                       a.employee?.code.toLowerCase().includes(search.value.toLowerCase());
                       
    // Lọc theo phòng ban: Cần kiểm tra cả a.employee.department và a.employee.team.department vì backend trả về cấu trúc phân cấp
    const deptId = a.employee?.department?.id || a.employee?.team?.department?.id;
    const matchDept = !isFilterActive(filterDept.value) || deptId == filterDept.value;
    
    // Lọc theo tổ đội
    const teamId = a.employee?.team?.id || a.originalTeam?.id;
    const matchTeam = !isFilterActive(filterTeam.value) || teamId == filterTeam.value;
    
    return matchSearch && matchDept && matchTeam;
  });
});

const absentEmployees = computed(() => {
  const attendedIds = new Set(attendances.value.map(a => a.employee?.id));
  return employees.value.filter(e => {
    // Chỉ lấy nhân viên Đang làm việc (ACTIVE)
    if (e.status !== 'ACTIVE') return false;
    // Bỏ qua nếu đã chấm công
    if (attendedIds.has(e.id)) return false;
    
    // Áp dụng bộ lọc phòng ban/tổ đội nếu đang chọn
    const matchDept = !isFilterActive(filterDept.value) || (e.department?.id || e.team?.department?.id) == filterDept.value;
    const matchTeam = !isFilterActive(filterTeam.value) || e.team?.id == filterTeam.value;
    
    return matchDept && matchTeam;
  });
});

const statistics = computed(() => {
  // Quân số đi làm là danh sách đã filter
  const presentCount = filteredAttendances.value.length;
  // Tổng quân số (ACTIVE) tương ứng điều kiện lọc
  const totalCount = employees.value.filter(e => {
    if (e.status !== 'ACTIVE') return false;
    const matchDept = !isFilterActive(filterDept.value) || (e.department?.id || e.team?.department?.id) == filterDept.value;
    const matchTeam = !isFilterActive(filterTeam.value) || e.team?.id == filterTeam.value;
    return matchDept && matchTeam;
  }).length;
  
  return {
    present: presentCount,
    total: totalCount
  };
});

const openModal = (att = null) => {
  console.log("Button clicked, params:", att);
  
  // Nếu att có chứa id thực sự từ database, thì là chế độ SỬA
  if (att && typeof att === 'object' && att.id) {
    currentId.value = att.id;
    form.employeeId = att.employee?.id;
    form.originalTeamId = att.originalTeam?.id || null;
    form.actualTeamId = att.actualTeam?.id || null;
    form.attendanceDate = att.attendanceDate;
  } else {
    currentId.value = null;
    form.employeeId = null;
    form.originalTeamId = null;
    form.actualTeamId = null;
    form.attendanceDate = filterDate.value;
  }
  showModal.value = true;
};

const onEmployeeSelect = (empId) => {
  if (!empId) {
    form.originalTeamId = null;
    form.actualTeamId = null;
    return;
  }
  const emp = employees.value.find(e => e.id == empId);
  if (emp && emp.team) {
    form.originalTeamId = emp.team.id;
    form.actualTeamId = emp.team.id;
  }
};

const switchToEditDuplicate = () => {
  showDuplicateModal.value = false;
  openModal(duplicateRecord.value);
};

const handleSubmit = async () => {
  // Validate duplicate on frontend (if same date)
  if (!currentId.value) {
    const existing = attendances.value.find(
      a => a.employee?.id == form.employeeId && a.attendanceDate === form.attendanceDate
    );
    if (existing) {
      duplicateRecord.value = existing;
      showDuplicateModal.value = true;
      return;
    }
  }

  saving.value = true;
  try {
    if (currentId.value) {
      await $api.put(`/attendances/${currentId.value}`, form);
    } else {
      await $api.post('/attendances', form);
    }
    showModal.value = false;
    fetchData();
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '';
    if (msg.includes('Duplicate entry') || msg.includes('constraint')) {
      alert('Lỗi: Nhân viên này đã được chấm công trong ngày, dữ liệu bị trùng lặp ở Database!');
    } else {
      alert('Lỗi xử lý: ' + msg);
    }
  } finally {
    saving.value = false;
  }
};

const handleExport = async () => {
  exporting.value = true;
  try {
    const month = new Date(filterDate.value).getMonth() + 1;
    const year = new Date(filterDate.value).getFullYear();
    
    const response = await $api.get('/attendances/export', {
      params: { month, year, departmentId: filterDept.value },
      responseType: 'blob'
    });
    
    // Vì axios interceptor đã trả về response.data, nên ở đây data chính là Blob
    const url = window.URL.createObjectURL(new Blob([response]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `ChamCong_${month}_${year}.xlsx`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (err) {
    alert('Lỗi xuất file: ' + err.message);
  } finally {
    exporting.value = false;
  }
};

const handleDownloadTemplate = async () => {
  try {
    const response = await $api.get('/attendances/download-template', {
      responseType: 'blob'
    });
    
    // Vì axios interceptor đã trả về response.data, nên ở đây data chính là Blob
    const url = window.URL.createObjectURL(new Blob([response]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'Mau_Nhap_Cham_Cong.xlsx');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (err) {
    alert('Lỗi tải file mẫu: ' + err.message);
  }
};

const handleImport = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  const formData = new FormData();
  formData.append('file', file);
  
  importing.value = true;
  try {
    await $api.post('/attendances/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    alert('Nhập dữ liệu thành công!');
    fetchData();
  } catch (err) {
    alert('Lỗi nhập file: ' + err.response?.data?.message || err.message);
  } finally {
    importing.value = false;
    event.target.value = ''; // Reset input
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa bản ghi chấm công này?')) return;
  try {
    await $api.delete(`/attendances/${id}`);
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Lỗi');
  }
};

onMounted(fetchData);
</script>
