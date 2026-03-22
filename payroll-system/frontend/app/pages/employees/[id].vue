<template>
  <div class="space-y-6 max-w-5xl mx-auto pb-12">
    <!-- Header with Back Button -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <button @click="handleBack" class="p-2 hover:bg-slate-100 rounded-xl transition-all">
          <ChevronLeft class="w-6 h-6 text-slate-600" />
        </button>
        <div>
          <h2 class="text-2xl font-black text-slate-900 leading-none mb-1">Chi tiết Nhân viên</h2>
          <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ employee.fullName }} ({{ employee.code }})</p>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <UiButton variant="outline" @click="handleBack">
          Hủy
        </UiButton>
        <UiButton @click="handleSave" :loading="saving">
          <Save class="w-4 h-4" />
          Lưu thay đổi
        </UiButton>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Left Column: Avatar & Basic Info -->
      <div class="space-y-6">
        <div class="card p-8 flex flex-col items-center">
          <div class="relative group cursor-pointer" @click="$refs.fileInput.click()">
            <div class="w-32 h-32 rounded-3xl bg-slate-100 overflow-hidden border-4 border-white shadow-xl ring-1 ring-slate-100 flex items-center justify-center">
              <img v-if="employee.avatarUrl" :src="fullAvatarUrl" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full flex items-center justify-center text-slate-300">
                <User class="w-16 h-16" />
              </div>
            </div>
            <div class="absolute inset-x-0 bottom-0 bg-slate-900/60 backdrop-blur-sm p-1.5 opacity-0 group-hover:opacity-100 transition-all rounded-b-3xl flex justify-center">
              <Camera class="w-4 h-4 text-white" />
            </div>
            <input type="file" ref="fileInput" class="hidden" accept="image/*" @change="handleAvatarUpload" />
          </div>
          <p class="mt-4 font-black text-slate-900 tracking-tight">{{ employee.fullName }}</p>
          <p class="text-[10px] font-black text-emerald-600 uppercase tracking-widest mt-1">{{ employee.role?.name || 'Vị trí chưa cập nhật' }}</p>
          
          <div class="w-full mt-8 space-y-4">
             <div class="flex items-center justify-between text-[10px] font-black uppercase tracking-widest border-b border-slate-50 pb-2">
                <span class="text-slate-400">Trạng thái</span>
                <span :class="employee.status === 'ACTIVE' ? 'text-emerald-500' : 'text-slate-400'">{{ employee.status }}</span>
             </div>
             <div class="flex items-center justify-between text-[10px] font-black uppercase tracking-widest border-b border-slate-50 pb-2">
                <span class="text-slate-400">Bộ phận</span>
                <span class="text-slate-700">{{ employee.department?.name || '---' }}</span>
             </div>
             <div class="flex items-center justify-between text-[10px] font-black uppercase tracking-widest border-b border-slate-50 pb-2">
                <span class="text-slate-400">Ngày tham gia</span>
                <span class="text-slate-700">{{ employee.joinDate || '---' }}</span>
             </div>
          </div>
        </div>

        <div class="card p-6 space-y-4">
           <h3 class="text-xs font-black text-slate-900 uppercase tracking-widest border-b border-slate-50 pb-3">Tài khoản hệ thống</h3>
           <div class="space-y-3">
              <div>
                <p class="text-[9px] font-black text-slate-400 uppercase tracking-widest mb-1">Tên đăng nhập</p>
                <p class="text-xs font-bold text-slate-700">{{ employee.username || '---' }}</p>
              </div>
              <div class="flex items-center justify-between">
                <p class="text-[9px] font-black text-slate-400 uppercase tracking-widest">Quyền đăng nhập</p>
                <span :class="employee.canLogin ? 'text-emerald-500 font-black text-[10px]' : 'text-slate-400 font-bold text-[10px]'">
                  {{ employee.canLogin ? 'CHO PHÉP' : 'TỪ CHỐI' }}
                </span>
              </div>
           </div>
        </div>
      </div>

      <!-- Right Column: Form -->
      <div class="lg:col-span-2 space-y-6">
        <div class="card p-8">
          <h3 class="text-sm font-black text-slate-900 uppercase tracking-widest mb-6 pb-2 border-b border-slate-100">Thông tin cá nhân</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
             <UiInput v-model="form.fullName" label="Họ và tên" required />
             <div class="space-y-1.5">
                <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Giới tính</label>
                <select v-model="form.gender" class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 outline-none focus:bg-white focus:border-slate-200 transition-all">
                  <option value="MALE">Nam</option>
                  <option value="FEMALE">Nữ</option>
                  <option value="OTHER">Khác</option>
                </select>
             </div>
             <UiInput v-model="form.dob" type="date" label="Ngày sinh" />
             <UiInput v-model="form.phone" label="Số điện thoại" />
             <UiInput v-model="form.citizenId" label="Số CCCD" />
             <UiInput v-model="form.citizenIdIssuedDate" type="date" label="Ngày cấp CCCD" />
             <div class="col-span-2">
                <UiInput v-model="form.citizenIdIssuedPlace" label="Nơi cấp" />
             </div>
             <div class="col-span-2">
                <UiInput v-model="form.birthAddress" label="Địa chỉ khai sinh" />
             </div>
             <div class="col-span-2">
                <UiInput v-model="form.permanentAddress" label="Địa chỉ thường trú" />
             </div>
          </div>

          <h3 class="text-sm font-black text-slate-900 uppercase tracking-widest mt-10 mb-6 pb-2 border-b border-slate-100">Công việc & BHXH</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
             <UiInput v-model="form.joinDate" type="date" label="Ngày vào công ty" />
             <UiInput v-model="form.insuranceStartDate" type="date" label="Ngày bắt đầu đóng BHXH" />
          </div>

          <h3 class="text-sm font-black text-slate-900 uppercase tracking-widest mt-10 mb-6 pb-2 border-b border-slate-100">Ghi chú</h3>
          <textarea v-model="form.notes" rows="4" class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-3 text-sm font-medium text-slate-700 outline-none focus:bg-white focus:border-slate-200 transition-all resize-none" placeholder="Nhập ghi chú chi tiết..."></textarea>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ChevronLeft, Save, User, Camera } from 'lucide-vue-next';

const route = useRoute();
const router = useRouter();
const { $api } = useNuxtApp();

const employee = ref({});
const saving = ref(false);

const form = reactive({
  fullName: '',
  gender: 'MALE',
  dob: '',
  phone: '',
  citizenId: '',
  citizenIdIssuedDate: '',
  citizenIdIssuedPlace: '',
  birthAddress: '',
  permanentAddress: '',
  joinDate: '',
  insuranceStartDate: '',
  notes: '',
});

const fullAvatarUrl = computed(() => {
  if (!employee.value.avatarUrl) return '';
  return `http://localhost:8080${employee.value.avatarUrl}`;
});

const fetchData = async () => {
  try {
    const res = await $api.get(`/employees/${route.params.id}`);
    employee.value = res.data;
    
    // Copy data to form
    Object.keys(form).forEach(key => {
      form[key] = res.data[key] || '';
    });
    
    if (!form.gender) form.gender = 'MALE';
  } catch (err) {
    console.error(err);
    alert('Không thể tải thông tin nhân viên');
  }
};

const handleSave = async () => {
  saving.value = true;
  try {
    const payload = { 
      ...employee.value, 
      ...form,
      departmentId: employee.value.department?.id,
      teamId: employee.value.team?.id,
      roleId: employee.value.role?.id
    };
    await $api.put(`/employees/${employee.value.id}`, payload);
    fetchData();
    alert('Cập nhật thành công');
  } catch (err) {
    alert(err.response?.data?.message || 'Lỗi khi lưu thông tin');
  } finally {
    saving.value = false;
  }
};

const handleAvatarUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  if (file.size > 2 * 1024 * 1024) {
    alert('Ảnh đại diện không được vượt quá 2MB');
    return;
  }

  const formData = new FormData();
  formData.append('file', file);

  try {
    await $api.post(`/employees/${employee.value.id}/avatar`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || 'Lỗi khi tải ảnh lên');
  }
};

const handleBack = () => router.push('/employees');

onMounted(fetchData);
</script>
