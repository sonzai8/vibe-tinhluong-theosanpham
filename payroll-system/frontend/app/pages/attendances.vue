<template>
  <div class="space-y-8">
    <!-- Header with Breadcrumbs & Actions -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-3xl font-black text-slate-900 tracking-tight">Nhật ký Chấm công</h2>
        <p class="text-slate-500 font-medium">Ghi lại danh sách nhân viên đi làm hàng ngày</p>
      </div>
      <div class="flex gap-3">
        <UiButton @click="openModal()" class="shadow-lg shadow-emerald-100">
          <CalendarPlus class="w-4 h-4" />
          Chấm công mới
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

            <UiButton @click="fetchAttendances" class="w-full h-11 bg-slate-900 hover:bg-slate-800 transition-all font-black uppercase tracking-widest text-xs">
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
              <p class="text-[10px] font-black text-primary-600 uppercase">Quân số đi làm</p>
              <p class="text-lg font-black text-slate-900">{{ statistics.present }} / {{ statistics.total }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Attendance Table -->
      <div class="lg:col-span-3 card">
        <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
          <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
          <p class="text-slate-500 font-bold animate-pulse">Đang lấy dữ liệu chấm công...</p>
        </div>

        <div v-else-if="attendances.length === 0" class="p-20 text-center space-y-4">
          <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-300">
            <CalendarX class="w-10 h-10" />
          </div>
          <p class="text-slate-500 font-bold">Không tìm thấy dữ liệu trong ngày {{ filterDate }}.</p>
          <UiButton @click="openModal()" variant="outline">Chấm công ngay</UiButton>
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
            required
          />
            
            <UiInput v-model="form.attendanceDate" label="Ngày chấm công" type="date" required />

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
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
  </div>
</template>

<script setup>
import { CalendarPlus, Search, CalendarX, CheckCircle2, PencilLine, Trash2, X, Info } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const attendances = ref([]);
const employees = ref([]);
const teams = ref([]);

const employeeOptions = computed(() => employees.value.map(e => ({ value: e.id, label: e.fullName })));
const teamOptions = computed(() => teams.value.map(t => ({ value: t.id, label: t.name })));

const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const filterDate = ref(new Date().toISOString().substr(0, 10));
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
    const [attRes, empRes, teamRes] = await Promise.all([
      $api.get(`/attendances/date/${filterDate.value}`),
      $api.get('/employees'),
      $api.get('/teams')
    ]);
    attendances.value = attRes.data;
    employees.value = empRes.data;
    teams.value = teamRes.data;
  } catch (err) {
    console.error(err);
    // fallback if date endpoint fails
    if (err.response?.status === 404) attendances.value = [];
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
    alert(err.response?.data?.message || err.message || 'Lỗi xử lý');
  } finally {
    saving.value = false;
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
