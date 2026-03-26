<template>
  <div class="space-y-8">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-3xl font-black text-slate-900 tracking-tight">Trung tâm Bảng lương</h2>
        <p class="text-slate-500 font-medium">Tính toán và quản lý lương nhân viên hàng tháng</p>
      </div>
      <div class="flex gap-3">
        <div class="card p-1 flex bg-slate-100 rounded-xl">
          <button 
            @click="viewMode = 'list'"
            :class="`px-4 py-2 rounded-lg text-sm font-black transition-all ${viewMode === 'list' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-400 hover:text-slate-600'}`"
          >
            Lịch sử chốt lương
          </button>
          <button 
            @click="viewMode = 'calculate'"
            :class="`px-4 py-2 rounded-lg text-sm font-black transition-all ${viewMode === 'calculate' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-400 hover:text-slate-600'}`"
          >
            Tính lương mới
          </button>
        </div>
      </div>
    </div>

    <!-- Calculate View -->
    <div v-if="viewMode === 'calculate'" class="space-y-8 animate-in fade-in slide-in-from-top-4 duration-500">
      <div class="card p-10 flex flex-col items-center text-center space-y-8 max-w-2xl mx-auto shadow-2xl shadow-primary-50">
        <div class="w-20 h-20 bg-primary-600 rounded-3xl flex items-center justify-center text-white shadow-xl shadow-primary-200">
          <Calculator class="w-10 h-10" />
        </div>
        <div class="space-y-2">
          <h3 class="text-2xl font-black text-slate-900">Tính toán lương tháng</h3>
          <p class="text-slate-500 font-medium max-w-sm">Hệ thống sẽ tổng hợp chấm công, sản lượng và các khoản thưởng/phạt để xuất bảng lương.</p>
        </div>

        <div class="flex gap-6 w-full">
          <div class="flex-1 flex flex-col gap-2">
            <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest text-left ml-1">Chọn Tháng</label>
            <select v-model="calcForm.month" class="input-field h-14 text-lg font-black">
              <option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</option>
            </select>
          </div>
          <div class="flex-1 flex flex-col gap-2">
            <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest text-left ml-1">Chọn Năm</label>
            <select v-model="calcForm.year" class="input-field h-14 text-lg font-black">
              <option v-for="y in [2024, 2025, 2026]" :key="y" :value="y">Năm {{ y }}</option>
            </select>
          </div>
        </div>

        <UiButton @click="handleCalculate" class="w-full h-16 text-xl font-black rounded-2xl shadow-xl shadow-primary-100" :loading="calculating">
          <Play class="w-6 h-6 fill-current" />
          Bắt đầu tính lương
        </UiButton>
      </div>
    </div>

    <!-- Filters -->
    <div v-if="viewMode === 'list'" class="grid grid-cols-1 md:grid-cols-4 gap-6 animate-in fade-in slide-in-from-left-4 duration-500">
      <div class="flex flex-col gap-1.5 min-w-[200px]">
        <SelectDepartment 
          v-model="selectedDepartments" 
          multiple
          label="Lọc Phòng Ban" 
        />
      </div>

      <div class="flex flex-col gap-1.5 min-w-[200px]">
        <SelectTeam 
          v-model="selectedTeams" 
          multiple
          :departmentId="selectedDepartments.length === 1 ? selectedDepartments[0] : null"
          label="Lọc Tổ Đội" 
        />
      </div>

      <!-- Reset Filter & Export -->
      <div class="flex items-end pb-1 gap-2">
        <button 
          v-if="selectedDepartments.length > 0 || selectedTeams.length > 0"
          @click="resetFilters"
          class="px-4 py-2 bg-slate-100 hover:bg-primary-50 text-[10px] font-black text-slate-500 hover:text-primary-600 uppercase rounded-xl transition-all tracking-widest"
        >
          Xóa tất cả bộ lọc
        </button>

        <button 
          v-if="payrolls.length > 0"
          @click="handleExportPayslips"
          class="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white text-[10px] font-black uppercase rounded-xl transition-all tracking-widest shadow-md flex items-center gap-1 whitespace-nowrap"
          :disabled="exporting"
        >
          <Download class="w-3.5 h-3.5" v-if="!exporting" />
          <div v-else class="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
          {{ exporting ? 'Đang xuất...' : 'Xuất Phiếu Lương' }}
        </button>
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

    <!-- History List View -->
    <div v-if="viewMode === 'list'" class="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div class="card overflow-hidden">
        <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
          <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
          <p class="text-slate-500 font-bold">Đang tải lịch sử...</p>
        </div>

        <div v-else-if="payrolls.length === 0" class="p-20 text-center space-y-4">
          <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-300">
            <Wallet class="w-10 h-10" />
          </div>
          <p class="text-slate-500 font-bold">Chưa có bảng lương nào được chốt.</p>
        </div>

        <table v-else class="w-full text-left">
          <thead>
            <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
              <th class="px-8 py-5">Kỳ lương</th>
              <th class="px-8 py-5">Nhân viên</th>
              <th class="px-8 py-5">Phòng ban / Tổ</th>
              <th class="px-8 py-5">Lương SP/Cố định</th>
              <th class="px-8 py-5">Thưởng/Phạt</th>
              <th class="px-8 py-5">{{ $t('employee.insurance_salary_actual') }}</th>
              <th class="px-8 py-5">{{ $t('employee.cash_salary') }}</th>
              <th class="px-8 py-5">Thực lĩnh</th>
              <th class="px-8 py-5">Trạng thái</th>
              <th class="px-8 py-5 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 border-b border-slate-100">
            <tr v-for="p in paginatedPayrolls" :key="p.id" class="hover:bg-slate-50/50 transition-all group">
              <td class="px-8 py-5">
                <span class="font-black text-slate-900">{{ p.month }}/{{ p.year }}</span>
              </td>
              <td class="px-8 py-5 cursor-pointer hover:text-primary-600" @click="showDetails(p)">
                <p class="font-bold text-slate-700 underline decoration-slate-200 underline-offset-4">{{ p.employeeName }}</p>
                <p class="text-[10px] font-black text-slate-400 uppercase tracking-tighter">{{ p.employeeCode }}</p>
              </td>
              <td class="px-8 py-5">
                <div class="flex flex-col">
                  <span class="text-xs font-bold text-slate-600">{{ p.departmentName }}</span>
                  <span class="text-[10px] font-black text-slate-400 uppercase">{{ p.teamName }}</span>
                </div>
              </td>
              <td class="px-8 py-5 text-sm font-bold text-slate-600">{{ (p.productSalary || 0).toLocaleString() }}đ</td>
              <td class="px-8 py-5 text-sm font-bold" :class="(p.totalPenaltyBonus || 0) >= 0 ? 'text-emerald-600' : 'text-red-600'">
                {{ (p.totalPenaltyBonus || 0) >= 0 ? '+' : '' }}{{ (p.totalPenaltyBonus || 0).toLocaleString() }}đ
              </td>
              <td class="px-8 py-5 text-sm font-bold text-blue-600">{{ (p.insuranceSalary || 0).toLocaleString() }}đ</td>
              <td class="px-8 py-5 text-sm font-bold text-amber-600">{{ (p.cashSalary || 0).toLocaleString() }}đ</td>
              <td class="px-8 py-5 font-black text-primary-700 text-lg">{{ (p.totalSalary || 0).toLocaleString() }}đ</td>
              <td class="px-8 py-5">
                <span :class="`px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${p.status === 'CONFIRMED' ? 'bg-emerald-50 text-emerald-600' : 'bg-orange-50 text-orange-600'}`">
                  {{ p.status === 'CONFIRMED' ? 'Đã chốt' : 'Dự toán' }}
                </span>
              </td>
              <td class="px-8 py-5 text-right">
                <div class="flex justify-end gap-2">
                  <button v-if="p.status !== 'CONFIRMED'" @click="handleConfirm(p)" class="px-3 py-1.5 bg-primary-50 text-primary-700 rounded-lg text-[10px] font-black hover:bg-primary-600 hover:text-white transition-all uppercase tracking-widest">
                    Chốt
                  </button>
                  <button @click="showConfirmTeam(p)" class="px-3 py-1.5 bg-slate-100 text-slate-600 rounded-lg text-[10px] font-black hover:bg-slate-900 hover:text-white transition-all uppercase tracking-widest">
                    Tổ
                  </button>
                  <div v-if="p.status === 'CONFIRMED'" class="flex items-center pl-2">
                    <CheckCircle2 class="text-emerald-500 w-5 h-5" />
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div v-if="payrolls.length > 0" class="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
          <div class="flex items-center gap-4">
            <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Hiển thị</span>
            <select v-model="itemsPerPage" class="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-bold text-slate-600 focus:ring-2 focus:ring-primary-500 outline-none">
              <option :value="10">10 dòng</option>
              <option :value="20">20 dòng</option>
              <option :value="50">50 dòng</option>
            </select>
            <span class="text-xs font-bold text-slate-500">
              {{ (currentPage - 1) * itemsPerPage + 1 }}-{{ Math.min(currentPage * itemsPerPage, payrolls.length) }} của {{ payrolls.length }}
            </span>
          </div>
          <div class="flex items-center gap-2">
            <button 
              @click="currentPage--" 
              :disabled="currentPage === 1"
              class="p-2 rounded-lg bg-white border border-slate-200 text-slate-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 transition-all shadow-sm"
            >
              <ChevronLeft class="w-4 h-4" />
            </button>
            <div class="flex items-center gap-1">
              <button 
                v-for="p in totalPages" 
                :key="p"
                @click="currentPage = p"
                :class="['w-8 h-8 rounded-lg flex items-center justify-center text-xs font-black transition-all', 
                         currentPage === p ? 'bg-primary-600 text-white shadow-lg shadow-primary-200' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 shadow-sm']"
              >
                {{ p }}
              </button>
            </div>
            <button 
              @click="currentPage++" 
              :disabled="currentPage === totalPages"
              class="p-2 rounded-lg bg-white border border-slate-200 text-slate-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 transition-all shadow-sm"
            >
              <ChevronRight class="w-4 h-4" />
            </button>
          </div>
        </div>

      </div>
    </div>

    <!-- Daily Details Modal -->
    <div v-if="showDetailModal" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" @click="showDetailModal = false"></div>
      <div class="card w-full max-w-5xl max-h-[90vh] flex flex-col relative animate-in zoom-in-95 duration-300 shadow-2xl">
        <div class="px-8 py-6 border-b border-slate-100 flex items-center justify-between bg-white sticky top-0 z-10">
          <div>
            <h3 class="text-xl font-black text-slate-900">Chi tiết lương & Lịch sử đi làm</h3>
            <p class="text-slate-500 font-bold text-xs uppercase tracking-widest mt-1">
              Nhân viên: {{ selectedPayroll?.employeeName }} ({{ selectedPayroll?.employeeCode }}) - Tháng {{ selectedPayroll?.month }}/{{ selectedPayroll?.year }}
            </p>
          </div>
          <button @click="showDetailModal = false" class="p-2 hover:bg-slate-100 rounded-xl transition-colors">
            <X class="w-6 h-6 text-slate-400" />
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-8 custom-scrollbar bg-slate-50/30">
          <div v-if="loadingDetails" class="py-20 flex flex-col items-center gap-4">
            <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
            <p class="text-sm font-bold text-slate-500">Đang tải chi tiết...</p>
          </div>
          
          <div v-else class="space-y-6">
            <div class="grid grid-cols-4 gap-4">
              <div class="card p-4 bg-white">
                <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Lương Sản Phẩm</p>
                <p class="text-xl font-black text-primary-600">{{ (selectedPayroll?.productSalary || 0).toLocaleString() }}đ</p>
              </div>
              <div class="card p-4 bg-white">
                <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Phụ cấp chức vụ</p>
                <p class="text-xl font-black text-slate-700">{{ (selectedPayroll?.benefitSalary || 0).toLocaleString() }}đ</p>
              </div>
              <div class="card p-4 bg-white">
                <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Thưởng/Phạt/Khác</p>
                <p class="text-xl font-black" :class="(selectedPayroll?.totalPenaltyBonus || 0) >= 0 ? 'text-emerald-600' : 'text-red-600'">
                  {{ (selectedPayroll?.totalPenaltyBonus || 0).toLocaleString() }}đ
                </p>
              </div>
              <div class="card p-4 bg-white border-l-4 border-blue-500">
                <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">{{ $t('employee.insurance_salary_actual') }}</p>
                <p class="text-xl font-black text-blue-600">{{ (selectedPayroll?.insuranceSalary || 0).toLocaleString() }}đ</p>
              </div>
              <div class="card p-4 bg-white border-l-4 border-amber-500">
                <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">{{ $t('employee.cash_salary') }}</p>
                <p class="text-xl font-black text-amber-600">{{ (selectedPayroll?.cashSalary || 0).toLocaleString() }}đ</p>
              </div>
              <div class="card p-4 bg-primary-600 text-white">
                <p class="text-[10px] font-black text-primary-200 uppercase tracking-widest mb-1">Thực lĩnh</p>
                <p class="text-xl font-black">{{ (selectedPayroll?.totalSalary || 0).toLocaleString() }}đ</p>
              </div>
            </div>

            <div class="card overflow-hidden">
              <table class="w-full text-left text-xs">
                <thead class="bg-slate-50 text-slate-400 font-bold uppercase tracking-wider border-b border-slate-100">
                  <tr>
                    <th class="px-6 py-4">Ngày</th>
                    <th class="px-6 py-4">Chấm công</th>
                    <th class="px-6 py-4">Tổ làm việc</th>
                    <th class="px-6 py-4">Lương SP ngày</th>
                    <th class="px-6 py-4">Phụ cấp</th>
                    <th class="px-6 py-4">Thưởng / Phạt</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100 bg-white">
                  <tr v-for="d in dailyDetails" :key="d.date" class="hover:bg-slate-50 transition-colors">
                    <td class="px-6 py-4 font-black text-slate-700">
                      {{ new Date(d.date).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }) }}
                    </td>
                    <td class="px-6 py-4">
                      <span :class="`px-2 py-0.5 rounded-full font-black ${d.attendanceSymbol === 'X' ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-400'}`">
                        {{ d.attendanceSymbol }}
                      </span>
                    </td>
                    <td class="px-6 py-4">
                       <span class="font-bold text-slate-600">{{ d.teamName }}</span>
                    </td>
                    <td class="px-6 py-4 font-bold text-slate-700">{{ (d.productSalary || 0).toLocaleString() }}đ</td>
                    <td class="px-6 py-4 text-slate-500">{{ (d.benefitSalary || 0).toLocaleString() }}đ</td>
                    <td class="px-6 py-4">
                      <div class="flex flex-col">
                        <span v-if="d.bonus > 0" class="text-emerald-500 font-bold">+{{ d.bonus.toLocaleString() }}đ</span>
                        <span v-if="d.penalty > 0" class="text-red-500 font-bold">-{{ d.penalty.toLocaleString() }}đ</span>
                        <span v-if="d.bonus === 0 && d.penalty === 0" class="text-slate-300">-</span>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Calculator, Play, Wallet, CheckCircle2, ChevronLeft, ChevronRight, X, Download } from 'lucide-vue-next';

const { $api } = useNuxtApp();
const viewMode = ref('list');
const calculating = ref(false);
const loading = ref(true);
const payrolls = ref([]);
const selectedDepartments = ref([]);
const selectedTeams = ref([]);

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

const resetFilters = () => {
  selectedDepartments.value = [];
  selectedTeams.value = [];
  currentPage.value = 1;
};

// Pagination
const currentPage = ref(1);
const itemsPerPage = ref(10);
const totalPages = computed(() => Math.ceil(payrolls.value.length / itemsPerPage.value) || 1);

const sortedPayrolls = computed(() => {
  let filtered = [...payrolls.value];

  if (selectedDepartments.value.length > 0) {
    filtered = filtered.filter(p => selectedDepartments.value.includes(p.departmentId));
  }

  if (selectedTeams.value.length > 0) {
    filtered = filtered.filter(p => selectedTeams.value.includes(p.teamId));
  }

  return filtered.sort((a, b) => {
    // Sắp xếp theo Phòng ban -> Tổ -> Tên NV
    const deptCompare = (a.departmentName || '').localeCompare(b.departmentName || '');
    if (deptCompare !== 0) return deptCompare;
    
    const teamCompare = (a.teamName || '').localeCompare(b.teamName || '');
    if (teamCompare !== 0) return teamCompare;
    
    return (a.employeeName || '').localeCompare(b.employeeName || '');
  });
});

const paginatedPayrolls = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return sortedPayrolls.value.slice(start, end);
});

watch(itemsPerPage, () => {
  currentPage.value = 1;
});

const calcForm = reactive({
  month: new Date().getMonth() + 1,
  year: new Date().getFullYear()
});

const fetchPayrolls = async () => {
  loading.value = true;
  try {
    const res = await $api.get(`/payrolls/${calcForm.year}/${calcForm.month}/items`);
    payrolls.value = res.data;
  } catch (err) {
    triggerError('Lỗi tải bảng lương', 'Không thể lấy dữ liệu bảng lương cho kỳ này.', err.message);
  } finally {
    loading.value = false;
  }
};

const fetchDeptsAndTeams = async () => {
  try {
    const [deptRes, teamRes] = await Promise.all([
      $api.get('/departments'),
      $api.get('/teams')
    ]);
    departments.value = deptRes.data;
    teams.value = teamRes.data;
  } catch (err) {
    console.error('Lỗi tải danh mục:', err);
  }
};

const handleCalculate = async () => {
  calculating.value = true;
  try {
    const res = await $api.post('/payrolls/calculate', calcForm);
    alert(res.message);
    viewMode.value = 'list';
    fetchPayrolls();
  } catch (err) {
    triggerError('Lỗi tính lương', 'Quá trình tính toán lương gặp sự cố.', err.response?.data?.message || err.message);
  } finally {
    calculating.value = false;
  }
};

const handleConfirm = async (p) => {
  if (!confirm(`Xác nhận chốt lương cho nhân viên ${p.employeeName}?`)) return;
  try {
    // API chốt toàn bộ Payroll (vì status ở Payroll entity) 
    // Hoặc nếu muốn chốt lẻ thì cần logic Backend khác. 
    // Hiện tại confirmPayroll(id) chốt thực thể Payroll.
    await $api.put(`/payrolls/${p.payrollId}/confirm`);
    fetchPayrolls();
  } catch (err) {
    triggerError('Lỗi chốt lương', 'Không thể chốt lương cho cá nhân này.', err.response?.data?.message || err.message);
  }
};

const showConfirmTeam = async (p) => {
  if (!p.teamId) return alert('Nhân viên này chưa thuộc tổ nào');
  if (!confirm(`Xác nhận chốt bảng lương cho toàn bộ [${p.teamName}]?`)) return;
  
  try {
    alert(`Đã chốt lương cho tổ ${p.teamName}`);
    fetchPayrolls();
  } catch (err) {
    triggerError('Lỗi chốt tổ', 'Không thể chốt lương cho toàn bộ tổ đội.', err.response?.data?.message || err.message);
  }
};

const showDetailModal = ref(false);
const selectedPayroll = ref(null);
const loadingDetails = ref(false);
const dailyDetails = ref([]);

const showDetails = async (p) => {
  selectedPayroll.value = p;
  showDetailModal.value = true;
  loadingDetails.value = true;
  try {
    const res = await $api.get(`/payrolls/items/${p.id}/daily-details`);
    dailyDetails.value = res.data;
  } catch (err) {
    triggerError('Lỗi tải chi tiết', 'Không thể lấy thông tin chi tiết các ngày làm việc.', err.message);
  } finally {
    loading.value = false;
  }
};

const exporting = ref(false);

const handleExportPayslips = async () => {
  if (exporting.value) return;
  exporting.value = true;
  try {
    const res = await $api.get(`/payrolls/${calcForm.year}/${calcForm.month}/export-payslips`, {
      responseType: 'blob'
    });
    
    const url = window.URL.createObjectURL(new Blob([res]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `PhieuLuong_Thang_${calcForm.month}_${calcForm.year}.xlsx`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    window.URL.revokeObjectURL(url);
  } catch (error) {
    triggerError('Lỗi xuất phiếu lương', 'Không thể tạo file Excel phiếu lương.', error.message);
    console.error(error);
  } finally {
    exporting.value = false;
  }
};

onMounted(() => {
  fetchPayrolls();
});

onUnmounted(() => {
});

// Reset payrolls when month/year changes in list view
watch([() => calcForm.month, () => calcForm.year], () => {
  if (viewMode.value === 'list') fetchPayrolls();
});
</script>
