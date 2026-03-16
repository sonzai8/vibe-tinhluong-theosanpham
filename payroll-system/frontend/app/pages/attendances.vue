<template>
  <div class="space-y-8">
    <!-- Header with Breadcrumbs & Actions -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-3xl font-black text-slate-900 tracking-tight">Nhật ký Chấm công</h2>
        <p class="text-slate-500 font-medium">Ghi lại giờ công hàng ngày của toàn xưởng</p>
      </div>
      <div class="flex gap-3">
        <UiButton @click="openModal()" class="shadow-lg shadow-emerald-100">
          <CalendarPlus class="w-4 h-4" />
          Chấm công hôm nay
        </UiButton>
      </div>
    </div>

    <!-- Filters & Summary -->
    <div class="grid grid-cols-1 lg:grid-cols-4 gap-8">
      <!-- Fast Filter Sidebar -->
      <div class="lg:col-span-1 space-y-6">
        <div class="card p-6 space-y-6">
          <h3 class="font-black text-slate-900 text-sm uppercase tracking-widest">Bộ lọc nhanh</h3>
          
          <div class="space-y-4">
            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-black text-slate-400 uppercase">Ngày chấm công</label>
              <input v-model="filterDate" type="date" class="input-field py-2 text-sm font-bold" />
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-black text-slate-400 uppercase">Tìm nhân viên</label>
              <div class="relative">
                <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-300" />
                <input v-model="search" type="text" placeholder="Tên hoặc mã..." class="input-field py-2 pl-9 text-sm" />
              </div>
            </div>

            <UiButton @click="fetchAttendances" class="w-full h-11 bg-slate-900 hover:bg-slate-800">
              Lọc dữ liệu
            </UiButton>
          </div>
        </div>

        <!-- Static Summary Stats -->
        <div class="card p-6 bg-primary-50 border-primary-100 space-y-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary-600 flex items-center justify-center text-white">
              <CheckCircle2 class="w-5 h-5" />
            </div>
            <div>
              <p class="text-[10px] font-black text-primary-600 uppercase">Tổng quân số</p>
              <p class="text-lg font-black text-slate-900">{{ statistics.present }} / {{ statistics.total }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Attendance Table -->
      <div class="lg:col-span-3 card">
        <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
          <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
          <p class="text-slate-500 font-bold">Đang lấy dữ liệu chấm công...</p>
        </div>

        <div v-else-if="attendances.length === 0" class="p-20 text-center space-y-4">
          <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-300">
            <CalendarX class="w-10 h-10" />
          </div>
          <p class="text-slate-500 font-bold">Không tìm thấy dữ liệu trong ngày này.</p>
          <UiButton @click="openModal()" variant="outline">Tạo bản ghi mới</UiButton>
        </div>

        <div v-else class="overflow-x-auto">
          <table class="w-full text-left">
            <thead>
              <tr class="bg-slate-50/50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
                <th class="px-8 py-5">Nhân viên</th>
                <th class="px-8 py-5">Giờ công</th>
                <th class="px-8 py-5">Tổ sản xuất</th>
                <th class="px-8 py-5">Ghi chú</th>
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
                      <p class="text-[10px] font-bold text-slate-400 uppercase">{{ att.employee?.code }}</p>
                    </div>
                  </div>
                </td>
                <td class="px-8 py-5 font-black text-primary-700">{{ att.workHours }} <span class="text-[10px] text-slate-400 ml-1">giờ</span></td>
                <td class="px-8 py-5">
                  <span class="px-3 py-1 bg-slate-100 rounded-full text-[10px] font-black text-slate-500 uppercase">{{ att.team?.name || 'Văn phòng' }}</span>
                </td>
                <td class="px-8 py-5 text-sm text-slate-400 italic">{{ att.note || '---' }}</td>
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
    </div>

    <!-- Attendance Modal (Simplified) -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-xl p-10 animate-in zoom-in slide-in-from-bottom duration-300">
        <div class="flex items-center justify-between mb-10">
          <h3 class="text-2xl font-black text-slate-900 tracking-tight">Báo cáo chấm công</h3>
          <button @click="showModal = false" class="p-2.5 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full transition-all"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-black text-slate-700 ml-1">Chọn nhân viên</label>
              <select v-model="form.employeeId" class="input-field h-12" required>
                <option :value="null" disabled>--- Chọn nhân viên ---</option>
                <option v-for="e in employees" :key="e.id" :value="e.id">{{ e.fullName }} ({{ e.code }})</option>
              </select>
            </div>
            
            <UiInput v-model="form.date" label="Ngày chấm công" type="date" required />
            
            <UiInput v-model="form.workHours" label="Số giờ làm việc" type="number" step="0.5" placeholder="VD: 8.5" required />
            
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-black text-slate-700 ml-1">Tổ làm việc hôm nay</label>
              <select v-model="form.teamId" class="input-field h-12">
                <option :value="null">Mặc định / Văn phòng</option>
                <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.name }}</option>
              </select>
            </div>
          </div>

          <UiInput v-model="form.note" label="Ghi chú thêm" placeholder="Ví dụ: Làm thêm giờ, nghỉ sớm..." />
          
          <div class="flex gap-4 pt-6">
            <button type="button" @click="showModal = false" class="flex-1 py-3.5 rounded-2xl border border-slate-200 text-slate-500 font-black hover:bg-slate-50 transition-all">Đóng</button>
            <UiButton type="submit" class="flex-[2] h-14 text-lg shadow-xl shadow-primary-200" :loading="saving">Xác nhận chấm công</UiButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { CalendarPlus, Search, CalendarX, CheckCircle2, PencilLine, Trash2, X } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const attendances = ref([]);
const employees = ref([]);
const teams = ref([]);
const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const filterDate = ref(new Date().toISOString().substr(0, 10));
const search = ref('');

const form = reactive({
  employeeId: null,
  teamId: null,
  date: filterDate.value,
  workHours: 8,
  note: ''
});

const currentId = ref(null);

const fetchData = async () => {
  loading.value = true;
  try {
    const [attRes, empRes, teamRes] = await Promise.all([
      $api.get('/attendances', { params: { date: filterDate.value } }),
      $api.get('/employees'),
      $api.get('/teams')
    ]);
    attendances.value = attRes.data;
    employees.value = empRes.data;
    teams.value = teamRes.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const fetchAttendances = () => fetchData();

const filteredAttendances = computed(() => {
  if (!search.value) return attendances.value;
  return attendances.value.filter(a => 
    a.employee?.fullName.toLowerCase().includes(search.value.toLowerCase()) ||
    a.employee?.code.toLowerCase().includes(search.value.toLowerCase())
  );
});

const statistics = computed(() => {
  return {
    present: attendances.value.length,
    total: employees.value.length
  };
});

const openModal = (att = null) => {
  if (att) {
    currentId.value = att.id;
    form.employeeId = att.employee?.id;
    form.teamId = att.team?.id;
    form.date = att.date;
    form.workHours = att.workHours;
    form.note = att.note;
  } else {
    currentId.value = null;
    form.employeeId = null;
    form.teamId = null;
    form.date = filterDate.value;
    form.workHours = 8;
    form.note = '';
  }
  showModal.value = true;
};

const handleSubmit = async () => {
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
    alert(err.message || 'Lỗi xử lý');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Xóa bản ghi chấm công này?')) return;
  try {
    await $api.delete(`/attendances/${id}`);
    fetchData();
  } catch (err) {
    alert(err.message);
  }
};

onMounted(fetchData);
</script>
