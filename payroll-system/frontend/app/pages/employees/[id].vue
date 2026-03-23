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

          <h3 class="text-sm font-black text-slate-900 uppercase tracking-widest mt-10 mb-6 pb-2 border-b border-slate-100">Lịch sử Ghi chú</h3>
          
          <!-- Add Note Form -->
          <div class="mb-8 p-6 bg-slate-50 rounded-2xl border border-slate-100 space-y-4">
             <div class="grid grid-cols-2 gap-4">
                <UiInput v-model="noteForm.month" type="number" label="Tháng" placeholder="1-12" />
                <UiInput v-model="noteForm.year" type="number" label="Năm" placeholder="202x" />
             </div>
             <textarea v-model="noteForm.content" rows="3" class="w-full bg-white border border-slate-200 rounded-xl px-4 py-3 text-sm font-medium text-slate-700 outline-none focus:ring-2 focus:ring-primary-500 transition-all resize-none" placeholder="Nhập ghi chú mới..."></textarea>
             <div class="flex justify-end">
                <UiButton size="sm" @click="handleAddNote" :loading="addingNote" :disabled="!noteForm.content">
                  <Plus class="w-4 h-4" />
                  Thêm ghi chú
                </UiButton>
             </div>
          </div>

          <!-- Notes Timeline -->
          <div class="space-y-6">
             <div v-if="notes.length === 0" class="py-8 text-center bg-slate-50/50 rounded-2xl border border-dashed border-slate-200">
                <p class="text-slate-400 text-xs font-medium italic">Chưa có ghi chú nào</p>
             </div>
             <div v-else class="relative pl-6 space-y-6 before:content-[''] before:absolute before:left-0 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-100">
                <div v-for="note in notes" :key="note.id" class="relative">
                   <div class="absolute -left-[27px] top-1.5 w-3 h-3 rounded-full bg-white border-2 border-primary-500 z-10"></div>
                   <div class="bg-white border border-slate-100 rounded-2xl p-5 hover:shadow-md transition-all group">
                      <div class="flex items-center justify-between mb-2">
                         <div class="flex items-center gap-2">
                            <span v-if="note.month && note.year" class="px-2 py-0.5 bg-primary-100 text-primary-700 text-[10px] font-black rounded-lg">Tháng {{ note.month }}/{{ note.year }}</span>
                            <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ formatDate(note.createdAt) }}</span>
                         </div>
                         <button @click="handleDeleteNote(note.id)" class="p-1.5 text-slate-300 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-all rounded-lg hover:bg-red-50">
                            <Trash2 class="w-3.5 h-3.5" />
                         </button>
                      </div>
                      <p class="text-sm font-medium text-slate-700 leading-relaxed whitespace-pre-wrap">{{ note.content }}</p>
                      <p class="mt-3 text-[9px] font-bold text-slate-400 uppercase tracking-widest flex items-center gap-1">
                         <User class="w-3 h-3" /> {{ note.createdBy }}
                      </p>
                   </div>
                </div>
             </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Common Error Modal -->
    <UiErrorModal
      :show="showErrorModal"
      :title="errorTitle"
      :message="errorMessage"
      :detail="errorDetail"
      @close="showErrorModal = false"
    />
  </div>
</template>

<script setup>
import { ChevronLeft, Save, User, Camera, Trash2, Plus } from 'lucide-vue-next';

const route = useRoute();
const router = useRouter();
const { $api } = useNuxtApp();

const employee = ref({});
const addingNote = ref(false);

// Error Modal State
const showErrorModal = ref(false);
const errorTitle = ref('');
const errorMessage = ref('');
const errorDetail = ref('');

const triggerError = (title, message, detail = '') => {
  errorTitle.value = title;
  errorMessage.value = message;
  errorDetail.value = detail;
  showErrorModal.value = true;
};

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

const noteForm = reactive({
  content: '',
  month: new Date().getMonth() + 1,
  year: new Date().getFullYear()
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
    fetchNotes();
  } catch (err) {
    console.error(err);
    triggerError('Lỗi tải dữ liệu', 'Không thể tải thông tin nhân viên này.', err.message);
  }
};

const fetchNotes = async () => {
  try {
    const res = await $api.get(`/employees/${route.params.id}/notes`);
    notes.value = res.data;
  } catch (err) {
    console.error(err);
  }
};

const handleAddNote = async () => {
  if (!noteForm.content) return;
  addingNote.value = true;
  try {
    await $api.post(`/employees/${employee.value.id}/notes`, noteForm);
    fetchNotes();
  } catch (err) {
    triggerError('Lỗi thêm ghi chú', 'Đã xảy ra lỗi khi thêm ghi chú mới.', err.message);
  } finally {
    addingNote.value = false;
  }
};

const handleDeleteNote = async (noteId) => {
  if (!confirm('Bạn có chắc chắn muốn xóa ghi chú này?')) return;
  try {
    await $api.delete(`/employees/notes/${noteId}`);
    fetchNotes();
  } catch (err) {
    triggerError('Lỗi xóa ghi chú', 'Không thể xóa ghi chú này khỏi hệ thống.', err.message);
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
    // Use a success modal or simple notification if needed, but the requirement is to use the common error popup for errors.
    // For success, alert is okay if no common success modal is defined, but let's just use alert for now as requested.
    alert('Cập nhật thành công');
  } catch (err) {
    triggerError('Lỗi lưu thông tin', 'Đã xảy ra lỗi khi cập nhật thông tin nhân viên.', err.response?.data?.message || err.message);
  } finally {
    saving.value = false;
  }
};

const handleAvatarUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  if (file.size > 2 * 1024 * 1024) {
    triggerError('Lỗi tệp tin', 'Ảnh đại diện không được vượt quá 2MB.');
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
    triggerError('Lỗi tải ảnh', 'Không thể tải ảnh đại diện lên hệ thống.', err.response?.data?.message || err.message);
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const handleBack = () => router.push('/employees');

onMounted(fetchData);
</script>
