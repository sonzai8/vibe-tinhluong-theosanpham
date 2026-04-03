<template>
  <div class="space-y-6 max-w-[1200px] mx-auto pb-12">
    <!-- Header with Back Button -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <button @click="router.push('/work-histories')" class="p-2 hover:bg-white rounded-xl transition-all shadow-sm border border-transparent hover:border-slate-100 bg-slate-50">
          <ChevronLeft class="w-6 h-6 text-slate-600" />
        </button>
        <div>
          <h1 class="text-3xl font-black text-slate-900 tracking-tight">Chi tiết lịch sử công tác</h1>
          <p v-if="employee" class="text-slate-500 text-sm font-medium mt-1 uppercase tracking-widest">
            {{ employee.fullName }} - {{ employee.code }} ({{ employee.departmentName || '---' }})
          </p>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
      <!-- Left: Transfer Form -->
      <div class="lg:col-span-4 space-y-6">
        <div class="card p-6 shadow-sm border border-slate-100 bg-white">
          <h3 class="text-xs font-black text-slate-900 uppercase tracking-widest border-b border-slate-50 pb-4 mb-6 flex items-center gap-2">
            <Plus class="w-4 h-4 text-primary-500" />
            Ghi nhận điều chuyển mới
          </h3>
          
          <div class="space-y-5">
            <div>
              <SelectTeamTree
                v-model="transferForm.teamId"
                label="Tổ điều chuyển đến"
                placeholder="Chọn tổ đội mới..."
                :departmentId="null"
                :allowClear="true"
              />
            </div>

            <UiInput v-model="transferForm.startDate" type="date" label="Ngày bắt đầu (Hiệu lực)" />

            <div class="pt-2">
              <UiButton class="w-full h-11" @click="handleTransfer" :loading="transferring" :disabled="!transferForm.teamId || !transferForm.startDate">
                <Save class="w-4 h-4 mr-2" />
                Lưu điều chuyển
              </UiButton>
            </div>
          </div>
        </div>

        <!-- Current Status Info -->
        <div class="p-5 bg-primary-50/50 rounded-2xl border border-primary-100/50">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-10 h-10 rounded-xl bg-primary-100 flex items-center justify-center">
              <History class="w-5 h-5 text-primary-600" />
            </div>
            <div>
              <p class="text-[10px] font-black text-primary-600 uppercase tracking-widest">Tổ hiện tại</p>
              <p class="text-sm font-black text-slate-900">{{ employee?.teamName || 'Chưa gán tổ' }}</p>
            </div>
          </div>
          <p class="text-[10px] font-medium text-slate-500 italic leading-relaxed">
            * Khi điều chuyển sang tổ mới, hệ thống sẽ tự động đóng bản ghi làm việc tại tổ cũ vào ngày trước đó.
          </p>
        </div>
      </div>

      <!-- Right: Timeline History -->
      <div class="lg:col-span-8 space-y-6">
        <div class="card p-6 shadow-sm border border-slate-100 bg-white">
          <h3 class="text-xs font-black text-slate-900 uppercase tracking-widest border-b border-slate-50 pb-4 mb-8">
            Quá trình công tác
          </h3>

          <div v-if="loadingHistory" class="py-20 flex flex-col items-center justify-center gap-4">
            <div class="w-10 h-10 border-4 border-slate-100 border-t-primary-600 rounded-full animate-spin"></div>
            <p class="text-slate-400 text-[10px] font-black uppercase tracking-widest animate-pulse">Đang tải lịch sử...</p>
          </div>

          <div v-else-if="history.length === 0" class="py-20 text-center">
             <div class="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4">
                <History class="w-8 h-8 text-slate-200" />
             </div>
             <p class="text-slate-400 text-sm font-medium italic">Chưa có lịch sử điều chuyển nào được ghi nhận</p>
          </div>

          <div v-else class="space-y-6 relative pl-4">
            <div v-for="(item, index) in history" :key="item.id" class="relative pl-10 group">
              <!-- Timeline line -->
              <div v-if="index !== history.length - 1" class="absolute left-[11px] top-8 bottom-0 w-0.5 bg-slate-100 group-hover:bg-primary-100 transition-colors"></div>
              
              <!-- Timeline Dot -->
              <div class="absolute left-0 top-2 w-6 h-6 rounded-full border-4 border-white shadow-md flex items-center justify-center z-10 transition-all group-hover:scale-110"
                   :class="index === 0 ? 'bg-primary-500' : 'bg-slate-200'">
                 <div class="w-1.5 h-1.5 rounded-full bg-white"></div>
              </div>

              <div class="bg-slate-50/30 border rounded-2xl p-6 transition-all group-hover:bg-white group-hover:border-primary-100 group-hover:shadow-lg"
                   :class="index === 0 ? 'border-primary-500/20 ring-1 ring-primary-500/5 bg-primary-50/10' : 'border-slate-100'">
                <div class="flex flex-wrap items-center justify-between gap-4 mb-4">
                  <div class="flex items-center gap-3">
                    <p class="text-base font-black text-slate-900">{{ item.team?.name || '---' }}</p>
                    <span v-if="index === 0" class="px-2.5 py-1 bg-primary-100 text-primary-600 text-[9px] font-black rounded-lg uppercase tracking-widest border border-primary-200">Hiện tại</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <div class="flex items-center gap-2 text-slate-400 mr-2">
                      <Clock class="w-3.5 h-3.5" />
                      <p class="text-xs font-bold italic">
                        {{ formatDate(item.startDate) }} - {{ item.endDate ? formatDate(item.endDate) : 'Hiện tại' }}
                      </p>
                    </div>
                    <UiButton variant="ghost" size="xs" @click="openEditModal(item)" class="text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg">
                      <Edit2 class="w-3.5 h-3.5 mr-1" />
                      {{ $t('common.edit') }}
                    </UiButton>
                  </div>
                </div>
                
                <div class="space-y-3">
                  <div class="flex flex-wrap items-center gap-3">
                      <span class="px-3 py-1 bg-white text-[10px] font-black text-slate-500 uppercase tracking-widest rounded-lg border border-slate-200 shadow-sm">
                        {{ item.team?.departmentName }}
                      </span>
                      <div class="h-4 w-px bg-slate-200 mx-1"></div>
                      <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-1.5">
                        <span class="w-1.5 h-1.5 rounded-full bg-slate-300"></span>
                        Thời lượng: {{ calculateDuration(item.startDate, item.endDate) }}
                      </p>
                  </div>

                  <!-- Note if exists -->
                  <div v-if="item.note" class="p-3 bg-white rounded-xl border border-slate-100 text-xs text-slate-600 font-medium italic">
                    "{{ item.note }}"
                  </div>

                  <!-- Auditing Log Information -->
                  <div class="pt-3 border-t border-slate-100 flex items-center justify-between opacity-60 group-hover:opacity-100 transition-opacity">
                    <p class="text-[9px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                      <User class="w-3 h-3" />
                      Tạo bởi: <span class="text-slate-600">{{ item.createdBy || 'Hệ thống' }}</span>
                      <span class="mx-1">|</span>
                      Lúc: <span class="text-slate-600">{{ item.createdAt ? formatDateTime(item.createdAt) : '---' }}</span>
                    </p>
                    <p v-if="item.updatedBy" class="text-[9px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                      <Edit2 class="w-3 h-3" />
                      Cập nhật: <span class="text-slate-600">{{ item.updatedBy }}</span> ({{ formatDateTime(item.updatedAt) }})
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit History Modal -->
    <UiModal v-model="showEditModal" title="Chỉnh sửa bản ghi lịch sử" size="lg">
      <div v-if="editingItem" class="space-y-6">
        <div class="grid grid-cols-2 gap-4">
          <SelectTeamTree v-model="editForm.teamId" label="Đổi tổ đội" class="col-span-2" />
        </div>

        <div class="grid grid-cols-2 gap-4">
          <UiInput v-model="editForm.startDate" type="date" label="Ngày bắt đầu" />
          <UiInput v-model="editForm.endDate" type="date" label="Ngày kết thúc" placeholder="Để trống nếu là tổ hiện tại" />
        </div>

        <div class="space-y-1.5">
          <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Ghi chú / Lý do điều chỉnh</label>
          <textarea 
            v-model="editForm.note"
            rows="3"
            class="w-full bg-white border border-slate-200 rounded-xl px-4 py-3 text-sm font-medium text-slate-900 focus:ring-2 focus:ring-primary-500 outline-none transition-all"
            placeholder="Nhập ghi chú chi tiết..."
          ></textarea>
        </div>

        <div class="flex justify-end gap-3 pt-4 border-t border-slate-100">
          <UiButton variant="ghost" @click="showEditModal = false">Hủy</UiButton>
          <UiButton @click="updateHistory" :loading="updating">
            <Save class="w-4 h-4 mr-2" />
            Cập nhật bản ghi
          </UiButton>
        </div>
      </div>
    </UiModal>

    <!-- Error Modal -->
    <UiErrorModal
      :show="showError"
      title="Lỗi xử lý"
      :message="errorMessage"
      @close="showError = false"
    />
  </div>
</template>

<script setup>
import { ChevronLeft, History, Plus, Save, Clock, Edit2, User } from 'lucide-vue-next';

const route = useRoute();
const router = useRouter();
const { $api } = useNuxtApp();

const employee = ref(null);
const history = ref([]);
const loadingHistory = ref(true);

const transferForm = reactive({
  teamId: null,
  startDate: new Date().toISOString().split('T')[0]
});
const transferring = ref(false);

// Edit History State
const showEditModal = ref(false);
const editingItem = ref(null);
const updating = ref(false);
const editForm = reactive({
  startDate: '',
  endDate: '',
  note: ''
});

const showError = ref(false);
const errorMessage = ref('');

const fetchEmployee = async () => {
  try {
    const res = await $api.get(`/employees/${route.params.id}`);
    employee.value = res.data;
  } catch (err) {
    console.error(err);
    errorMessage.value = 'Không thể tải thông tin nhân viên';
    showError.value = true;
  }
};

const fetchHistory = async () => {
  loadingHistory.value = true;
  try {
    const res = await $api.get(`/employees/${route.params.id}/team-history`);
    history.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loadingHistory.value = false;
  }
};

const handleTransfer = async () => {
  if (!transferForm.teamId || !transferForm.startDate || !employee.value) return;
  transferring.value = true;
  try {
    // Để gọi API PUT /employees/:id thành công, chúng ta cần gửi đủ thông tin (do backend @Valid fullName)
    const payload = {
      fullName: employee.value.fullName,
      code: employee.value.code,
      phone: employee.value.phone,
      citizenId: employee.value.citizenId,
      status: employee.value.status,
      departmentId: employee.value.department?.id,
      roleId: employee.value.role?.id,
      salaryType: employee.value.salaryType,
      baseSalaryConfig: employee.value.baseSalaryConfig,
      insuranceSalaryConfig: employee.value.insuranceSalaryConfig,
      canLogin: employee.value.canLogin,
      // Dữ liệu điều chuyển mới
      teamId: transferForm.teamId,
      transferDate: transferForm.startDate
    };

    await $api.put(`/employees/${route.params.id}`, payload);
    
    // Refresh data
    await fetchEmployee();
    await fetchHistory();
    
    // Reset form
    transferForm.teamId = null;
    transferForm.startDate = new Date().toISOString().split('T')[0];
    alert('Điều chuyển thành công');
  } catch (err) {
    errorMessage.value = err.response?.data?.message || err.message || String(err);
    showError.value = true;
  } finally {
    transferring.value = false;
  }
};

const openEditModal = (item) => {
  editingItem.value = item;
  editForm.teamId = item.team?.id;
  editForm.startDate = item.startDate;
  editForm.endDate = item.endDate || '';
  editForm.note = item.note || '';
  showEditModal.value = true;
};

const updateHistory = async () => {
  updating.value = true;
  try {
    // Gửi JSON body để khớp với @RequestBody ở backend
    const payload = {
      teamId: editForm.teamId,
      startDate: editForm.startDate,
      endDate: editForm.endDate || null,
      note: editForm.note || null
    };

    await $api.put(`/employees/team-history/${editingItem.value.id}`, payload);
    
    showEditModal.value = false;
    await fetchHistory();
    alert('Cập nhật lịch sử thành công');
  } catch (err) {
    errorMessage.value = err.response?.data?.message || err.message || String(err);
    showError.value = true;
  } finally {
    updating.value = false;
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('vi-VN');
};

const formatDateTime = (dateStr) => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return `${d.toLocaleDateString('vi-VN')} ${d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`;
};

const calculateDuration = (start, end) => {
  const s = new Date(start);
  const e = end ? new Date(end) : new Date();
  const diffTime = Math.abs(e - s);
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return `${diffDays} ngày`;
};

onMounted(() => {
  fetchEmployee();
  fetchHistory();
});
</script>
